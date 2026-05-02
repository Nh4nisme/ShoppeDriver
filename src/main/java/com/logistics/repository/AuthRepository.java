package com.logistics.repository;

import com.logistics.model.User;

/**
 * Repository interface for authentication operations
 */
public interface AuthRepository {

    /**
     * Find user by username
     * @param username the username to search for
     * @return User object or null if not found
     */
    User findByUsername(String username);

    /**
     * Create default admin user if not exists
     */
    void createDefaultAdmin();
}
