package com.logistics.repository;

import com.logistics.model.User;
import com.logistics.util.EntityManagerUtil;
import com.logistics.util.Logger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

/**
 * JPA implementation of AuthRepository
 */
public class AuthRepositoryImpl implements AuthRepository {

    @Override
    public User findByUsername(String username) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Finding user: " + username);
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            Logger.log("DATABASE", "User not found: " + username);
            return null;
        } catch (Exception e) {
            Logger.log("DATABASE", "Error finding user: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public void createDefaultAdmin() {
        // Check if admin exists
        if (findByUsername("admin") != null) {
            Logger.log("DATABASE", "Admin user already exists");
            return;
        }

        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Creating default admin user");

            // Hash password with BCrypt
            String hashedPassword = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
                    .hashToString(12, "admin123".toCharArray());

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(hashedPassword);
            admin.setRole("ADMIN");
            em.persist(admin);
            em.getTransaction().commit();
            Logger.log("DATABASE", "Admin user created successfully");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.log("DATABASE", "Error creating admin user: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public User createShipperUserIfNotExists(String username, String displayName) {
        User existing = findByUsername(username);
        if (existing != null) {
            Logger.log("DATABASE", "Shipper user da ton tai: " + username);
            return existing;
        }

        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            String hashedPassword = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
                    .hashToString(12, "shipper123".toCharArray());

            User user = new User();
            user.setUsername(username);
            user.setPassword(hashedPassword);
            user.setRole("SHIPPER");
            em.persist(user);
            em.getTransaction().commit();

            Logger.log("DATABASE", "Da tao user shipper: " + username);
            return findByUsername(username); // reload để lấy id được generate
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            Logger.error("DATABASE", "Loi tao user shipper: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }
}
