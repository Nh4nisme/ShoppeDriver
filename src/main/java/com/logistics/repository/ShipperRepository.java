package com.logistics.repository;

import com.logistics.db.DBConnection;
import com.logistics.model.Shipper;
import com.logistics.model.ShipperStatus;
import com.logistics.util.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for shipper operations
 */
public class ShipperRepository {

    /**
     * Find all shippers
     * @return list of all shippers
     */
    public List<Shipper> findAll() {
        String sql = "SELECT id, name, status, location_x, location_y FROM shippers ORDER BY id";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        List<Shipper> shippers = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Shipper shipper = new Shipper();
                shipper.setId(rs.getInt("id"));
                shipper.setName(rs.getString("name"));
                shipper.setStatus(ShipperStatus.valueOf(rs.getString("status")));
                shipper.setCurrentX(rs.getDouble("location_x"));
                shipper.setCurrentY(rs.getDouble("location_y"));
                shippers.add(shipper);
            }

            Logger.log("SHIPPER", "Lấy " + shippers.size() + " shipper từ database");

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lấy shipper: " + e.getMessage());
        }

        return shippers;
    }

    /**
     * Find shipper by ID
     * @param shipperId the shipper ID
     * @return Shipper object or null
     */
    public Shipper findById(String shipperId) {
        String sql = "SELECT id, name, status, location_x, location_y FROM shippers WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, shipperId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Shipper shipper = new Shipper();
                shipper.setId(rs.getInt("id"));
                shipper.setName(rs.getString("name"));
                shipper.setStatus(ShipperStatus.valueOf(rs.getString("status")));
                shipper.setCurrentX(rs.getDouble("location_x"));
                shipper.setCurrentY(rs.getDouble("location_y"));
                return shipper;
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi tìm shipper: " + e.getMessage());
        }

        return null;
    }

    /**
     * Find available shippers (not busy)
     * @return list of available shippers
     */
    public List<Shipper> findAvailable() {
        String sql = "SELECT id, name, status, location_x, location_y FROM shippers WHERE status != 'BUSY' ORDER BY id";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        List<Shipper> shippers = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Shipper shipper = new Shipper();
                shipper.setId(rs.getInt("id"));
                shipper.setName(rs.getString("name"));
                shipper.setStatus(ShipperStatus.valueOf(rs.getString("status")));
                shipper.setCurrentX(rs.getDouble("location_x"));
                shipper.setCurrentY(rs.getDouble("location_y"));
                shippers.add(shipper);
            }

            Logger.log("SHIPPER", "Tìm thấy " + shippers.size() + " shipper khả dụng");

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi tìm shipper khả dụng: " + e.getMessage());
        }

        return shippers;
    }

    /**
     * Update shipper location
     * @param shipperId the shipper ID
     * @param x new X coordinate
     * @param y new Y coordinate
     * @return true if update successful
     */
    public boolean updateLocation(int shipperId, double x, double y) {
        String sql = "UPDATE shippers SET location_x = ?, location_y = ? WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, x);
            stmt.setDouble(2, y);
            stmt.setInt(3, shipperId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                Logger.log("SHIPPER", "Cập nhật vị trí shipper " + shipperId + " → (" + x + "," + y + ")");
                return true;
            }
            return false;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi cập nhật vị trí shipper: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update shipper status
     * @param shipperId the shipper ID
     * @param status the new status
     * @return true if update successful
     */
    public boolean updateStatus(int shipperId, ShipperStatus status) {
        String sql = "UPDATE shippers SET status = ? WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, shipperId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                Logger.log("SHIPPER", "Cập nhật trạng thái shipper " + shipperId + " → " + status);
                return true;
            }
            return false;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi cập nhật trạng thái shipper: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create default shippers if not exist
     */
    public void createDefaultShippers() {
        String[] shipperNames = {"Alice", "Bob", "Charlie", "Diana"};

        for (String name : shipperNames) {
            if (!shipperExists(name)) {
                createShipper(name);
            }
        }
    }

    private boolean shipperExists(String name) {
        String sql = "SELECT COUNT(*) FROM shippers WHERE name = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            return false;
        }
    }

    private void createShipper(String name) {
        String shipperId = "S" + String.format("%03d", (int)(Math.random() * 1000));
        String sql = "INSERT INTO shippers (id, name, status, location_x, location_y) VALUES (?, ?, 'AVAILABLE', ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Random starting location
            double x = Math.random() * 20 + 40; // 40-60
            double y = Math.random() * 20 + 40; // 40-60

            stmt.setString(1, shipperId);
            stmt.setString(2, name);
            stmt.setDouble(3, x);
            stmt.setDouble(4, y);

            stmt.executeUpdate();
            Logger.log("SHIPPER", "Tạo shipper: " + name + " (ID: " + shipperId + ")");

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi tạo shipper: " + e.getMessage());
        }
    }
}
