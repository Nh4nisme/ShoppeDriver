package com.logistics.model;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Batch implements Serializable {
    private int id;
    private final List<Order> orders;
    private final AtomicReference<BatchStatus> status;
    private int shipperId;

    public Batch() {
        this.orders = Collections.synchronizedList(new ArrayList<>());
        this.status = new AtomicReference<>(BatchStatus.PENDING);
    }

    public Batch(int id) {
        this.id = id;
        this.orders = Collections.synchronizedList(new ArrayList<>());
        this.status = new AtomicReference<>(BatchStatus.PENDING);
        this.shipperId = 0;
    }

    public int getId() {return id;}

    public List<Order> getOrders() {
        return new ArrayList<>(orders);
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public void setOrders(List<Order> orders) {
        this.orders.clear();
        this.orders.addAll(orders);
    }

    public BatchStatus getStatus() {
        return status.get();
    }

    public void setStatus(BatchStatus newStatus) {
        this.status.set(newStatus);
    }

    public int getShipperId() {
        return shipperId;
    }

    public void setShipperId(int shipperId) {
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

    public void setId(int id) {
        this.id = id;
    }
}
