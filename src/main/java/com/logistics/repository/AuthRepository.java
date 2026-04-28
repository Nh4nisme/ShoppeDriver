package com.logistics.repository;

import com.logistics.db.DBConnection;
import com.logistics.model.User;
import com.logistics.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Repository for authentication operations
 */
public class AuthRepository {

    /**
     * Find user by username
     * @param username the username to search for
     * @return User object or null if not found
     */
    public User findByUsername(String username) {
        String sql = "SELECT id, username, password, role FROM users WHERE username = ?";
        Logger.log("DATABASE", "Thực thi query: " + sql);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                Logger.log("DATABASE", "Tìm thấy user: " + username);
                return user;
            } else {
                Logger.log("DATABASE", "Không tìm thấy user: " + username);
                return null;
            }

        } catch (SQLException e) {
            Logger.log("DATABASE", "Lỗi database khi tìm user: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create default admin user if not exists
     */
    public void createDefaultAdmin() {
        // Check if admin exists
        if (findByUsername("admin") != null) {
            Logger.log("DATABASE", "Admin user đã tồn tại");
            return;
        }

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        Logger.log("DATABASE", "Tạo admin user mặc định");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash password with BCrypt
            String hashedPassword = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
                    .hashToString(12, "admin123".toCharArray());

            stmt.setString(1, "admin");
            stmt.setString(2, hashedPassword);
            stmt.setString(3, "ADMIN");

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                Logger.log("DATABASE", "Tạo admin user thành công");
            }

        } catch (SQLException e) {
            Logger.log("DATABASE", "Lỗi tạo admin user: " + e.getMessage());
        }
    }
}
