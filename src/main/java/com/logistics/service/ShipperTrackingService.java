package com.logistics.service;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Shipper;
import com.logistics.repository.BatchRepository;
import com.logistics.repository.ShipperRepository;
import com.logistics.util.DataChangeListener;
import com.logistics.util.Logger;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShipperTrackingService {
    private static final ShipperTrackingService instance = new ShipperTrackingService();

    private final ShipperRepository shipperRepository;
    private final BatchRepository batchRepository;
    private final List<DataChangeListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;

    private ShipperTrackingService() {
        this.shipperRepository = new ShipperRepository();
        this.batchRepository = new BatchRepository();
    }

    public static ShipperTrackingService getInstance() {
        return instance;
    }

    /**
     * Start the tracking service
     */
    public void start() {
        running = true;
        Logger.log("SERVICE", "ShipperTrackingService bắt đầu chạy");

        // Start background polling thread
        Thread pollingThread = new Thread(() -> {
            while (running) {
                try {
                    // Poll every 2 seconds for updates
                    Thread.sleep(2000);
                    notifyListeners(); // Notify UI of potential changes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    /**
     * Stop the tracking service
     */
    public void stop() {
        running = false;
        Logger.log("SERVICE", "ShipperTrackingService dừng");
    }

    /**
     * Get all shippers from database
     */
    public List<Shipper> getAllShippers() {
        return shipperRepository.findAll();
    }

    /**
     * Get shipper by ID
     */
    public Shipper getShipper(int shipperId) {
        return shipperRepository.findById(shipperId);
    }

    /**
     * Update shipper location in database
     */
    public void updateShipperLocation(int shipperId, double x, double y) {
        shipperRepository.updateLocation(shipperId, x, y);
        Logger.log("TRACKING", "Cập nhật vị trí shipper " + shipperId + " → (" + x + "," + y + ")");
        notifyListeners();
    }

    /**
     * Get all batches from database
     */
    public List<Batch> getAllBatches() {
        return batchRepository.findByStatus(BatchStatus.ASSIGNED);
    }

    /**
     * Get batch by ID
     */
    public Batch getBatch(int batchId) {
        // This would need to be implemented in BatchRepository if needed
        return null;
    }

    /**
     * Get batches assigned to a specific shipper
     */
    public List<Batch> getBatchesForShipper(int shipperId) {
        List<Batch> allBatches = batchRepository.findByStatus(BatchStatus.ASSIGNED);
        return allBatches.stream()
                .filter(batch -> batch.getShipperId() == shipperId)
                .toList();
    }

    /**
     * Add data change listener
     */
    public void addListener(DataChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Logger.log("TRACKING", "Thêm listener: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * Remove data change listener
     */
    public void removeListener(DataChangeListener listener) {
        listeners.remove(listener);
        Logger.log("TRACKING", "Xóa listener: " + listener.getClass().getSimpleName());
    }

    /**
     * Notify all listeners of data changes
     */
    private void notifyListeners() {
        listeners.forEach(listener -> {
            try {
                listener.onDataChanged();
            } catch (Exception e) {
                Logger.error("TRACKING", "Lỗi thông báo listener: " + e.getMessage());
            }
        });
    }

    /**
     * clear listeners of data
     */
    public void clearListeners() {
        listeners.clear();
        Logger.log("TRACKING", "Đã clear tất cả listeners");
    }
}
