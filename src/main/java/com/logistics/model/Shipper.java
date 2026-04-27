package com.logistics.model;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

public class Shipper implements Serializable {
    private final String id;
    private final String name;
    private final AtomicReference<Double> currentX;
    private final AtomicReference<Double> currentY;
    private final AtomicReference<ShipperStatus> status;
    private volatile boolean active;

    public Shipper(String id, String name, double startX, double startY) {
        this.id = id;
        this.name = name;
        this.currentX = new AtomicReference<>(startX);
        this.currentY = new AtomicReference<>(startY);
        this.status = new AtomicReference<>(ShipperStatus.IDLE);
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getCurrentX() {
        return currentX.get();
    }

    public double getCurrentY() {
        return currentY.get();
    }

    public void setLocation(double x, double y) {
        currentX.set(x);
        currentY.set(y);
    }

    public ShipperStatus getStatus() {
        return status.get();
    }

    public void setStatus(ShipperStatus newStatus) {
        this.status.set(newStatus);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double distanceTo(double x, double y) {
        double dx = currentX.get() - x;
        double dy = currentY.get() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return "Shipper{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", x=" + currentX.get() +
                ", y=" + currentY.get() +
                ", status=" + status.get() +
                ", active=" + active +
                '}';
    }
}

