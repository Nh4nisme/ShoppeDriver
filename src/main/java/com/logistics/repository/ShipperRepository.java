package com.logistics.repository;

import com.logistics.db.DBConnection;
import com.logistics.model.Shipper;
import com.logistics.model.ShipperStatus;
import com.logistics.util.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShipperRepository {

    public List<Shipper> findAll() {
        String sql = "SELECT id, name, status, current_x, current_y FROM shippers ORDER BY id";
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
                shipper.setCurrentX(rs.getDouble("current_x"));
                shipper.setCurrentY(rs.getDouble("current_y"));
                shippers.add(shipper);
            }

            Logger.log("SHIPPER", "Lấy " + shippers.size() + " shipper từ database");

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi lấy shipper: " + e.getMessage());
        }

        return shippers;
    }

    public Shipper findById(int shipperId) {
        String sql = "SELECT id, name, status, current_x, current_y FROM shippers WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, shipperId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Shipper shipper = new Shipper();
                shipper.setId(rs.getInt("id"));
                shipper.setName(rs.getString("name"));
                shipper.setStatus(ShipperStatus.valueOf(rs.getString("status")));
                shipper.setCurrentX(rs.getDouble("current_x"));
                shipper.setCurrentY(rs.getDouble("current_y"));
                return shipper;
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi tìm shipper: " + e.getMessage());
        }

        return null;
    }

    public List<Shipper> findAvailable() {
        String sql = "SELECT id, name, status, current_x, current_y FROM shippers WHERE status != 'BUSY' ORDER BY id";
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
                shipper.setCurrentX(rs.getDouble("current_x"));
                shipper.setCurrentY(rs.getDouble("current_y"));
                shippers.add(shipper);
            }

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi tìm shipper khả dụng: " + e.getMessage());
        }

        return shippers;
    }

    public boolean updateLocation(int shipperId, double x, double y) {
        String sql = "UPDATE shippers SET current_x = ?, current_y = ? WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, x);
            stmt.setDouble(2, y);
            stmt.setInt(3, shipperId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi cập nhật vị trí shipper: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(int shipperId, ShipperStatus status) {
        String sql = "UPDATE shippers SET status = ? WHERE id = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, shipperId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi cập nhật trạng thái shipper: " + e.getMessage());
            return false;
        }
    }

    public void createDefaultShippers() {
        String[] names = {"Alice", "Bob", "Charlie", "Diana"};

        for (String name : names) {
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
        String sql = "INSERT INTO shippers (id, name, current_x, current_y, status, active) VALUES (?, ?, ?, ?, 'IDLE', TRUE)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int id = (int) (Math.random() * 1000);
            double x = Math.random() * 20 + 40;
            double y = Math.random() * 20 + 40;

            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setDouble(3, x);
            stmt.setDouble(4, y);

            stmt.executeUpdate();

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi tạo shipper: " + e.getMessage());
        }
    }
}