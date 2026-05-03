package com.logistics.service;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.repository.BatchRepository;
import com.logistics.repository.BatchRepositoryImpl;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.OrderRepositoryImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BatchService {
    private final BatchRepository batchRepository;
    private final OrderRepository orderRepository;

    public BatchService() {
        this.batchRepository = new BatchRepositoryImpl();
        this.orderRepository = new OrderRepositoryImpl();
    }

    public Batch createBatch(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("No orders selected");
        }

        Map<Integer, Order> uniqueOrders = new LinkedHashMap<>();
        for (Order order : orders) {
            if (order != null) {
                uniqueOrders.put(order.getId(), order);
            }
        }

        if (uniqueOrders.isEmpty()) {
            throw new IllegalArgumentException("No orders selected");
        }

        List<Order> persistedOrders = new ArrayList<>();
        for (Order candidate : uniqueOrders.values()) {
            Order current = orderRepository.findById(candidate.getId());
            if (current == null) {
                throw new IllegalStateException("Order not found: " + candidate.getId());
            }
            if (current.getStatus() != OrderStatus.PENDING) {
                throw new IllegalStateException("Order is not available: " + candidate.getId());
            }
            persistedOrders.add(current);
        }

        Batch batch = new Batch();
        batch.setStatus(BatchStatus.CREATED);
        batch.setOrders(new ArrayList<>()); // Avoid detached entity exception

        Batch saved = batchRepository.save(batch);
        if (saved == null) {
            throw new IllegalStateException("Could not create batch");
        }

        for (Order order : persistedOrders) {
            boolean assigned = orderRepository.assignToBatch(order.getId(), saved.getId());
            if (!assigned) {
                throw new IllegalStateException("Could not assign order to batch: " + order.getId());
            }
        }

        saved.setOrders(persistedOrders);
        ShipperTrackingService.getInstance().notifyBatchUpdated(saved);
        return saved;
    }
}
