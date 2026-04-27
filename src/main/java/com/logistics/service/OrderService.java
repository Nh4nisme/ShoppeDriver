package com.logistics.service;

import com.logistics.model.Order;
import com.logistics.util.QueueManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderService implements Runnable {
    private static final OrderService instance = new OrderService();
    private volatile boolean running = false;
    private final BlockingQueue<Order> orderQueue;
    private final AtomicInteger orderCounter = new AtomicInteger(0);

    private OrderService() {
        this.orderQueue = QueueManager.getInstance().getOrderQueue();
    }

    public static OrderService getInstance() {
        return instance;
    }

    @Override
    public void run() {
        running = true;
        System.out.println("[OrderService] Starting...");

        try {
            // Generate initial batch of orders
            generateMockOrders(15);

            // Periodically add new orders
            int count = 0;
            while (running && count < 30) {
                Thread.sleep(5000); // Add orders every 5 seconds
                generateMockOrders(2);
                count++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[OrderService] Interrupted!");
        } finally {
            running = false;
            System.out.println("[OrderService] Stopped!");
        }
    }

    private void generateMockOrders(int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            double x = Math.random() * 100;
            double y = Math.random() * 100;
            Order order = new Order("ORD-" + orderCounter.incrementAndGet(), x, y);
            orderQueue.put(order);
            System.out.println("[OrderService] Generated: " + order);
        }
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}

