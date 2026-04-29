package com.logistics.service;

import com.logistics.model.LatLng;
import com.logistics.model.Order;
import com.logistics.model.Route;
import com.logistics.repository.OrderRepository;
import com.logistics.util.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrderService implements Runnable {
    private static final OrderService instance = new OrderService();
    private volatile boolean running = false;
    private final OrderRepository orderRepository;

    private OrderService() {
        this.orderRepository = new OrderRepository();
    }

    public static OrderService getInstance() {
        return instance;
    }

    @Override
    public void run() {
        running = true;
        Logger.log("SERVICE", "OrderService bat dau chay");

        try {
            while (running) {
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log("SERVICE", "OrderService bi ngat");
        } finally {
            running = false;
            Logger.log("SERVICE", "OrderService dung");
        }
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public List<Order> findOrdersAlongRoute(Route route, double thresholdKm) {
        if (route == null) {
            return List.of();
        }

        double latPadding = kilometersToLatitudeDelta(thresholdKm);
        double centerLatitude = (route.getMinLatitude() + route.getMaxLatitude()) / 2.0;
        double lngPadding = kilometersToLongitudeDelta(thresholdKm, centerLatitude);

        List<Order> candidates = orderRepository.findOrdersInBoundingBox(
                route.getMinLatitude() - latPadding,
                route.getMinLongitude() - lngPadding,
                route.getMaxLatitude() + latPadding,
                route.getMaxLongitude() + lngPadding,
                200
        );

        List<Order> matchedOrders = new ArrayList<>();
        for (Order order : candidates) {
            LatLng orderPoint = new LatLng(order.getLatitude(), order.getLongitude());
            double minDistanceKm = calculateMinDistanceToPolyline(orderPoint, route.getPolyline());

            if (minDistanceKm <= thresholdKm) {
                matchedOrders.add(order);
            }
        }

        matchedOrders.sort(Comparator.comparingDouble(order -> {
            LatLng orderPoint = new LatLng(order.getLatitude(), order.getLongitude());
            return calculateMinDistanceToPolyline(orderPoint, route.getPolyline());
        }));
        return matchedOrders;
    }

    private double calculateMinDistanceToPolyline(LatLng point, List<LatLng> polyline) {
        if (polyline.isEmpty()) {
            return Double.MAX_VALUE;
        }
        if (polyline.size() == 1) {
            return haversineKm(point, polyline.get(0));
        }

        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < polyline.size() - 1; i++) {
            LatLng start = polyline.get(i);
            LatLng end = polyline.get(i + 1);
            double segmentDistance = perpendicularDistanceToSegment(point, start, end);
            minDistance = Math.min(minDistance, segmentDistance);
        }
        return minDistance;
    }

    private double perpendicularDistanceToSegment(LatLng point, LatLng segStart, LatLng segEnd) {
        double originLat = Math.toRadians(segStart.latitude());
        double kmPerDegreeLat = 111.32;
        double kmPerDegreeLng = 111.32 * Math.cos(originLat);

        double startX = 0.0;
        double startY = 0.0;
        double endX = (segEnd.longitude() - segStart.longitude()) * kmPerDegreeLng;
        double endY = (segEnd.latitude() - segStart.latitude()) * kmPerDegreeLat;
        double pointX = (point.longitude() - segStart.longitude()) * kmPerDegreeLng;
        double pointY = (point.latitude() - segStart.latitude()) * kmPerDegreeLat;

        double segmentX = endX - startX;
        double segmentY = endY - startY;
        double segmentLengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (segmentLengthSquared == 0.0) {
            return haversineKm(point, segStart);
        }

        double projection = ((pointX - startX) * segmentX + (pointY - startY) * segmentY) / segmentLengthSquared;
        double clampedProjection = Math.max(0.0, Math.min(1.0, projection));
        double closestX = startX + clampedProjection * segmentX;
        double closestY = startY + clampedProjection * segmentY;

        double dx = pointX - closestX;
        double dy = pointY - closestY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double haversineKm(LatLng a, LatLng b) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLng = Math.toRadians(b.longitude() - a.longitude());
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());

        double haversine = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return earthRadiusKm * c;
    }

    private double kilometersToLatitudeDelta(double km) {
        return km / 111.0;
    }

    private double kilometersToLongitudeDelta(double km, double latitude) {
        double divisor = 111.0 * Math.cos(Math.toRadians(latitude));
        if (Math.abs(divisor) < 0.0001) {
            return km / 111.0;
        }
        return km / divisor;
    }
}
