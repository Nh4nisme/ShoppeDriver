package com.logistics.service;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Shipper;
import com.logistics.repository.BatchRepository;
import com.logistics.repository.ShipperRepository;
import com.logistics.util.DataChangeListener;
import com.logistics.util.Logger;

import java.util.ArrayList;
import java.util.List;
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

    public void start() {
        running = true;
        Logger.log("SERVICE", "ShipperTrackingService bat dau chay");

        Thread pollingThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(2000);
                    notifyListeners();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    public void stop() {
        running = false;
        Logger.log("SERVICE", "ShipperTrackingService dung");
    }

    public List<Shipper> getAllShippers() {
        return shipperRepository.findAll();
    }

    public Shipper getShipper(int shipperId) {
        return shipperRepository.findById(shipperId);
    }

    public void updateShipperLocation(int shipperId, double x, double y) {
        shipperRepository.updateLocation(shipperId, x, y);
        Logger.log("TRACKING", "Cap nhat vi tri shipper " + shipperId + " -> (" + x + "," + y + ")");
        notifyListeners();
    }

    public List<Batch> getAllBatches() {
        List<Batch> batches = new ArrayList<>();
        batches.addAll(batchRepository.findByStatus(BatchStatus.CREATED));
        batches.addAll(batchRepository.findByStatus(BatchStatus.ASSIGNED));
        batches.addAll(batchRepository.findByStatus(BatchStatus.IN_DELIVERY));
        batches.addAll(batchRepository.findByStatus(BatchStatus.COMPLETED));
        return batches;
    }

    public Batch getBatch(int batchId) {
        return batchRepository.findById(batchId);
    }

    public List<Batch> getBatchesForShipper(int shipperId) {
        return batchRepository.findActiveByShipper(shipperId);
    }

    public Batch getActiveBatchForShipper(int shipperId) {
        return batchRepository.findActiveByShipper(shipperId).stream().findFirst().orElse(null);
    }

    public void addListener(DataChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Logger.log("TRACKING", "Them listener: " + listener.getClass().getSimpleName());
        }
    }

    public void removeListener(DataChangeListener listener) {
        listeners.remove(listener);
        Logger.log("TRACKING", "Xoa listener: " + listener.getClass().getSimpleName());
    }

    public void refreshData() {
        notifyListeners();
    }

    private void notifyListeners() {
        listeners.forEach(listener -> {
            try {
                listener.onDataChanged();
            } catch (Exception e) {
                Logger.error("TRACKING", "Loi thong bao listener: " + e.getMessage());
            }
        });
    }

    public void clearListeners() {
        listeners.clear();
        Logger.log("TRACKING", "Da clear tat ca listeners");
    }
}
