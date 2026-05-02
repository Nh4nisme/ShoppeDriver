package com.logistics.db;

import com.logistics.repository.AuthRepository;
import com.logistics.repository.AuthRepositoryImpl;
import com.logistics.repository.ShipperRepository;
import com.logistics.repository.ShipperRepositoryImpl;
import com.logistics.util.Logger;

/**
 * Database initialization utility
 */
public class DatabaseInitializer {

    /**
     * Initialize database schema and default data
     */
    public static void initialize() {
        Logger.log("SYSTEM", "Khoi tao database...");

        try {
            // Removed createTables() - use direct MariaDB connection
            createDefaultData();
            Logger.log("SYSTEM", "Khoi tao database thanh cong");

        } catch (Exception e) {
            Logger.error("SYSTEM", "Loi khoi tao database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Create default data
     */
    private static void createDefaultData() {
        Logger.log("DATABASE", "Tạo dữ liệu mặc định...");

        // Create default admin user
        AuthRepository authRepo = new AuthRepositoryImpl();
        authRepo.createDefaultAdmin();

        // Create default shippers
        ShipperRepository shipperRepo = new ShipperRepositoryImpl();
        shipperRepo.createDefaultShippers();

        Logger.log("DATABASE", "Tạo dữ liệu mặc định thành công");
    }
}
