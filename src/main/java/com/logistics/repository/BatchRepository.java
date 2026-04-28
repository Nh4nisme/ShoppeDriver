package com.logistics.repository;

import com.logistics.db.DBConnection;
import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.util.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BatchRepository {

    public Batch save(Batch batch) {
        String sql = "INSERT INTO batch (id, status, shipper_id) VALUES (?, ?, ?)";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, batch.getId());
            stmt.setString(2, batch.getStatus().name());
            stmt.setInt(3, batch.getShipperId());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                Logger.log("BATCH", "Tạo batch: ID=" + batch.getId());
                return batch;
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lưu batch: " + e.getMessage());
        }

        return null;
    }

    public List<Batch> findByStatus(BatchStatus status) {
        String sql = "SELECT id, status, shipper_id FROM batch WHERE status = ? ORDER BY id";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        List<Batch> batches = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Batch batch = new Batch();
                batch.setId(rs.getInt("id"));
                batch.setStatus(BatchStatus.valueOf(rs.getString("status")));
                batch.setShipperId(rs.getInt("shipper_id"));

                batch.setOrders(findOrdersByBatchId(batch.getId()));

                batches.add(batch);
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lấy batch: " + e.getMessage());
        }

        return batches;
    }

    private List<Order> findOrdersByBatchId(int batchId) {
        String sql = "SELECT id, x, y, status FROM orders WHERE batch_id = ? ORDER BY id";

        List<Order> orders = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, batchId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setX(rs.getDouble("x"));
                order.setY(rs.getDouble("y"));

                String dbStatus = rs.getString("status");

                // FIX ENUM mismatch
                if ("DELIVERING".equals(dbStatus)) {
                    order.setStatus(OrderStatus.DELIVERING);
                } else {
                    order.setStatus(OrderStatus.valueOf(dbStatus));
                }

                orders.add(order);
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lấy orders của batch: " + e.getMessage());
        }

        return orders;
    }

    public boolean updateStatus(int batchId, BatchStatus status) {
        String sql = "UPDATE batch SET status = ? WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, batchId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi update batch: " + e.getMessage());
            return false;
        }
    }

    public Batch findByShipperAndStatus(int shipperId, BatchStatus status) {
        String sql = "SELECT id, status, shipper_id FROM batch WHERE shipper_id = ? AND status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, shipperId);
            stmt.setString(2, status.name());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Batch batch = new Batch();
                batch.setId(rs.getInt("id"));
                batch.setStatus(BatchStatus.valueOf(rs.getString("status")));
                batch.setShipperId(rs.getInt("shipper_id"));

                // load orders
                batch.setOrders(findOrdersByBatchId(batch.getId()));

                return batch;
            }

        } catch (Exception e) {
            Logger.error("DATABASE", "Lỗi findByShipperAndStatus: " + e.getMessage());
        }

        return null;
    }

}

