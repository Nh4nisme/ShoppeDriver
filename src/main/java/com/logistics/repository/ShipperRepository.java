package com.logistics.repository;

import com.logistics.model.Shipper;
import com.logistics.model.ShipperStatus;

import java.util.List;

/**
 * Repository interface for shipper operations
 */
public interface ShipperRepository {

    List<Shipper> findAll();

    Shipper findById(int shipperId);

    List<Shipper> findAvailable();

    boolean updateLocation(int shipperId, double x, double y);

    boolean updateStatus(int shipperId, ShipperStatus status);

    void createDefaultShippers();
}