package com.logistics.repository;

import com.logistics.db.DBConnection;
import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.util.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderRepository {

    public Order save(Order order) {
        String sql = "INSERT INTO orders (x, y, status, batch_id) VALUES (?, ?, ?, ?)";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDouble(1, order.getX());
            stmt.setDouble(2, order.getY());
            stmt.setString(3, order.getStatus().name());

            if (order.getId() == 0) {
                stmt.setNull(4, Types.INTEGER);
            } else {
                stmt.setInt(4, order.getId());
            }

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    order.setId(rs.getInt(1));
                }
                return order;
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lưu đơn hàng: " + e.getMessage());
        }

        return null;
    }

    public List<Order> findByStatus(OrderStatus status) {
        String sql = "SELECT id, x, y, status, batch_id FROM orders WHERE status = ? ORDER BY id";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        List<Order> orders = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setX(rs.getDouble("x"));
                order.setY(rs.getDouble("y"));

                String dbStatus = rs.getString("status");

                // FIX mismatch enum
                if ("DELIVERING".equals(dbStatus)) {
                    order.setStatus(OrderStatus.DELIVERING);
                } else {
                    order.setStatus(OrderStatus.valueOf(dbStatus));
                }

                orders.add(order);
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lấy đơn hàng: " + e.getMessage());
        }

        return orders;
    }

    public boolean updateStatus(int orderId, OrderStatus status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, orderId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi update order: " + e.getMessage());
            return false;
        }
    }

    public boolean assignToBatch(int orderId, int batchId) {
        String sql = "UPDATE orders SET batch_id = ?, status = 'IN_BATCH' WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, batchId);
            stmt.setInt(2, orderId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi assign batch: " + e.getMessage());
            return false;
        }
    }

    public List<Order> findOrdersInBoundingBox(double startX, double startY, double endX, double endY, int maxCount) {
        double minX = Math.min(startX, endX);
        double maxX = Math.max(startX, endX);
        double minY = Math.min(startY, endY);
        double maxY = Math.max(startY, endY);

        String sql = "SELECT id, x, y, status, batch_id FROM orders WHERE status = 'PENDING' AND x >= ? AND x <= ? AND y >= ? AND y <= ? ORDER BY id LIMIT ?";
        Logger.log("DATABASE", "Thực thi query lọc orders trong bounding box: [" + minX + "," + maxX + "] x [" + minY + "," + maxY + "]");

        List<Order> orders = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, minX);
            stmt.setDouble(2, maxX);
            stmt.setDouble(3, minY);
            stmt.setDouble(4, maxY);
            stmt.setInt(5, maxCount);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setX(rs.getDouble("x"));
                order.setY(rs.getDouble("y"));

                String dbStatus = rs.getString("status");

                if ("DELIVERING".equals(dbStatus)) {
                    order.setStatus(OrderStatus.DELIVERING);
                } else {
                    order.setStatus(OrderStatus.valueOf(dbStatus));
                }

                orders.add(order);
            }

            Logger.log("BATCH", "Tìm thấy " + orders.size() + " đơn trong bounding box");

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lấy đơn hàng trong vùng: " + e.getMessage());
        }

        return orders;
    }

    public static double calculateDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
}