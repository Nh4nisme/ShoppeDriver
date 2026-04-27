package com.logistics.service;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Shipper;
import com.logistics.model.ShipperStatus;
import com.logistics.util.QueueManager;
import com.logistics.worker.ShipperWorker;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class DispatcherService implements Runnable {
    private static final DispatcherService instance = new DispatcherService();
    private volatile boolean running = false;
    private final BlockingQueue<Batch> batchQueue;
    private final Map<String, ShipperWorker> shipperWorkers = new ConcurrentHashMap<>();

    private DispatcherService() {
        this.batchQueue = QueueManager.getInstance().getBatchQueue();
    }

    public static DispatcherService getInstance() {
        return instance;
    }

    @Override
    public void run() {
        running = true;
        System.out.println("[DispatcherService] Starting...");

        try {
            while (running) {
                Batch batch = batchQueue.poll(2, java.util.concurrent.TimeUnit.SECONDS);
                if (batch == null) {
                    continue;
                }

                // Find nearest available shipper
                ShipperWorker nearestWorker = findNearestAvailableWorker(batch);
                if (nearestWorker != null) {
                    batch.setStatus(BatchStatus.ASSIGNED);
                    batch.setShipperId(nearestWorker.getShipper().getId());
                    nearestWorker.assignBatch(batch);
                    System.out.println("[DispatcherService] Assigned " + batch + " to " + nearestWorker.getShipper().getName());
                    ShipperTrackingService.getInstance().updateBatchStatus(batch.getId());
                } else {
                    // Put back if no available shipper
                    batchQueue.put(batch);
                    System.out.println("[DispatcherService] No available shipper, requeueing: " + batch);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[DispatcherService] Interrupted!");
        } finally {
            running = false;
            System.out.println("[DispatcherService] Stopped!");
        }
    }

    private ShipperWorker findNearestAvailableWorker(Batch batch) {
        ShipperWorker nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ShipperWorker worker : shipperWorkers.values()) {
            Shipper shipper = worker.getShipper();
            if (shipper.isActive() && shipper.getStatus() == ShipperStatus.IDLE) {
                // Calculate distance to first order in batch
                if (!batch.getOrders().isEmpty()) {
                    double dist = shipper.distanceTo(
                        batch.getOrders().get(0).getX(),
                        batch.getOrders().get(0).getY()
                    );
                    if (dist < minDistance) {
                        minDistance = dist;
                        nearest = worker;
                    }
                }
            }
        }

        return nearest;
    }

    public void registerShipperWorker(String shipperId, ShipperWorker worker) {
        shipperWorkers.put(shipperId, worker);
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}

