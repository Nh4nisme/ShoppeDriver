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
        Logger.log("DATABASE", "Thuc thi query: " + sql);

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
                Logger.log("DATABASE", "Tim thay user: " + username);
                return user;
            } else {
                Logger.log("DATABASE", "Khong tim thay user: " + username);
                return null;
            }

        } catch (SQLException e) {
            Logger.log("DATABASE", "Loi database khi tim user: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create default admin user if not exists
     */
    public void createDefaultAdmin() {
        // Check if admin exists
        if (findByUsername("admin") != null) {
            Logger.log("DATABASE", "Admin user da ton tai");
            return;
        }

        String sql = "INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, ?)";
        Logger.log("DATABASE", "Tao admin user mac dinh");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash password with BCrypt
            String hashedPassword = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
                    .hashToString(12, "admin123".toCharArray());

            stmt.setInt(1, 1); // id
            stmt.setString(2, "admin"); // username
            stmt.setString(3, hashedPassword);
            stmt.setString(4, "ADMIN");

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                Logger.log("DATABASE", "Tao admin user thanh cong");
            }

        } catch (SQLException e) {
            Logger.log("DATABASE", "Loi tao admin user: " + e.getMessage());
        }
    }
}
