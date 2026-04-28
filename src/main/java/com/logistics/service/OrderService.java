package com.logistics.service;

import com.logistics.util.Logger;

public class OrderService implements Runnable {
    private static final OrderService instance = new OrderService();
    private volatile boolean running = false;

    private OrderService() {
    }

    public static OrderService getInstance() {
        return instance;
    }

    @Override
    public void run() {
        running = true;
        Logger.log("SERVICE", "OrderService bắt đầu chạy");

        try {
            // Keep the service running for consistency
            while (running) {
                Thread.sleep(10000); // Sleep for 10 seconds, just to keep alive
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("SERVICE", "OrderService bị ngắt");
        } finally {
            running = false;
            Logger.log("SERVICE", "OrderService dừng");
        }
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
