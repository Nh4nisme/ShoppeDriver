package com.logistics.auth;

import com.logistics.model.User;

/**
 * Session management for authentication
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private boolean authenticated = false;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Login user and create session
     * @param user the authenticated user
     */
    public void login(User user) {
        this.currentUser = user;
        this.authenticated = true;
    }

    /**
     * Logout current user
     */
    public void logout() {
        this.currentUser = null;
        this.authenticated = false;
    }

    /**
     * Check if user is authenticated
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Get current authenticated user
     * @return current user or null
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if current user is admin
     * @return true if admin
     */
    public boolean isAdmin() {
        return authenticated && currentUser != null &&
               "ADMIN".equals(currentUser.getRole());
    }
}
