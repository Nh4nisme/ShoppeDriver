package com.logistics.repository;

import com.logistics.model.Shipper;
import com.logistics.model.ShipperStatus;
import com.logistics.model.User;
import com.logistics.util.EntityManagerUtil;
import com.logistics.util.Logger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;

/**
 * JPA implementation of ShipperRepository
 */
public class ShipperRepositoryImpl implements ShipperRepository {

    @Override
    public List<Shipper> findAll() {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Finding all shippers");
            List<Shipper> shippers = em.createQuery("SELECT s FROM Shipper s ORDER BY s.id", Shipper.class).getResultList();
            Logger.log("SHIPPER", "Retrieved " + shippers.size() + " shippers from database");
            return shippers;
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding shippers: " + e.getMessage());
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public Shipper findById(int shipperId) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Finding shipper by id: " + shipperId);
            return em.find(Shipper.class, shipperId);
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding shipper: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Shipper> findAvailable() {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Finding available shippers");
            return em.createQuery("SELECT s FROM Shipper s WHERE s.status != :busy ORDER BY s.id", Shipper.class)
                    .setParameter("busy", ShipperStatus.BUSY)
                    .getResultList();
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding available shippers: " + e.getMessage());
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public boolean updateLocation(int shipperId, double x, double y) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Updating shipper location: " + shipperId);
            Query query = em.createQuery("UPDATE Shipper s SET s.currentX = :x, s.currentY = :y WHERE s.id = :id");
            query.setParameter("x", x);
            query.setParameter("y", y);
            query.setParameter("id", shipperId);
            int rows = query.executeUpdate();
            em.getTransaction().commit();
            return rows > 0;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error updating shipper location: " + e.getMessage());
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean updateStatus(int shipperId, ShipperStatus status) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Updating shipper status: " + shipperId);
            Query query = em.createQuery("UPDATE Shipper s SET s.status = :status WHERE s.id = :id");
            query.setParameter("status", status);
            query.setParameter("id", shipperId);
            int rows = query.executeUpdate();
            em.getTransaction().commit();
            return rows > 0;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error updating shipper status: " + e.getMessage());
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public void createDefaultShippers() {
        // { username, displayName, x, y }
        Object[][] defaults = {
                {"shipper_nam",  "Nguyen Van Nam",  10.7769, 106.7009},
                {"shipper_hung", "Tran Van Hung",   10.7850, 106.6950},
                {"shipper_linh", "Le Thi Linh",     10.7720, 106.7100},
                {"shipper_minh", "Pham Van Minh",   10.7680, 106.6880},
                {"shipper_tuan", "Nguyen Van Tuan", 10.7900, 106.7050},
        };

        AuthRepositoryImpl authRepo = new AuthRepositoryImpl();

        for (Object[] entry : defaults) {
            String username    = (String) entry[0];
            String displayName = (String) entry[1];
            double x           = (double) entry[2];
            double y           = (double) entry[3];

            // Bước 1: tạo user nếu chưa có
            User user = authRepo.createShipperUserIfNotExists(username, displayName);
            if (user == null) {
                Logger.error("DATABASE", "Khong tao duoc user cho: " + username);
                continue;
            }

            // Bước 2: tạo shipper nếu chưa có, gắn user_id
            if (!shipperExistsByUserId(user.getId())) {
                createShipper(displayName, x, y, user);
            } else {
                Logger.log("DATABASE", "Shipper da ton tai cho user: " + username);
            }
        }
    }

    @Override
    public Shipper findByUserId(int userId) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT s FROM Shipper s WHERE s.user.id = :userId", Shipper.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }

    private boolean shipperExistsByUserId(int userId) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(s) FROM Shipper s WHERE s.user.id = :userId", Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            return false;
        } finally {
            em.close();
        }
    }

    private void createShipper(String name, double x, double y, User user) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            User managedUser = em.find(User.class, user.getId());

            Shipper shipper = new Shipper();

            shipper.setName(name);
            shipper.setCurrentX(x);
            shipper.setCurrentY(y);
            shipper.setStatus(ShipperStatus.IDLE);
            shipper.setActive(true);
            shipper.setUser(managedUser);


            em.persist(shipper);
            em.getTransaction().commit();
            Logger.log("DATABASE", "Da tao shipper: " + name + " (user=" + user.getUsername() + ")");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            Logger.error("DATABASE", "Error creating shipper " + name + ": " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
