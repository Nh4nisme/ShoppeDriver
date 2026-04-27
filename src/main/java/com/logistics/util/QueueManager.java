package com.logistics.util;

import com.logistics.model.Order;
import com.logistics.model.Batch;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueManager {
    private static final QueueManager instance = new QueueManager();

    private final BlockingQueue<Order> orderQueue;
    private final BlockingQueue<Batch> batchQueue;

    private QueueManager() {
        this.orderQueue = new LinkedBlockingQueue<>();
        this.batchQueue = new LinkedBlockingQueue<>();
    }

    public static QueueManager getInstance() {
        return instance;
    }

    public BlockingQueue<Order> getOrderQueue() {
        return orderQueue;
    }

    public BlockingQueue<Batch> getBatchQueue() {
        return batchQueue;
    }

    public void clear() {
        orderQueue.clear();
        batchQueue.clear();
    }
}

