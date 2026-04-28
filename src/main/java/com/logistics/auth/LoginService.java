package com.logistics.auth;

import com.logistics.model.User;
import com.logistics.repository.AuthRepository;
import com.logistics.util.Logger;

/**
 * Service for authentication operations
 */
public class LoginService {
    private final AuthRepository authRepository;

    public LoginService() {
        this.authRepository = new AuthRepository();
    }

    /**
     * Authenticate user with username and password
     * @param username the username
     * @param password the plain text password
     * @return true if authentication successful
     */
    public boolean authenticate(String username, String password) {
        Logger.log("AUTH", "Bắt đầu đăng nhập: " + username);

        try {
            User user = authRepository.findByUsername(username);

            if (user == null) {
                Logger.log("AUTH", "Đăng nhập thất bại: User không tồn tại");
                return false;
            }

            // Verify password with BCrypt
            boolean passwordValid = at.favre.lib.crypto.bcrypt.BCrypt.verifyer()
                    .verify(password.toCharArray(), user.getPassword()).verified;

            if (passwordValid) {
                SessionManager.getInstance().login(user);
                Logger.log("AUTH", "Đăng nhập thành công cho user: " + username);
                return true;
            } else {
                Logger.log("AUTH", "Đăng nhập thất bại: Sai mật khẩu");
                return false;
            }

        } catch (Exception e) {
            Logger.error("AUTH", "Lỗi đăng nhập: " + e.getMessage());
            return false;
        }
    }

    /**
     * Logout current user
     */
    public void logout() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            Logger.log("AUTH", "Đăng xuất user: " + currentUser.getUsername());
        }
        SessionManager.getInstance().logout();
    }

    /**
     * Initialize default admin user
     */
    public void initializeDefaultAdmin() {
        Logger.log("AUTH", "Khởi tạo admin mặc định");
        authRepository.createDefaultAdmin();
    }
}
