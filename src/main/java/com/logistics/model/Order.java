package com.logistics.model;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

public class Order implements Serializable {
    private int id;
    private double x;
    private double y;
    private final AtomicReference<OrderStatus> status;

    public Order(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.status = new AtomicReference<>(OrderStatus.PENDING);
    }

    public Order() {
       this(0, 0, 0);
    }

    public Order(double x, double y) {
        this(0, x, y);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
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
