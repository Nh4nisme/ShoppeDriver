package com.logistics.model;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Batch implements Serializable {
    private final String id;
    private final List<Order> orders;
    private final AtomicReference<BatchStatus> status;
    private String shipperId;

    public Batch(String id) {
        this.id = id;
        this.orders = Collections.synchronizedList(new ArrayList<>());
        this.status = new AtomicReference<>(BatchStatus.PENDING);
        this.shipperId = null;
    }

    public String getId() {
        return id;
    }

    public List<Order> getOrders() {
        return new ArrayList<>(orders);
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public BatchStatus getStatus() {
        return status.get();
    }

    public void setStatus(BatchStatus newStatus) {
        this.status.set(newStatus);
    }

    public String getShipperId() {
        return shipperId;
    }

    public void setShipperId(String shipperId) {
        this.shipperId = shipperId;
    }

    public int getOrderCount() {
        return orders.size();
    }

    public int getDeliveredCount() {
        return (int) orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DONE)
                .count();
    }

    @Override
    public String toString() {
        return "Batch{" +
                "id='" + id + '\'' +
                ", orders=" + orders.size() +
                ", status=" + status.get() +
                ", shipperId='" + shipperId + '\'' +
                '}';
    }
}

