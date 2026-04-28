package com.logistics.service;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Shipper;
import com.logistics.model.ShipperStatus;
import com.logistics.repository.BatchRepository;
import com.logistics.repository.ShipperRepository;
import com.logistics.util.Logger;

import java.util.List;

public class DispatcherService implements Runnable {
    private static final DispatcherService instance = new DispatcherService();
    private volatile boolean running = false;
    private final BatchRepository batchRepository;
    private final ShipperRepository shipperRepository;

    private DispatcherService() {
        this.batchRepository = new BatchRepository();
        this.shipperRepository = new ShipperRepository();
    }

    public static DispatcherService getInstance() {
        return instance;
    }

    @Override
    public void run() {
        running = true;
        Logger.log("SERVICE", "DispatcherService bắt đầu chạy");

        try {
            while (running) {
                // Poll for created batches every 2-5 seconds
                Thread.sleep(2000 + (int)(Math.random() * 3000));

                List<Batch> createdBatches = batchRepository.findByStatus(BatchStatus.CREATED);

                for (Batch batch : createdBatches) {
                    // Find nearest available shipper
                    Shipper nearestShipper = findNearestAvailableShipper(batch);
                    if (nearestShipper != null) {
                        // Assign batch to shipper
                        batch.setStatus(BatchStatus.ASSIGNED);
                        batch.setShipperId(nearestShipper.getId());

                        // Update batch in database
                        batchRepository.updateStatus(batch.getId(), BatchStatus.ASSIGNED);

                        // Update shipper status to BUSY
                        shipperRepository.updateStatus(nearestShipper.getId(), ShipperStatus.BUSY);

                        Logger.log("DISPATCH", "Gán batch " + batch.getId() + " → shipper " + nearestShipper.getName());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("SERVICE", "DispatcherService bị ngắt");
        } finally {
            running = false;
            Logger.log("SERVICE", "DispatcherService dừng");
        }
    }

    private Shipper findNearestAvailableShipper(Batch batch) {
        List<Shipper> availableShippers = shipperRepository.findAvailable();

        if (availableShippers.isEmpty() || batch.getOrders().isEmpty()) {
            return null;
        }

        Shipper nearest = null;
        double minDistance = Double.MAX_VALUE;

        // Calculate distance to first order in batch
        double orderX = batch.getOrders().get(0).getX();
        double orderY = batch.getOrders().get(0).getY();

        for (Shipper shipper : availableShippers) {
            double distance = Math.sqrt(
                Math.pow(shipper.getCurrentX() - orderX, 2) +
                Math.pow(shipper.getCurrentY() - orderY, 2)
            );

            if (distance < minDistance) {
                minDistance = distance;
                nearest = shipper;
            }
        }

        return nearest;
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
