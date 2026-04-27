package com.logistics.ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.logistics.model.Order;
import com.logistics.model.Shipper;
import com.logistics.service.ShipperTrackingService;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.application.Platform;

import java.net.URL;
import java.util.Collection;

public class GoogleMapsPanel extends BorderPane {
    private final WebView webView;
    private final WebEngine engine;
    private final Gson gson;

    public GoogleMapsPanel() {
        this.webView = new WebView();
        this.engine = webView.getEngine();
        this.gson = new Gson();

        this.setCenter(webView);

        // Load the map HTML
        loadMapHTML();
    }

    private void loadMapHTML() {
        try {
            URL mapURL = getClass().getResource("/map.html");
            if (mapURL != null) {
                engine.load(mapURL.toExternalForm());
                System.out.println("[GoogleMapsPanel] Map loaded from: " + mapURL);
            } else {
                System.err.println("[GoogleMapsPanel] map.html not found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[GoogleMapsPanel] Error loading map: " + e.getMessage());
        }
    }

    public void updateMap() {
        Platform.runLater(() -> {
            try {
                ShipperTrackingService trackingService = ShipperTrackingService.getInstance();

                // Build JSON data
                JsonObject mapData = new JsonObject();

                // Add shippers
                JsonArray shippersArray = new JsonArray();
                for (Shipper shipper : trackingService.getAllShippers()) {
                    JsonObject shipperObj = new JsonObject();
                    shipperObj.addProperty("id", shipper.getId());
                    shipperObj.addProperty("name", shipper.getName());
                    shipperObj.addProperty("x", shipper.getCurrentX());
                    shipperObj.addProperty("y", shipper.getCurrentY());
                    shipperObj.addProperty("status", shipper.getStatus().getDisplayName());
                    shippersArray.add(shipperObj);
                }
                mapData.add("shippers", shippersArray);

                // Add orders
                JsonArray ordersArray = new JsonArray();
                for (com.logistics.model.Batch batch : trackingService.getAllBatches()) {
                    for (Order order : batch.getOrders()) {
                        JsonObject orderObj = new JsonObject();
                        orderObj.addProperty("id", order.getId());
                        orderObj.addProperty("x", order.getX());
                        orderObj.addProperty("y", order.getY());
                        orderObj.addProperty("status", order.getStatus().getDisplayName());
                        ordersArray.add(orderObj);
                    }
                }
                mapData.add("orders", ordersArray);

                // Add bounds
                JsonObject bounds = new JsonObject();
                bounds.addProperty("minX", 0);
                bounds.addProperty("minY", 0);
                bounds.addProperty("maxX", 100);
                bounds.addProperty("maxY", 100);
                mapData.add("bounds", bounds);

                // Execute update
                String mapDataJson = gson.toJson(mapData);
                engine.executeScript("updateMap('" + mapDataJson.replace("'", "\\'") + "');");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

