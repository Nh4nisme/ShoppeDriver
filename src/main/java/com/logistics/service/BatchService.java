package com.logistics.service;

import com.logistics.model.Batch;
import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.repository.BatchRepository;
import com.logistics.repository.BatchRepositoryImpl;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.OrderRepositoryImpl;
import com.logistics.util.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BatchService {
    private static final double MIN_EPS_METERS = 300.0;
    private static final double MAX_EPS_METERS = 700.0;
    private static final double EPS_STEP_METERS = 100.0;
    private static final int MIN_MIN_SAMPLES = 2;
    private static final int DEFAULT_MIN_SAMPLES = 3;
    private static final int MAX_MIN_SAMPLES = 3;
    private static final int MAX_TUNING_ATTEMPTS = 5;
    private static final int LARGE_CLUSTER_SIZE = 12;
    private static final double LARGE_CLUSTER_RATIO = 0.45;
    private static final double EXCESSIVE_NOISE_RATIO = 0.40;
    private static final Object BATCH_WRITE_LOCK = new Object();

    private final BatchRepository batchRepository;
    private final OrderRepository orderRepository;

    public BatchService() {
        this.batchRepository = new BatchRepositoryImpl();
        this.orderRepository = new OrderRepositoryImpl();
    }

    public Batch createBatch(List<Order> orders) {
        synchronized (BATCH_WRITE_LOCK) {
            if (orders == null || orders.isEmpty()) {
                throw new IllegalArgumentException("No orders selected");
            }

            Map<Integer, Order> uniqueOrders = new LinkedHashMap<>();
            for (Order order : orders) {
                if (order != null) {
                    uniqueOrders.put(order.getId(), order);
                }
            }

            if (uniqueOrders.isEmpty()) {
                throw new IllegalArgumentException("No orders selected");
            }

            List<Order> persistedOrders = new ArrayList<>();
            for (Order candidate : uniqueOrders.values()) {
                Order current = orderRepository.findById(candidate.getId());
                if (current == null) {
                    throw new IllegalStateException("Order not found: " + candidate.getId());
                }
                if (current.getStatus() != OrderStatus.PENDING) {
                    throw new IllegalStateException("Order is not available: " + candidate.getId());
                }
                persistedOrders.add(current);
            }

            List<Integer> orderIds = persistedOrders.stream()
                    .map(Order::getId)
                    .toList();
            Batch saved = batchRepository.createWithPendingOrders(orderIds);
            if (saved == null) {
                throw new IllegalStateException("Could not create batch");
            }

            ShipperTrackingService.getInstance().notifyBatchUpdated(saved);
            return saved;
        }
    }

    public BatchPlanningResult planBatches() {
        synchronized (BATCH_WRITE_LOCK) {
            List<Order> allPendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
            List<Order> pendingOrders = allPendingOrders.stream()
                    .filter(this::hasUsableCoordinates)
                    .toList();

            if (pendingOrders.isEmpty()) {
                Logger.log("BATCH_PLANNING", "No pending orders with usable coordinates");
                return new BatchPlanningResult(List.of(), allPendingOrders.size(), 0, allPendingOrders.size(), 0.0, DEFAULT_MIN_SAMPLES);
            }

            DbscanParameters parameters = tuneDbscanParameters(pendingOrders);
            List<List<Order>> clusters = runDbscan(pendingOrders, parameters.epsMeters(), parameters.minSamples());
            List<Order> clusteredOrders = clusters.stream().flatMap(List::stream).toList();
            int unbatchedOrders = allPendingOrders.size() - clusteredOrders.size();

            List<Batch> createdBatches = new ArrayList<>();
            for (List<Order> cluster : clusters) {
                if (cluster.size() >= parameters.minSamples()) {
                    createdBatches.add(createBatch(cluster));
                }
            }

            Logger.log("BATCH_PLANNING", "Created " + createdBatches.size() + " batch(es) from "
                    + clusteredOrders.size() + " order(s); unbatched=" + unbatchedOrders);

            return new BatchPlanningResult(
                    createdBatches,
                    allPendingOrders.size(),
                    clusteredOrders.size(),
                    unbatchedOrders,
                    parameters.epsMeters(),
                    parameters.minSamples()
            );
        }
    }

    private DbscanParameters tuneDbscanParameters(List<Order> orders) {
        double epsMeters = 500.0;
        int minSamples = DEFAULT_MIN_SAMPLES;
        DbscanStats lastStats = null;

        for (int attempt = 1; attempt <= MAX_TUNING_ATTEMPTS; attempt++) {
            List<List<Order>> clusters = runDbscan(orders, epsMeters, minSamples);
            lastStats = DbscanStats.from(clusters, orders.size());
            logClusterStats(attempt, epsMeters, minSamples, lastStats);

            boolean hasLargeCluster = lastStats.maxClusterSize() > LARGE_CLUSTER_SIZE
                    || lastStats.maxClusterSize() > Math.ceil(orders.size() * LARGE_CLUSTER_RATIO);
            boolean hasExcessiveNoise = lastStats.noiseRatio() > EXCESSIVE_NOISE_RATIO;

            if (hasLargeCluster && epsMeters > MIN_EPS_METERS) {
                epsMeters = Math.max(MIN_EPS_METERS, epsMeters - EPS_STEP_METERS);
                continue;
            }

            if (hasExcessiveNoise && epsMeters < MAX_EPS_METERS) {
                epsMeters = Math.min(MAX_EPS_METERS, epsMeters + EPS_STEP_METERS);
                continue;
            }

            if (hasExcessiveNoise && minSamples > MIN_MIN_SAMPLES) {
                minSamples--;
                continue;
            }

            if (!hasExcessiveNoise && !hasLargeCluster && lastStats.clusterCount() > 0 && minSamples < MAX_MIN_SAMPLES) {
                minSamples++;
                List<List<Order>> stricterClusters = runDbscan(orders, epsMeters, minSamples);
                DbscanStats stricterStats = DbscanStats.from(stricterClusters, orders.size());
                logClusterStats(attempt, epsMeters, minSamples, stricterStats);
                if (stricterStats.noiseRatio() <= EXCESSIVE_NOISE_RATIO) {
                    lastStats = stricterStats;
                } else {
                    minSamples--;
                }
            }
            break;
        }

        if (lastStats != null) {
            Logger.log("BATCH_PLANNING", "Final DBSCAN parameters: eps=" + Math.round(epsMeters)
                    + "m, min_samples=" + minSamples + ", clusters=" + lastStats.clusterCount()
                    + ", noise=" + lastStats.noiseCount());
        }
        return new DbscanParameters(epsMeters, minSamples);
    }

    private List<List<Order>> runDbscan(List<Order> orders, double epsMeters, int minSamples) {
        Map<Integer, DbscanState> states = new LinkedHashMap<>();
        for (Order order : orders) {
            states.put(order.getId(), DbscanState.UNVISITED);
        }

        List<List<Order>> clusters = new ArrayList<>();
        for (Order order : orders) {
            if (states.get(order.getId()) != DbscanState.UNVISITED) {
                continue;
            }

            states.put(order.getId(), DbscanState.VISITED);
            List<Order> neighbors = regionQuery(orders, order, epsMeters);
            if (neighbors.size() < minSamples) {
                states.put(order.getId(), DbscanState.NOISE);
                continue;
            }

            List<Order> cluster = new ArrayList<>();
            expandCluster(orders, states, cluster, order, neighbors, epsMeters, minSamples);
            clusters.add(cluster);
        }
        return clusters;
    }

    private void expandCluster(
            List<Order> orders,
            Map<Integer, DbscanState> states,
            List<Order> cluster,
            Order seed,
            List<Order> seedNeighbors,
            double epsMeters,
            int minSamples
    ) {
        addToCluster(cluster, seed);
        List<Order> neighbors = new ArrayList<>(seedNeighbors);
        Set<Integer> queued = new HashSet<>();
        neighbors.forEach(order -> queued.add(order.getId()));

        for (int index = 0; index < neighbors.size(); index++) {
            Order neighbor = neighbors.get(index);
            DbscanState state = states.get(neighbor.getId());

            if (state == DbscanState.UNVISITED) {
                states.put(neighbor.getId(), DbscanState.VISITED);
                List<Order> expandedNeighbors = regionQuery(orders, neighbor, epsMeters);
                if (expandedNeighbors.size() >= minSamples) {
                    for (Order expanded : expandedNeighbors) {
                        if (queued.add(expanded.getId())) {
                            neighbors.add(expanded);
                        }
                    }
                }
            }

            if (states.get(neighbor.getId()) != DbscanState.CLUSTERED) {
                addToCluster(cluster, neighbor);
                states.put(neighbor.getId(), DbscanState.CLUSTERED);
            }
        }
        states.put(seed.getId(), DbscanState.CLUSTERED);
    }

    private List<Order> regionQuery(List<Order> orders, Order center, double epsMeters) {
        return orders.stream()
                .filter(order -> distanceMeters(center, order) <= epsMeters)
                .toList();
    }

    private double distanceMeters(Order first, Order second) {
        double earthRadiusMeters = 6_371_000.0;
        double dLat = Math.toRadians(second.getLatitude() - first.getLatitude());
        double dLng = Math.toRadians(second.getLongitude() - first.getLongitude());
        double lat1 = Math.toRadians(first.getLatitude());
        double lat2 = Math.toRadians(second.getLatitude());

        double haversine = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return earthRadiusMeters * c;
    }

    private void addToCluster(List<Order> cluster, Order order) {
        boolean exists = cluster.stream().anyMatch(existing -> existing.getId() == order.getId());
        if (!exists) {
            cluster.add(order);
        }
    }

    private boolean hasUsableCoordinates(Order order) {
        return order != null && (order.getLatitude() != 0.0 || order.getLongitude() != 0.0);
    }

    private void logClusterStats(int attempt, double epsMeters, int minSamples, DbscanStats stats) {
        Logger.log("BATCH_PLANNING", "DBSCAN attempt " + attempt
                + ": eps=" + Math.round(epsMeters) + "m, min_samples=" + minSamples
                + ", clusters=" + stats.clusterCount()
                + ", sizes=" + stats.clusterSizes()
                + ", noise=" + stats.noiseCount());
    }

    private enum DbscanState {
        UNVISITED,
        VISITED,
        CLUSTERED,
        NOISE
    }

    private record DbscanParameters(double epsMeters, int minSamples) {
    }

    private record DbscanStats(int clusterCount, List<Integer> clusterSizes, int noiseCount, double noiseRatio, int maxClusterSize) {
        private static DbscanStats from(List<List<Order>> clusters, int orderCount) {
            List<Integer> sizes = clusters.stream().map(List::size).toList();
            int clusteredCount = sizes.stream().mapToInt(Integer::intValue).sum();
            int noiseCount = Math.max(0, orderCount - clusteredCount);
            int maxClusterSize = sizes.stream().mapToInt(Integer::intValue).max().orElse(0);
            double noiseRatio = orderCount == 0 ? 0.0 : (double) noiseCount / orderCount;
            return new DbscanStats(clusters.size(), sizes, noiseCount, noiseRatio, maxClusterSize);
        }
    }

    public record BatchPlanningResult(
            List<Batch> batches,
            int pendingOrders,
            int batchedOrders,
            int unbatchedOrders,
            double epsMeters,
            int minSamples
    ) {
        public int batchCount() {
            return batches.size();
        }
    }
}
