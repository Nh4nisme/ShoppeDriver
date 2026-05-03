package com.logistics.repository;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.util.EntityManagerUtil;
import com.logistics.util.Logger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA implementation of BatchRepository
 */
public class BatchRepositoryImpl implements BatchRepository {

    @Override
    public Batch save(Batch batch) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Saving batch");
            if (batch.getId() == 0) {
                em.persist(batch);
            } else {
                batch = em.merge(batch);
            }
            em.getTransaction().commit();
            Logger.log("BATCH", "Saved batch: ID=" + batch.getId());
            return batch;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error saving batch: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public Batch createWithPendingOrders(List<Integer> orderIds) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Creating batch with " + orderIds.size() + " pending order(s)");

            List<Order> orders = new ArrayList<>();
            for (Integer orderId : orderIds) {
                Order order = em.find(Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
                if (order == null) {
                    throw new IllegalStateException("Order not found: " + orderId);
                }
                if (order.getStatus() != OrderStatus.PENDING) {
                    throw new IllegalStateException("Order is not available: " + orderId);
                }
                orders.add(order);
            }

            Batch batch = new Batch();
            batch.setStatus(BatchStatus.CREATED);
            batch.setOrders(new ArrayList<>());
            em.persist(batch);

            for (Order order : orders) {
                order.setBatch(batch);
                order.setStatus(OrderStatus.DELIVERING);
                batch.getOrders().add(order);
            }

            em.getTransaction().commit();
            Logger.log("BATCH", "Created batch transactionally: ID=" + batch.getId()
                    + ", orders=" + orders.size());
            return batch;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error creating batch with orders: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Batch> findByStatus(BatchStatus status) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Finding batches by status: " + status);
            return em.createQuery("SELECT b FROM Batch b LEFT JOIN FETCH b.orders WHERE b.status = :status ORDER BY b.id", Batch.class)
                    .setParameter("status", status)
                    .getResultList();
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding batches by status: " + e.getMessage());
            return List.of();
        } finally {
            em.close();
        }
    }

    @Override
    public Batch findById(int batchId) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Finding batch by id: " + batchId);
            return em.createQuery("SELECT b FROM Batch b LEFT JOIN FETCH b.orders WHERE b.id = :id", Batch.class)
                    .setParameter("id", batchId)
                    .getSingleResult();
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding batch by id: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean updateStatus(int batchId, BatchStatus status) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Updating batch status: " + batchId);
            Query query = em.createQuery("UPDATE Batch b SET b.status = :status WHERE b.id = :id");
            query.setParameter("status", status);
            query.setParameter("id", batchId);
            int rows = query.executeUpdate();
            em.getTransaction().commit();
            return rows > 0;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error updating batch status: " + e.getMessage());
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean assignToShipper(int batchId, int shipperId, BatchStatus status) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Logger.log("DATABASE", "Assigning batch to shipper: " + batchId);
            Query query = em.createQuery("UPDATE Batch b SET b.shipperId = :shipperId, b.status = :status WHERE b.id = :id");
            query.setParameter("shipperId", shipperId);
            query.setParameter("status", status);
            query.setParameter("id", batchId);
            int rows = query.executeUpdate();
            em.getTransaction().commit();
            return rows > 0;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            Logger.error("DATABASE", "Error assigning batch to shipper: " + e.getMessage());
            return false;
        } finally {
            em.close();
        }
    }

    @Override
    public Batch findByShipperAndStatus(int shipperId, BatchStatus status) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Finding batch by shipper and status: " + shipperId);
            return em.createQuery("SELECT b FROM Batch b LEFT JOIN FETCH b.orders WHERE b.shipperId = :shipperId AND b.status = :status", Batch.class)
                    .setParameter("shipperId", shipperId)
                    .setParameter("status", status)
                    .getSingleResult();
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding batch by shipper and status: " + e.getMessage());
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Batch> findActiveByShipper(int shipperId) {
        EntityManager em = EntityManagerUtil.getEntityManager();
        try {
            Logger.log("DATABASE", "Finding active batches by shipper: " + shipperId);
            return em.createQuery("SELECT b FROM Batch b LEFT JOIN FETCH b.orders WHERE b.shipperId = :shipperId AND b.status IN (:statuses) ORDER BY b.id DESC", Batch.class)
                    .setParameter("shipperId", shipperId)
                    .setParameter("statuses", List.of(BatchStatus.ASSIGNED, BatchStatus.IN_DELIVERY, BatchStatus.COMPLETED))
                    .getResultList();
        } catch (Exception e) {
            Logger.error("DATABASE", "Error finding active batches by shipper: " + e.getMessage());
            return List.of();
        } finally {
            em.close();
        }
    }
}
