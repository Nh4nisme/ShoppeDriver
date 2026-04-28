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
        Logger.log("SERVICE", "DispatcherService bắt đầu chạy (User-Driven Mode)");

        try {
            // Keep service running but wait for manual batch assignment
            while (running) {
                Thread.sleep(10000); // Keep alive, no automatic dispatch
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("SERVICE", "DispatcherService bị ngắt");
        } finally {
            running = false;
            Logger.log("SERVICE", "DispatcherService dừng");
        }
    }

    /**
     * Manually assign batch to shipper (User-Driven)
     * @param batchId Batch ID
     * @param shipperId Shipper ID
     * @return Success status
     */
    public boolean assignBatchToShipper(int batchId, int shipperId) {
        Logger.log("DISPATCH", "Gán batch " + batchId + " cho shipper " + shipperId);

        try {
            // Update batch status and shipper ID
            batchRepository.updateStatus(batchId, BatchStatus.ASSIGNED);

            // Get batch to set shipper ID
            List<Batch> batches = batchRepository.findByStatus(BatchStatus.CREATED);
            for (Batch batch : batches) {
                if (batch.getId() == batchId) {
                    batch.setShipperId(shipperId);
                    break;
                }
            }

            // Update shipper status to BUSY
            shipperRepository.updateStatus(shipperId, ShipperStatus.BUSY);

            Logger.log("DISPATCH", "Gán batch " + batchId + " → shipper " + shipperId + " thành công");
            return true;

        } catch (Exception e) {
            Logger.error("DISPATCH", "Lỗi gán batch: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all available shippers
     */
    public List<Shipper> getAvailableShippers() {
        return shipperRepository.findAvailable();
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
