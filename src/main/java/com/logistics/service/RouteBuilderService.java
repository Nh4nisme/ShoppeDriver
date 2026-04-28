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
        Logger.log("SERVICE", "RouteBuilderService bắt đầu chạy");

        try {
            while (running) {
                // Poll for pending orders every 2-5 seconds
                Thread.sleep(2000 + (int)(Math.random() * 3000));

                List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);

                if (pendingOrders.size() >= BATCH_SIZE_MIN) {
                    // Take up to BATCH_SIZE_MAX orders
                    List<Order> batchOrders = pendingOrders.subList(0,
                        Math.min(BATCH_SIZE_MAX, pendingOrders.size()));

                    // Sort by nearest neighbor
                    List<Order> sortedOrders = sortByNearestNeighbor(batchOrders);

                    // Create batch
                    Batch batch = new Batch();
                    batch.setStatus(BatchStatus.CREATED);
                    for (Order order : sortedOrders) {
                        batch.addOrder(order);
                    }

                    // Save batch to database
                    Batch savedBatch = batchRepository.save(batch);
                    if (savedBatch != null) {
                        // Save batch-order relationships
                        List<Integer> orderIds = sortedOrders.stream()
                            .map(Order::getId)
                            .toList();
                        batchRepository.saveBatchOrders(savedBatch.getId(), orderIds);

                        Logger.log("BATCH", "Tạo batch: ID=" + savedBatch.getId() + ", số đơn=" + sortedOrders.size());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("SERVICE", "RouteBuilderService bị ngắt");
        } finally {
            running = false;
            Logger.log("SERVICE", "RouteBuilderService dừng");
        }
    }

    private List<Order> sortByNearestNeighbor(List<Order> orders) {
        List<Order> sorted = new ArrayList<>();
        List<Order> remaining = new ArrayList<>(orders);

        if (remaining.isEmpty()) return sorted;

        // Start from a central point
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
