package com.logistics.repository;

import com.logistics.db.DBConnection;
import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class BatchRepository {

    public Batch save(Batch batch) {
        String sql = "INSERT INTO batch (status, shipper_id) VALUES (?, ?)";
        Logger.log("DATABASE", "Thuc thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, batch.getStatus().name());
            if (batch.getShipperId() > 0) {
                stmt.setInt(2, batch.getShipperId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    batch.setId(rs.getInt(1));
                }
                Logger.log("BATCH", "Tao batch: ID=" + batch.getId());
                return batch;
            }
        } catch (SQLException e) {
            Logger.error("DATABASE", "Loi luu batch: " + e.getMessage());
        }

        return null;
    }

    public List<Batch> findByStatus(BatchStatus status) {
        String sql = "SELECT id, status, shipper_id FROM batch WHERE status = ? ORDER BY id";
        Logger.log("DATABASE", "Thuc thi query: " + sql);

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
            Logger.error("DATABASE", "Loi lay batch: " + e.getMessage());
        }

        return batches;
    }

    public Batch findById(int batchId) {
        String sql = "SELECT id, status, shipper_id FROM batch WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, batchId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Batch batch = new Batch();
                batch.setId(rs.getInt("id"));
                batch.setStatus(BatchStatus.valueOf(rs.getString("status")));
                batch.setShipperId(rs.getInt("shipper_id"));
                batch.setOrders(findOrdersByBatchId(batch.getId()));
                return batch;
            }
        } catch (SQLException e) {
            Logger.error("DATABASE", "Loi findById batch: " + e.getMessage());
        }

        return null;
    }

    private List<Order> findOrdersByBatchId(int batchId) {
        String sql = "SELECT id, x, y, address, status FROM orders WHERE batch_id = ? ORDER BY id";
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
                order.setAddress(rs.getString("address"));

                String dbStatus = rs.getString("status");
                if ("DELIVERING".equals(dbStatus)) {
                    order.setStatus(OrderStatus.DELIVERING);
                } else {
                    order.setStatus(OrderStatus.valueOf(dbStatus));
                }
                orders.add(order);
            }
        } catch (SQLException e) {
            Logger.error("DATABASE", "Loi lay orders cua batch: " + e.getMessage());
        }

        return orders;
    }

    public boolean updateStatus(int batchId, BatchStatus status) {
        String sql = "UPDATE batch SET status = ? WHERE id = ?";
        Logger.log("DATABASE", "Thuc thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, batchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Logger.error("DATABASE", "Loi update batch: " + e.getMessage());
            return false;
        }
    }

    public boolean assignToShipper(int batchId, int shipperId, BatchStatus status) {
        String sql = "UPDATE batch SET shipper_id = ?, status = ? WHERE id = ?";
        Logger.log("DATABASE", "Thuc thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, shipperId);
            stmt.setString(2, status.name());
            stmt.setInt(3, batchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Logger.error("DATABASE", "Loi assign shipper cho batch: " + e.getMessage());
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
                batch.setOrders(findOrdersByBatchId(batch.getId()));
                return batch;
            }
        } catch (Exception e) {
            Logger.error("DATABASE", "Loi findByShipperAndStatus: " + e.getMessage());
        }

        return null;
    }

    public List<Batch> findActiveByShipper(int shipperId) {
        String sql = "SELECT id, status, shipper_id FROM batch " +
                "WHERE shipper_id = ? AND status IN ('ASSIGNED', 'IN_DELIVERY', 'COMPLETED') ORDER BY id DESC";
        List<Batch> batches = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, shipperId);
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
            Logger.error("DATABASE", "Loi findActiveByShipper: " + e.getMessage());
        }

        return batches;
    }
}
