package com.logistics.ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.logistics.model.LatLng;
import com.logistics.model.Order;
import com.logistics.model.Route;
import com.logistics.model.Shipper;
import com.logistics.service.ShipperTrackingService;
import com.logistics.util.Logger;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GoogleMapsPanel extends BorderPane {
    private static volatile GoogleMapsPanel instance;

    private final WebView webView;
    private final WebEngine engine;
    private final Gson gson;

    private volatile boolean mapReady;
    private volatile String pendingMapDataJson;
    private volatile List<Route> previewRoutes = List.of();
    private volatile int selectedPreviewRouteIndex = 0;
    private volatile List<Order> previewOrders = List.of();

    public GoogleMapsPanel() {
        instance = this;
        this.webView = new WebView();
        this.engine = webView.getEngine();
        this.gson = new Gson();

        this.setCenter(webView);
        loadMapHtml();
    }

    private void loadMapHtml() {
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapReady = true;
                Logger.log("MAP", "Map HTML loaded");
                flushPendingState();
            }
        });

        engine.setOnError(event -> Logger.error("MAP", "WebEngine error: " + event.getMessage()));

        try {
            URL mapUrl = getClass().getResource("/map.html");
            if (mapUrl == null) {
                throw new IllegalStateException("map.html not found");
            }
            engine.load(mapUrl.toExternalForm());
        } catch (Exception e) {
            Logger.error("MAP", "Error loading map: " + e.getMessage());
        }
    }

    public void updateMap() {
        Platform.runLater(() -> {
            try {
                JsonObject mapData = buildMapData();
                String json = gson.toJson(mapData);
                if (!mapReady) {
                    pendingMapDataJson = json;
                    return;
                }
                engine.executeScript("window.updateMap(" + json + ");");
            } catch (Exception e) {
                Logger.error("MAP", "Map update failed: " + e.getMessage());
            }
        });
    }

    private JsonObject buildMapData() {
        ShipperTrackingService trackingService = ShipperTrackingService.getInstance();
        JsonObject mapData = new JsonObject();

        JsonArray shippersArray = new JsonArray();
        for (Shipper shipper : trackingService.getAllShippers()) {
            if (!isRenderableVietnamCoordinate(shipper.getCurrentX(), shipper.getCurrentY())) {
                continue;
            }
            JsonObject shipperObject = new JsonObject();
            shipperObject.addProperty("id", shipper.getId());
            shipperObject.addProperty("name", shipper.getName());
            shipperObject.addProperty("lat", shipper.getCurrentX());
            shipperObject.addProperty("lng", shipper.getCurrentY());
            shipperObject.addProperty("status", shipper.getStatus().getDisplayName());
            shippersArray.add(shipperObject);
        }
        mapData.add("shippers", shippersArray);

        JsonArray persistedOrdersArray = new JsonArray();
        for (com.logistics.model.Batch batch : trackingService.getAllBatches()) {
            for (Order order : batch.getOrders()) {
                if (!isRenderableVietnamCoordinate(order.getLatitude(), order.getLongitude())) {
                    continue;
                }
                persistedOrdersArray.add(toOrderJson(order, false));
            }
        }
        mapData.add("orders", persistedOrdersArray);

        JsonArray previewOrdersArray = new JsonArray();
        for (Order order : previewOrders) {
            if (!isRenderableVietnamCoordinate(order.getLatitude(), order.getLongitude())) {
                continue;
            }
            previewOrdersArray.add(toOrderJson(order, true));
        }
        mapData.add("previewOrders", previewOrdersArray);

        JsonArray routeOptionsArray = new JsonArray();
        for (int i = 0; i < previewRoutes.size(); i++) {
            routeOptionsArray.add(toRouteJson(previewRoutes.get(i), i));
        }
        mapData.add("previewRoutes", routeOptionsArray);
        mapData.addProperty("selectedRouteIndex", selectedPreviewRouteIndex);

        return mapData;
    }

    private JsonObject toOrderJson(Order order, boolean preview) {
        JsonObject orderObject = new JsonObject();
        orderObject.addProperty("id", order.getId());
        orderObject.addProperty("lat", order.getLatitude());
        orderObject.addProperty("lng", order.getLongitude());
        orderObject.addProperty("status", order.getStatus().getDisplayName());
        orderObject.addProperty("address", order.getAddress() == null ? "" : order.getAddress());
        orderObject.addProperty("preview", preview);
        return orderObject;
    }

    private JsonObject toRouteJson(Route route, int index) {
        JsonObject routeObject = new JsonObject();
        routeObject.addProperty("index", index);
        routeObject.addProperty("distanceMeters", route.getDistanceMeters());
        routeObject.addProperty("durationSeconds", route.getDurationSeconds());

        JsonArray polylineArray = new JsonArray();
        for (LatLng point : route.getPolyline()) {
            JsonObject pointObject = new JsonObject();
            pointObject.addProperty("lat", point.latitude());
            pointObject.addProperty("lng", point.longitude());
            polylineArray.add(pointObject);
        }
        routeObject.add("polyline", polylineArray);
        JsonArray waypointsArray = new JsonArray();
        if (route.getWaypoints() != null) {
            for (LatLng wp : route.getWaypoints()) {
                JsonObject pointObject = new JsonObject();
                pointObject.addProperty("lat", wp.latitude());
                pointObject.addProperty("lng", wp.longitude());
                waypointsArray.add(pointObject);
            }
        }
        routeObject.add("waypoints", waypointsArray);

        routeObject.addProperty("fromLat", route.getFrom().latitude());
        routeObject.addProperty("fromLng", route.getFrom().longitude());
        routeObject.addProperty("toLat", route.getTo().latitude());
        routeObject.addProperty("toLng", route.getTo().longitude());
        return routeObject;
    }

    private boolean isRenderableVietnamCoordinate(double latitude, double longitude) {
        return latitude >= 8.0 && latitude <= 24.0 && longitude >= 102.0 && longitude <= 110.0;
    }

    private void flushPendingState() {
        String pending = pendingMapDataJson;
        if (pending != null) {
            Platform.runLater(() -> {
                try {
                    engine.executeScript("window.updateMap(" + pending + ");");
                    pendingMapDataJson = null;
                } catch (Exception e) {
                    Logger.error("MAP", "Failed to flush pending map state: " + e.getMessage());
                }
            });
        }
    }

    public static void showRoutePreview(List<Route> routes, int selectedIndex) {
        GoogleMapsPanel panel = instance;
        if (panel == null) {
            return;
        }
        panel.previewRoutes = routes == null ? List.of() : new ArrayList<>(routes);
        panel.selectedPreviewRouteIndex = Math.max(0, Math.min(selectedIndex, panel.previewRoutes.isEmpty() ? 0 : panel.previewRoutes.size() - 1));
        panel.updateMap();
        Logger.log("MAP", "Updated route preview with " + panel.previewRoutes.size() + " option(s)");
    }

    public static void showPreviewOrders(List<Order> orders) {
        GoogleMapsPanel panel = instance;
        if (panel == null) {
            return;
        }
        panel.previewOrders = orders == null ? List.of() : new ArrayList<>(orders);
        panel.updateMap();
        Logger.log("MAP", "Updated preview orders on map: " + panel.previewOrders.size());
    }

    public static void clearRoutePreview() {
        GoogleMapsPanel panel = instance;
        if (panel == null) {
            return;
        }
        panel.previewRoutes = List.of();
        panel.selectedPreviewRouteIndex = 0;
        panel.previewOrders = List.of();
        panel.updateMap();
        Logger.log("MAP", "Cleared route and preview orders from map");
    }
}
