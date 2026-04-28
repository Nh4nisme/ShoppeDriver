package com.logistics.service;

import com.logistics.model.Order;
import com.logistics.repository.OrderRepository;
import com.logistics.util.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderService implements Runnable {
    private static final OrderService instance = new OrderService();
    private volatile boolean running = false;
    private final OrderRepository orderRepository;
    private final AtomicInteger orderCounter = new AtomicInteger(0);

    private OrderService() {
        this.orderRepository = new OrderRepository();
    }

    public static OrderService getInstance() {
        return instance;
    }

    @Override
    public void run() {
        running = true;
        Logger.log("SERVICE", "OrderService bắt đầu chạy");

        try {
            // Generate initial batch of orders
            generateMockOrders(15);

            // Periodically add new orders
            int count = 0;
            while (running && count < 10) {
                Thread.sleep(5000); // Add orders every 5 seconds
                generateMockOrders(2);
                count++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("SERVICE", "OrderService bị ngắt");
        } finally {
            running = false;
            Logger.log("SERVICE", "OrderService dừng");
        }
    }

    private void generateMockOrders(int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            double x = Math.random() * 100;
            double y = Math.random() * 100;
            Order order = new Order(x, y);
            orderRepository.save(order);
            Logger.log("ORDER", "Tạo đơn hàng: ID=" + order.getId());
        }
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
