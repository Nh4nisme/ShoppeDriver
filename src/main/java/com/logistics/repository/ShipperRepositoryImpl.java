package com.logistics.repository;

import com.logistics.model.Shipper;
import com.logistics.model.ShipperStatus;
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
        String[] names = {"Alice", "Bob", "Charlie", "Diana"};

        for (String name : names) {
            if (!shipperExists(name)) {
                createShipper(name);
            }
        }
    }

    private boolean shipperExists(String name) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Long count = em.createQuery("SELECT COUNT(s) FROM Shipper s WHERE s.name = :name", Long.class)
                    .setParameter("name", name)
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            return false;
        } finally {
            em.close();
        }
    }

    private void createShipper(String name) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            int id = (int) (Math.random() * 1000);
            double x = Math.random() * 20 + 40;
            double y = Math.random() * 20 + 40;

            Shipper shipper = new Shipper(id, name, x, y, ShipperStatus.IDLE, true);
            em.persist(shipper);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error creating shipper: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
