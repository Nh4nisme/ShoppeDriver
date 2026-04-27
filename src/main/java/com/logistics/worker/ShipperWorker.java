package com.logistics.worker;

import com.logistics.model.*;
import com.logistics.service.ShipperTrackingService;
import com.logistics.util.DataChangeListener;
import com.logistics.util.LocationUtil;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ShipperWorker implements Runnable {
    private final Shipper shipper;
    private final BlockingQueue<Order> assignedOrders;
    private Order currentOrder;
    private volatile boolean autoMode = false;
    private volatile boolean running = false;
    private final List<DataChangeListener> listeners = new CopyOnWriteArrayList<>();

    public ShipperWorker(Shipper shipper) {
        this.shipper = shipper;
        this.assignedOrders = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        running = true;
        System.out.println("[ShipperWorker-" + shipper.getName() + "] Started!");

        try {
            while (running) {
                if (autoMode) {
                    // Auto-delivery mode
                    if (currentOrder == null) {
                        currentOrder = assignedOrders.poll();
                    }

                    if (currentOrder != null) {
                        moveTowardsOrder();
                        Thread.sleep(1000);
                    } else {
                        shipper.setStatus(ShipperStatus.IDLE);
                        Thread.sleep(2000);
                    }
                } else {
                    // Manual mode - wait for external delivery requests
                    Thread.sleep(500);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[ShipperWorker-" + shipper.getName() + "] Interrupted!");
        } finally {
            running = false;
            System.out.println("[ShipperWorker-" + shipper.getName() + "] Stopped!");
        }
    }

    public void assignBatch(Batch batch) {
        for (Order order : batch.getOrders()) {
            assignedOrders.offer(order);
        }
        shipper.setStatus(ShipperStatus.IN_DELIVERY);
        notifyListeners();
        System.out.println("[ShipperWorker-" + shipper.getName() + "] Batch assigned with " + batch.getOrders().size() + " orders");
    }

    public void startDelivery() {
        autoMode = true;
        System.out.println("[ShipperWorker-" + shipper.getName() + "] Auto-delivery started");
        notifyListeners();
    }

    public void stopDelivery() {
        autoMode = false;
        System.out.println("[ShipperWorker-" + shipper.getName() + "] Auto-delivery stopped");
        notifyListeners();
    }

    public void deliverNext() {
        if (currentOrder == null) {
            currentOrder = assignedOrders.poll();
        }

        if (currentOrder != null) {
            // Move directly to the order and mark as done
            shipper.setLocation(currentOrder.getX(), currentOrder.getY());
            currentOrder.setStatus(OrderStatus.DONE);
            System.out.println("[ShipperWorker-" + shipper.getName() + "] Delivered: " + currentOrder);
            
            ShipperTrackingService.getInstance().updateShipperLocation(shipper.getId(), currentOrder.getX(), currentOrder.getY());
            notifyListeners();

            // Move to next order
            currentOrder = null;
            
            if (assignedOrders.isEmpty()) {
                shipper.setStatus(ShipperStatus.IDLE);
                autoMode = false;
            }
        }
    }

    private void moveTowardsOrder() {
        if (currentOrder != null) {
            double currentX = shipper.getCurrentX();
            double currentY = shipper.getCurrentY();
            double orderX = currentOrder.getX();
            double orderY = currentOrder.getY();

            if (LocationUtil.isCloseEnough(currentX, currentY, orderX, orderY)) {
                // Reached order
                currentOrder.setStatus(OrderStatus.DONE);
                System.out.println("[ShipperWorker-" + shipper.getName() + "] Delivered: " + currentOrder);
                currentOrder = null;

                if (assignedOrders.isEmpty()) {
                    shipper.setStatus(ShipperStatus.IDLE);
                    autoMode = false;
                }
            } else {
                // Move towards order
                double[] newPos = LocationUtil.moveTowards(currentX, currentY, orderX, orderY);
                shipper.setLocation(newPos[0], newPos[1]);
                ShipperTrackingService.getInstance().updateShipperLocation(shipper.getId(), newPos[0], newPos[1]);
            }

            notifyListeners();
        }
    }

    public Shipper getShipper() {
        return shipper;
    }

    public Order getCurrentOrder() {
        return currentOrder;
    }

    public List<Order> getAssignedOrders() {
        List<Order> orders = new ArrayList<>();
        assignedOrders.drainTo(orders);
        // Put them back
        orders.forEach(o -> assignedOrders.offer(o));
        return orders;
    }

    public int getPendingOrderCount() {
        return assignedOrders.size() + (currentOrder != null ? 1 : 0);
    }

    public boolean isAutoMode() {
        return autoMode;
    }

    public void addListener(DataChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(DataChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        listeners.forEach(DataChangeListener::onDataChanged);
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}

