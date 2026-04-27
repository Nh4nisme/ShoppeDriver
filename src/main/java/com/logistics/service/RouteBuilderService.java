package com.logistics.service;

import com.logistics.model.Order;
import com.logistics.model.Batch;
import com.logistics.model.OrderStatus;
import com.logistics.util.QueueManager;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class RouteBuilderService implements Runnable {
    private static final RouteBuilderService instance = new RouteBuilderService();
    private volatile boolean running = false;
    private final int BATCH_SIZE_MIN = 3;
    private final int BATCH_SIZE_MAX = 5;
    private final BlockingQueue<Order> orderQueue;
    private final BlockingQueue<Batch> batchQueue;
    private final AtomicInteger batchCounter = new AtomicInteger(0);

    private RouteBuilderService() {
        this.orderQueue = QueueManager.getInstance().getOrderQueue();
        this.batchQueue = QueueManager.getInstance().getBatchQueue();
    }

    public static RouteBuilderService getInstance() {
        return instance;
    }

    @Override
    public void run() {
        running = true;
        System.out.println("[RouteBuilderService] Starting...");

        try {
            while (running) {
                List<Order> batchOrders = new ArrayList<>();

                // Wait for first order with timeout
                Order firstOrder = orderQueue.poll(2, java.util.concurrent.TimeUnit.SECONDS);
                if (firstOrder == null) {
                    continue;
                }

                batchOrders.add(firstOrder);

                // Try to collect more orders (up to BATCH_SIZE_MAX)
                orderQueue.drainTo(batchOrders, BATCH_SIZE_MAX - 1);

                // If we have at least BATCH_SIZE_MIN orders, create batch
                if (batchOrders.size() >= BATCH_SIZE_MIN) {
                    List<Order> sortedOrders = sortByNearestNeighbor(batchOrders);
                    Batch batch = new Batch("BATCH-" + batchCounter.incrementAndGet());
                    for (Order order : sortedOrders) {
                        batch.addOrder(order);
                        order.setStatus(OrderStatus.IN_DELIVERY);
                    }
                    batchQueue.put(batch);
                    System.out.println("[RouteBuilderService] Created: " + batch);
                } else {
                    // Put them back if not enough orders
                    for (Order order : batchOrders) {
                        orderQueue.put(order);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[RouteBuilderService] Interrupted!");
        } finally {
            running = false;
            System.out.println("[RouteBuilderService] Stopped!");
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

