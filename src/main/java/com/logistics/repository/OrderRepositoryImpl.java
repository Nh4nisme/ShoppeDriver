package com.logistics.repository;

import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.util.EntityManagerUtil;
import com.logistics.util.Logger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.Date;
import java.util.List;

/**
 * JPA implementation of OrderRepository
 */
public class OrderRepositoryImpl implements OrderRepository {

    @Override
    public Order save(Order order) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Saving order");
            if (order.getId() == 0) {
                order.setCreatedAt(new Date());
                em.persist(order);
            } else {
                order = em.merge(order);
            }
            em.getTransaction().commit();
            return order;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error saving order: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public Order findById(int orderId) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            return em.find(Order.class, orderId);
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding order by id: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Finding orders by status: " + status);
            return em.createQuery("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.id", Order.class)
                    .setParameter("status", status)
                    .getResultList();
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding orders by status: " + e.getMessage());
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public boolean updateStatus(int orderId, OrderStatus status) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Updating order status: " + orderId);
            Query query = em.createQuery("UPDATE Order o SET o.status = :status WHERE o.id = :id");
            query.setParameter("status", status);
            query.setParameter("id", orderId);
            int rows = query.executeUpdate();
            em.getTransaction().commit();
            return rows > 0;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error updating order status: " + e.getMessage());
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean assignToBatch(int orderId, int batchId) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Assigning order to batch: " + orderId);
            Query query = em.createQuery("UPDATE Order o SET o.batch.id = :batchId, o.status = :status WHERE o.id = :id");
            query.setParameter("batchId", batchId);
            query.setParameter("status", OrderStatus.DELIVERING);
            query.setParameter("id", orderId);
            int rows = query.executeUpdate();
            em.getTransaction().commit();
            return rows > 0;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error assigning order to batch: " + e.getMessage());
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Order> findOrdersInBoundingBox(double startX, double startY, double endX, double endY, int maxCount) {
        double minX = Math.min(startX, endX);
        double maxX = Math.max(startX, endX);
        double minY = Math.min(startY, endY);
        double maxY = Math.max(startY, endY);

        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Bounding box query: [" + minX + "," + maxX + "] x [" + minY + "," + maxY + "]");
            return em.createQuery("SELECT o FROM Order o WHERE o.status = :status AND o.x >= :minX AND o.x <= :maxX AND o.y >= :minY AND o.y <= :maxY ORDER BY o.id", Order.class)
                    .setParameter("status", OrderStatus.PENDING)
                    .setParameter("minX", minX)
                    .setParameter("maxX", maxX)
                    .setParameter("minY", minY)
                    .setParameter("maxY", maxY)
                    .setMaxResults(maxCount)
                    .getResultList();
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding orders in bounding box: " + e.getMessage());
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Order> findByDistrictAndBoundingBox(String district, double minX, double maxX, double minY, double maxY, int maxCount) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "District bounding box query: " + district);
            String pattern = "%" + (district == null ? "" : district.trim().toLowerCase()) + "%";
            return em.createQuery("SELECT o FROM Order o WHERE o.status = :status AND o.x >= :minX AND o.x <= :maxX AND o.y >= :minY AND o.y <= :maxY AND LOWER(o.address) LIKE :pattern ORDER BY o.id", Order.class)
                    .setParameter("status", OrderStatus.PENDING)
                    .setParameter("minX", minX)
                    .setParameter("maxX", maxX)
                    .setParameter("minY", minY)
                    .setParameter("maxY", maxY)
                    .setParameter("pattern", pattern)
                    .setMaxResults(maxCount)
                    .getResultList();
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding orders by district: " + e.getMessage());
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public double calculateDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
