package com.logistics.db;

import com.logistics.repository.AuthRepository;
import com.logistics.repository.ShipperRepository;
import com.logistics.util.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database initialization utility
 */
public class DatabaseInitializer {

    /**
     * Initialize database schema and default data
     */
    public static void initialize() {
        Logger.log("SYSTEM", "Khởi tạo database...");

        try {
            createTables();
            createDefaultData();
            Logger.log("SYSTEM", "Khởi tạo database thành công");

        } catch (SQLException e) {
            Logger.error("SYSTEM", "Lỗi khởi tạo database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Create database tables
     */
    private static void createTables() throws SQLException {
        Logger.log("DATABASE", "Đang kết nối MariaDB...");

        if (!DBConnection.testConnection()) {
            throw new SQLException("Cannot connect to database");
        }

        Logger.log("DATABASE", "Kết nối database thành công");

        String[] createTableStatements = {
            // Users table
            "CREATE TABLE IF NOT EXISTS users (" +
            "id VARCHAR(50) PRIMARY KEY," +
            "username VARCHAR(50) UNIQUE NOT NULL," +
            "password VARCHAR(255) NOT NULL," +
            "role VARCHAR(20) NOT NULL" +
            ")",

            // Shippers table
            "CREATE TABLE IF NOT EXISTS shippers (" +
            "id VARCHAR(50) PRIMARY KEY," +
            "name VARCHAR(100) NOT NULL," +
            "status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'," +
            "location_x DOUBLE NOT NULL DEFAULT 0.0," +
            "location_y DOUBLE NOT NULL DEFAULT 0.0" +
            ")",

            // Orders table
            "CREATE TABLE IF NOT EXISTS orders (" +
            "id VARCHAR(50) PRIMARY KEY," +
            "x DOUBLE NOT NULL," +
            "y DOUBLE NOT NULL," +
            "status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")",

            // Batches table
            "CREATE TABLE IF NOT EXISTS batches (" +
            "id VARCHAR(50) PRIMARY KEY," +
            "status VARCHAR(20) NOT NULL DEFAULT 'CREATED'," +
            "shipper_id VARCHAR(50)," +
            "FOREIGN KEY (shipper_id) REFERENCES shippers(id)" +
            ")",

            // Batch orders relationship table
            "CREATE TABLE IF NOT EXISTS batch_orders (" +
            "batch_id VARCHAR(50) NOT NULL," +
            "order_id VARCHAR(50) NOT NULL," +
            "PRIMARY KEY (batch_id, order_id)," +
            "FOREIGN KEY (batch_id) REFERENCES batches(id) ON DELETE CASCADE," +
            "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
            ")"
        };

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : createTableStatements) {
                Logger.log("DATABASE", "Tạo bảng: " + sql.substring(0, Math.min(50, sql.length())) + "...");
                stmt.execute(sql);
            }

            Logger.log("DATABASE", "Tạo tables thành công");

        } catch (SQLException e) {
            Logger.error("DATABASE", "Lỗi tạo tables: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create default data
     */
    private static void createDefaultData() {
        Logger.log("DATABASE", "Tạo dữ liệu mặc định...");

        // Create default admin user
        AuthRepository authRepo = new AuthRepository();
        authRepo.createDefaultAdmin();

        // Create default shippers
        ShipperRepository shipperRepo = new ShipperRepository();
        shipperRepo.createDefaultShippers();

        Logger.log("DATABASE", "Tạo dữ liệu mặc định thành công");
    }
}
