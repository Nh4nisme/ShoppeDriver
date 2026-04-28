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

/**
 * Repository for batch operations
 */
public class BatchRepository {

    /**
     * Save new batch to database
     * @param batch the batch to save
     * @return the saved batch with generated ID
     */
    public Batch save(Batch batch) {
        String sql = "INSERT INTO batches (id, status, shipper_id) VALUES (?, ?, ?)";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, batch.getId());
            stmt.setString(2, batch.getStatus().name());
            stmt.setInt(3, batch.getShipperId());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                Logger.log("BATCH", "Tạo batch: ID=" + batch.getId() + ", số đơn=" + batch.getOrders().size());
                return batch;
            }
            return null;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lưu batch: " + e.getMessage());
            return null;
        }
    }

    /**
     * Save batch orders relationship
     * @param batchId the batch ID
     * @param orderIds list of order IDs
     */
    public void saveBatchOrders(int batchId, List<Integer> orderIds) {
        String sql = "INSERT INTO batch_orders (batch_id, order_id) VALUES (?, ?)";
        Logger.log("DATABASE", "Thực thi query batch_orders");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Integer orderId : orderIds) {
                stmt.setInt(1, batchId);
                stmt.setInt(2, orderId);
                stmt.executeUpdate();
            }

            Logger.log("BATCH", "Lưu " + orderIds.size() + " đơn vào batch " + batchId);

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lưu batch orders: " + e.getMessage());
        }
    }

    /**
     * Find batches by status
     * @param status the batch status
     * @return list of batches with the status
     */
    public List<Batch> findByStatus(BatchStatus status) {
        String sql = "SELECT b.id, b.status, b.shipper_id FROM batches b WHERE b.status = ? ORDER BY b.id";
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

                // Load orders for this batch
                batch.setOrders(findOrdersByBatchId(batch.getId()));

                batches.add(batch);
            }

            Logger.log("BATCH", "Lấy " + batches.size() + " batch từ database");

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lấy batch: " + e.getMessage());
        }

        return batches;
    }

    /**
     * Find orders for a specific batch
     * @param batchId the batch ID
     * @return list of orders in the batch
     */
    private List<Order> findOrdersByBatchId(int batchId) {
        String sql = "SELECT o.id, o.x, o.y, o.status FROM orders o " +
                    "INNER JOIN batch_orders bo ON o.id = bo.order_id " +
                    "WHERE bo.batch_id = ? ORDER BY o.id";

        List<Order> orders = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, batchId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Order order = new Order(rs.getInt("id"), rs.getDouble("x"), rs.getDouble("y"));
                order.setStatus(OrderStatus.valueOf(rs.getString("status")));
                orders.add(order);
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lấy orders cho batch: " + e.getMessage());
        }

        return orders;
    }

    /**
     * Find batch by shipper ID and status
     * @param shipperId the shipper ID
     * @param status the batch status
     * @return Batch object or null
     */
    public Batch findByShipperAndStatus(String shipperId, BatchStatus status) {
        String sql = "SELECT id, status, shipper_id FROM batches WHERE shipper_id = ? AND status = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, shipperId);
            stmt.setString(2, status.name());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Batch batch = new Batch();
                batch.setId(rs.getInt("id"));
                batch.setStatus(BatchStatus.valueOf(rs.getString("status")));
                batch.setShipperId(rs.getInt("shipper_id"));

                // Load orders for this batch
                batch.setOrders(findOrdersByBatchId(batch.getId()));

                return batch;
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi tìm batch theo shipper: " + e.getMessage());
        }

        return null;
    }

    /**
     * Update batch status
     * @param batchId the batch ID
     * @param status the new status
     * @return true if update successful
     */
    public boolean updateStatus(int batchId, BatchStatus status) {
        String sql = "UPDATE batches SET status = ? WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, batchId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                Logger.log("BATCH", "Cập nhật trạng thái batch " + batchId + " → " + status);
                return true;
            }
            return false;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi cập nhật batch: " + e.getMessage());
            return false;
        }
    }
}
