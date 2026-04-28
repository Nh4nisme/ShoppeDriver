package com.logistics.model;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

public class Shipper implements Serializable {
    private int id;
    private String name;
    private final AtomicReference<Double> currentX;
    private final AtomicReference<Double> currentY;
    private final AtomicReference<ShipperStatus> status;
    private volatile boolean active;

    public Shipper(int id, String name, double startX, double startY) {
        this.id = id;
        this.name = name;
        this.currentX = new AtomicReference<>(startX);
        this.currentY = new AtomicReference<>(startY);
        this.status = new AtomicReference<>(ShipperStatus.IDLE);
        this.active = true;
    }

    public Shipper() {
        this.id = 0;
        this.name = null;
        this.currentX = new AtomicReference<>(0.0);
        this.currentY = new AtomicReference<>(0.0);
        this.status = new AtomicReference<>(ShipperStatus.IDLE);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCurrentX() {
        return currentX.get();
    }

    public void setCurrentX(double currentX) {
        this.currentX.set(currentX);
    }

    public double getCurrentY() {
        return currentY.get();
    }

    public void setCurrentY(double currentY) {
        this.currentY.set(currentY);
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
