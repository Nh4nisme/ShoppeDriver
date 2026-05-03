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
        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Shipper Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter username and password", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            AuthRepository repo = new AuthRepositoryImpl();
            User user = repo.findByUsername(username);
            if (user == null) {
                JOptionPane.showMessageDialog(null, "Invalid username or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Verify password using BCrypt (same as LoginService)
            boolean verified = at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(password.toCharArray(), user.getPassword()).verified;
            if (!verified) {
                JOptionPane.showMessageDialog(null, "Invalid username or password", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!"SHIPPER".equalsIgnoreCase(user.getRole())) {
                JOptionPane.showMessageDialog(null, "User is not a shipper", "Access Denied", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Logger.log("SHIPPER_LOGIN", "Shipper authenticated: " + username + " (id=" + user.getId() + ")");

            // Launch ShipperApp with shipperId as argument
            ShipperRepository shipperRepo = new ShipperRepositoryImpl();
            Shipper shipper = shipperRepo.findByUserId(user.getId());

            if (shipper == null) {
                JOptionPane.showMessageDialog(null, "Không tìm thấy shipper cho user này", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] launchArgs = new String[]{String.valueOf(shipper.getId())};
            ShipperApp.main(launchArgs);
        }
    }
}
