package com.logistics.service;

import com.logistics.model.Batch;
import com.logistics.model.Order;
import com.logistics.model.Route;
import com.logistics.util.Logger;

import java.io.IOException;
import java.util.List;

public class RouteBuilderService {
    private static final RouteBuilderService instance = new RouteBuilderService();
    private static final double ORDER_SEARCH_RADIUS_KM = 5.0;

    private final GeoService geoService;
    private final RouteService routeService;
    private final OrderService orderService;
    private final BatchService batchService;

    private RouteBuilderService() {
        this.geoService = GeoService.getInstance();
        this.routeService = RouteService.getInstance();
        this.orderService = OrderService.getInstance();
        this.batchService = new BatchService();
    }

    public static RouteBuilderService getInstance() {
        return instance;
    }

    public Batch createBatchFromAddresses(String from, String to) throws IOException, InterruptedException {
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("Invalid address");
        }

        Route route = previewRoute(from, to);
        if (route == null) {
            throw new IOException("Route API failed");
        }

        List<Order> orders = orderService.findOrdersAlongRoute(route, ORDER_SEARCH_RADIUS_KM);
        if (orders.isEmpty()) {
            Batch emptyBatch = new Batch();
            emptyBatch.setOrders(List.of());
            return emptyBatch;
        }

        Batch batch = batchService.createBatch(orders);
        Logger.log("BATCH", "Created batch from addresses: " + from + " -> " + to);
        return batch;
    }

    public Route previewRoute(String from, String to) throws IOException, InterruptedException {
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("Invalid address");
        }
        return routeService.getRoute(geoService.geocode(from), geoService.geocode(to));
    }
}
