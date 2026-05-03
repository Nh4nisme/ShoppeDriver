package com.logistics.repository;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;

import java.util.List;

/**
 * Repository interface for batch operations
 */
public interface BatchRepository {

    Batch save(Batch batch);

    Batch createWithPendingOrders(List<Integer> orderIds);

    List<Batch> findByStatus(BatchStatus status);

    Batch findById(int batchId);

    boolean updateStatus(int batchId, BatchStatus status);

    boolean assignToShipper(int batchId, int shipperId, BatchStatus status);

    Batch findByShipperAndStatus(int shipperId, BatchStatus status);

    List<Batch> findActiveByShipper(int shipperId);

    boolean removeOrderFromBatch(int orderId);
}
