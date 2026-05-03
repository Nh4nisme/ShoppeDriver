package com.logistics.shipper;

import com.logistics.db.DatabaseInitializer;
import com.logistics.model.Shipper;
import com.logistics.model.User;
import com.logistics.repository.AuthRepository;
import com.logistics.repository.AuthRepositoryImpl;
import com.logistics.repository.ShipperRepository;
import com.logistics.repository.ShipperRepositoryImpl;
import com.logistics.util.Logger;
import javax.swing.*;
import java.awt.*;

/**
 * Simple Swing-based login launcher for ShipperApp.
 * Prompts for username/password, authenticates against users table and launches ShipperApp with shipperId.
 */
public class ShipperLoginLauncher {
    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> showLoginDialog());
    }

    private static void showLoginDialog() {
        JFrame frame = new JFrame("ShoppeDriver - Shipper Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 380);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.getContentPane().setBackground(Color.decode("#f0f2f5"));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(10, 40, 30, 40);

        JLabel titleLabel = new JLabel("Đăng Nhập Shipper", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.decode("#d35400"));
        mainPanel.add(titleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 40, 5, 40);
        JLabel userLabel = new JLabel("Tên đăng nhập:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        mainPanel.add(userLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 40, 15, 40);
        JTextField usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(300, 38));
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        mainPanel.add(usernameField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 40, 5, 40);
        JLabel passLabel = new JLabel("Mật khẩu:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        mainPanel.add(passLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 40, 25, 40);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(300, 38));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        mainPanel.add(passwordField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 40, 20, 40);
        JButton loginButton = new JButton("ĐĂNG NHẬP");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setBackground(Color.decode("#d35400"));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setPreferredSize(new Dimension(300, 42));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mainPanel.add(loginButton, gbc);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Vui lòng nhập tên đăng nhập và mật khẩu", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            AuthRepository repo = new AuthRepositoryImpl();
            User user = repo.findByUsername(username);
            if (user == null) {
                JOptionPane.showMessageDialog(frame, "Sai tên đăng nhập hoặc mật khẩu", "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean verified = at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(password.toCharArray(), user.getPassword()).verified;
            if (!verified) {
                JOptionPane.showMessageDialog(frame, "Sai tên đăng nhập hoặc mật khẩu", "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!"SHIPPER".equalsIgnoreCase(user.getRole())) {
                JOptionPane.showMessageDialog(frame, "Tài khoản không có quyền Shipper", "Từ chối truy cập", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Logger.log("SHIPPER_LOGIN", "Shipper authenticated: " + username + " (id=" + user.getId() + ")");

            ShipperRepository shipperRepo = new ShipperRepositoryImpl();
            Shipper shipper = shipperRepo.findByUserId(user.getId());

            if (shipper == null) {
                JOptionPane.showMessageDialog(frame, "Không tìm thấy hồ sơ shipper cho user này", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            frame.dispose();
            String[] launchArgs = new String[]{String.valueOf(shipper.getId())};
            new Thread(() -> ShipperApp.main(launchArgs)).start();
        });

        frame.add(mainPanel);
        frame.setVisible(true);
    }
}
