package com.logistics.service;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Shipper;
import com.logistics.model.ShipperStatus;
import com.logistics.repository.BatchRepository;
import com.logistics.repository.BatchRepositoryImpl;
import com.logistics.repository.ShipperRepository;
import com.logistics.repository.ShipperRepositoryImpl;
import com.logistics.util.Logger;

import java.util.List;

public class DispatcherService {
    private static final DispatcherService instance = new DispatcherService();
    private final BatchRepository batchRepository;
    private final ShipperRepository shipperRepository;

    private DispatcherService() {
        this.batchRepository = new BatchRepositoryImpl();
        this.shipperRepository = new ShipperRepositoryImpl();
    }

    public static DispatcherService getInstance() {
        return instance;
    }

    public void assignBatchToShipper(String batchId, String shipperId) {
        if (batchId == null || shipperId == null || batchId.isBlank() || shipperId.isBlank()) {
            throw new IllegalArgumentException("Batch ID and shipper ID are required");
        }

        int parsedBatchId = Integer.parseInt(batchId);
        int parsedShipperId = Integer.parseInt(shipperId);
        Logger.log("DISPATCH", "Gan batch " + parsedBatchId + " cho shipper " + parsedShipperId);

        Batch batch = batchRepository.findById(parsedBatchId);
        if (batch == null) {
            throw new IllegalArgumentException("Batch not found");
        }

        boolean batchUpdated = batchRepository.assignToShipper(parsedBatchId, parsedShipperId, BatchStatus.ASSIGNED);
        boolean shipperUpdated = shipperRepository.updateStatus(parsedShipperId, ShipperStatus.BUSY);
        if (!batchUpdated || !shipperUpdated) {
            throw new IllegalStateException("Assignment failed");
        }
    }

    public boolean assignBatchToShipper(int batchId, int shipperId) {
        try {
            assignBatchToShipper(String.valueOf(batchId), String.valueOf(shipperId));
            return true;
        } catch (Exception e) {
            Logger.error("DISPATCH", "Loi gan batch: " + e.getMessage());
            return false;
        }
    }

    public List<Shipper> getAvailableShippers() {
        return shipperRepository.findAvailable();
    }
}
