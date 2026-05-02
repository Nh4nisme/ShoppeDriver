package com.logistics.repository;

import com.logistics.model.Order;
import com.logistics.model.OrderStatus;

import java.util.List;

/**
 * Repository interface for order operations
 */
public interface OrderRepository {

    Order save(Order order);

    Order findById(int orderId);

    List<Order> findByStatus(OrderStatus status);

    boolean updateStatus(int orderId, OrderStatus status);

    boolean assignToBatch(int orderId, int batchId);

    List<Order> findOrdersInBoundingBox(double startX, double startY, double endX, double endY, int maxCount);

    List<Order> findByDistrictAndBoundingBox(String district, double minX, double maxX, double minY, double maxY, int maxCount);

    double calculateDistance(double x1, double y1, double x2, double y2);
}
