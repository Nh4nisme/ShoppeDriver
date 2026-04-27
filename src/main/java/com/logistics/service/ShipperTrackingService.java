package com.logistics.service;

import com.logistics.model.Batch;
import com.logistics.model.Shipper;
import com.logistics.util.DataChangeListener;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShipperTrackingService {
    private static final ShipperTrackingService instance = new ShipperTrackingService();

    private final Map<String, Shipper> shippers = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Batch> batches = Collections.synchronizedMap(new HashMap<>());
    private final List<DataChangeListener> listeners = new CopyOnWriteArrayList<>();

    private ShipperTrackingService() {
    }

    public static ShipperTrackingService getInstance() {
        return instance;
    }

    public void registerShipper(Shipper shipper) {
        shippers.put(shipper.getId(), shipper);
        notifyListeners();
    }

    public Shipper getShipper(String shipperId) {
        return shippers.get(shipperId);
    }

    public Collection<Shipper> getAllShippers() {
        return new ArrayList<>(shippers.values());
    }

    public void updateShipperLocation(String shipperId, double x, double y) {
        Shipper shipper = shippers.get(shipperId);
        if (shipper != null) {
            shipper.setLocation(x, y);
            notifyListeners();
        }
    }

    public void registerBatch(Batch batch) {
        batches.put(batch.getId(), batch);
        notifyListeners();
    }

    public Batch getBatch(String batchId) {
        return batches.get(batchId);
    }

    public Collection<Batch> getAllBatches() {
        return new ArrayList<>(batches.values());
    }

    public void updateBatchStatus(String batchId) {
        Batch batch = batches.get(batchId);
        if (batch != null) {
            notifyListeners();
        }
    }

    public void addListener(DataChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(DataChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        listeners.forEach(DataChangeListener::onDataChanged);
    }

    public void clear() {
        shippers.clear();
        batches.clear();
    }
}

