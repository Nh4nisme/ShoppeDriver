package com.logistics.model;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

public class Order implements Serializable {
    private final String id;
    private final double x;
    private final double y;
    private final AtomicReference<OrderStatus> status;

    public Order(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.status = new AtomicReference<>(OrderStatus.PENDING);
    }

    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public OrderStatus getStatus() {
        return status.get();
    }

    public void setStatus(OrderStatus newStatus) {
        this.status.set(newStatus);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", status=" + status.get() +
                '}';
    }
}

