package com.logistics.repository;

import com.logistics.db.DBConnection;
import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.util.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for order operations
 */
public class OrderRepository {

    /**
     * Save new order to database
     * @param order the order to save
     * @return the saved order with generated ID
     */
    public Order save(Order order) {
        String sql = "INSERT INTO orders (x, y, status) VALUES (?, ?, ?)";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setDouble(1, order.getX());
            stmt.setDouble(2, order.getY());
            stmt.setString(3, order.getStatus().name());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    order.setId(rs.getInt(1));
                }
                Logger.log("ORDER", "Tạo đơn hàng: ID=" + order.getId());
                return order;
            }
            return null;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lưu đơn hàng: " + e.getMessage());
            return null;
        }
    }

    /**
     * Find orders by status
     * @param status the order status
     * @return list of orders with the status
     */
    public List<Order> findByStatus(OrderStatus status) {
        String sql = "SELECT id, x, y, status FROM orders WHERE status = ? ORDER BY id ASC";
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
                order.setStatus(OrderStatus.valueOf(rs.getString("status")));
                orders.add(order);
            }

            Logger.log("ORDER", "Lấy " + orders.size() + " đơn từ database");

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lấy đơn hàng: " + e.getMessage());
        }

        return orders;
    }

    /**
     * Update order status
     * @param orderId the order ID
     * @param status the new status
     * @return true if update successful
     */
    public boolean updateStatus(int orderId, OrderStatus status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, orderId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                Logger.log("ORDER", "Cập nhật trạng thái đơn " + orderId + " → " + status);
                return true;
            }
            return false;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi cập nhật đơn hàng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find order by ID
     * @param orderId the order ID
     * @return Order object or null
     */
    public Order findById(String orderId) {
        String sql = "SELECT id, x, y, status FROM orders WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, orderId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setX(rs.getDouble("x"));
                order.setY(rs.getDouble("y"));
                order.setStatus(OrderStatus.valueOf(rs.getString("status")));
                return order;
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi tìm đơn hàng: " + e.getMessage());
        }

        return null;
    }
}
