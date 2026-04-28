package com.logistics.service;

import com.logistics.model.Order;
import com.logistics.model.Batch;
import com.logistics.model.OrderStatus;
import com.logistics.model.BatchStatus;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.BatchRepository;
import com.logistics.util.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RouteBuilderService implements Runnable {

    private static final RouteBuilderService instance = new RouteBuilderService();

    private volatile boolean running = false;

    private final int BATCH_SIZE_MIN = 3;
    private final int BATCH_SIZE_MAX = 5;

    private final OrderRepository orderRepository;
    private final BatchRepository batchRepository;

    private final AtomicInteger batchCounter = new AtomicInteger(0);

    private RouteBuilderService() {
        this.orderRepository = new OrderRepository();
        this.batchRepository = new BatchRepository();
    }

    public static RouteBuilderService getInstance() {
        return instance;
    }

    @Override
    public void run() {
        running = true;
        Logger.log("SERVICE", "RouteBuilderService bắt đầu chạy (User-Driven Mode)");

        try {
            // Keep service running but wait for manual batch creation requests
            while (running) {
                Thread.sleep(10000); // Keep alive, no automatic batch creation
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("SERVICE", "RouteBuilderService bị ngắt");

        } finally {
            running = false;
            Logger.log("SERVICE", "RouteBuilderService dừng");
        }
    }

    /**
     * Create batch from route coordinates (User-Driven)
     * @param startX Starting point X coordinate
     * @param startY Starting point Y coordinate
     * @param endX Ending point X coordinate
     * @param endY Ending point Y coordinate
     * @param maxBatchSize Maximum number of orders in batch
     * @return Created batch with filtered orders
     */
    public Batch createBatchFromRoute(double startX, double startY, double endX, double endY, int maxBatchSize) {
        Logger.log("BATCH", "Tạo batch từ route: (" + startX + "," + startY + ") → (" + endX + "," + endY + ")");

        // Get orders within bounding box
        List<Order> filteredOrders = orderRepository.findOrdersInBoundingBox(startX, startY, endX, endY, maxBatchSize);

        if (filteredOrders.isEmpty()) {
            Logger.log("BATCH", "Không tìm thấy đơn hàng trong vùng chỉ định");
            return null;
        }

        // Sort by nearest neighbor
        List<Order> sortedOrders = sortByNearestNeighbor(filteredOrders);

        // Create batch
        Batch batch = new Batch();
        batch.setStatus(BatchStatus.CREATED);

        Batch savedBatch = batchRepository.save(batch);

        if (savedBatch != null) {
            // Assign orders to batch
            for (Order order : sortedOrders) {
                boolean assigned = orderRepository.assignToBatch(order.getId(), savedBatch.getId());
                if (assigned) {
                    Logger.log("BATCH", "Gán order " + order.getId() + " vào batch " + savedBatch.getId());
                } else {
                    Logger.error("BATCH", "Gán order thất bại: " + order.getId());
                }
            }

            Logger.log("BATCH", "Tạo batch: ID=" + savedBatch.getId() + ", số đơn=" + sortedOrders.size());
            return savedBatch;
        }

        return null;
    }

    // ============================
    // ROUTE LOGIC
    // ============================

    private List<Order> sortByNearestNeighbor(List<Order> orders) {
        List<Order> sorted = new ArrayList<>();
        List<Order> remaining = new ArrayList<>(orders);

        if (remaining.isEmpty()) return sorted;

        Order current = remaining.remove(0);
        sorted.add(current);

        while (!remaining.isEmpty()) {
            Order nearest = findNearest(current, remaining);
            sorted.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }

        return sorted;
    }

    private Order findNearest(Order from, List<Order> candidates) {
        Order nearest = candidates.get(0);
        double minDistance = distance(from, nearest);

        for (Order candidate : candidates) {
            double dist = distance(from, candidate);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private double distance(Order o1, Order o2) {
        double dx = o1.getX() - o2.getX();
        double dy = o1.getY() - o2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}