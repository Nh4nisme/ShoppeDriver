package com.logistics.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.model.LatLng;
import com.logistics.model.Route;
import com.logistics.util.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RouteService {
    private static final RouteService instance = new RouteService();
    // Use OpenRouteService (ORS) directions endpoint(s)
    private static final String[] ROUTE_BASE_URLS = {
            "https://api.openrouteservice.org/v2/directions/driving-car/geojson"
    };
    // ORS API key: prefer environment variable ORS_API_KEY, fall back to provided key
    private static final String ORS_API_KEY =
            System.getenv().getOrDefault("ORS_API_KEY", "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjgzZTJlNjU0M2JhNzRiMWRiM2EzZTdiMTkyYzJiZjY5IiwiaCI6Im11cm11cjY0In0=");

    private final HttpClient httpClient;
    private final Map<String, Route> routeCache = new ConcurrentHashMap<>();

    private RouteService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static RouteService getInstance() {
        return instance;
    }

    public Route getRoute(LatLng from, LatLng to) throws IOException, InterruptedException {
        List<Route> routes = getAlternativeRoutes(from, to);
        if (routes.isEmpty()) {
            throw new IOException("Route request failed");
        }
        return routes.get(0);
    }

    public List<Route> getAlternativeRoutes(LatLng from, LatLng to) throws IOException, InterruptedException {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Invalid address");
        }

        String cacheKey = buildCacheKey(from, to);
        Route cachedPrimary = routeCache.get(cacheKey);
        if (cachedPrimary != null) {
            return List.of(cachedPrimary);
        }

        IOException lastException = null;
        for (String baseUrl : ROUTE_BASE_URLS) {
            try {
                List<Route> routes = fetchRoutes(baseUrl, from, to);
                if (!routes.isEmpty()) {
                    routeCache.put(cacheKey, routes.getFirst());
                    Logger.log("ROUTE", "Loaded " + routes.size() + " route option(s) from " + baseUrl);
                    return routes;
                }
            } catch (IOException e) {
                lastException = e;
                Logger.error("ROUTE", "Route API failed at " + baseUrl + ": " + e.getMessage());
            }
        }

        throw lastException != null ? lastException : new IOException("Route request failed");
    }

    private String buildCacheKey(LatLng from, LatLng to) {
        return from.latitude() + "_" + from.longitude() + "_" + to.latitude() + "_" + to.longitude();
    }

    private List<Route> fetchRoutes(String baseUrl, LatLng from, LatLng to) throws IOException, InterruptedException {
        // Build ORS POST body with coordinates (lon, lat)
        String body = "{\"coordinates\":[[" + from.longitude() + "," + from.latitude() + "],[" + to.longitude() + "," + to.latitude() + "]],"
                + "\"instructions\":false,\"geometry\":true,\"units\":\"m\","
                + "\"alternative_routes\":{\"target_count\":3,\"share_factor\":0.6}}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "ShoppeDriver/1.0")
                .header("Content-Type", "application/json")
                .header("Authorization", ORS_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Route request failed: " + response.statusCode() + " - " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

        List<Route> routeOptions = new ArrayList<>();

        // ORS returns GeoJSON: features -> each feature has geometry.coordinates and properties.segments
        if (root.has("features")) {
            JsonArray features = root.getAsJsonArray("features");
            for (int i = 0; i < features.size(); i++) {
                JsonObject feature = features.get(i).getAsJsonObject();
                JsonObject geometry = feature.getAsJsonObject("geometry");
                JsonArray coordinates = geometry.getAsJsonArray("coordinates");

                List<LatLng> points = new ArrayList<>();
                for (int j = 0; j < coordinates.size(); j++) {
                    JsonArray coord = coordinates.get(j).getAsJsonArray();
                    points.add(new LatLng(coord.get(1).getAsDouble(), coord.get(0).getAsDouble()));
                }

                double distance = 0.0;
                double duration = 0.0;
                if (feature.has("properties")) {
                    JsonObject props = feature.getAsJsonObject("properties");
                    if (props.has("segments")) {
                        JsonArray segments = props.getAsJsonArray("segments");
                        if (segments != null && segments.size() > 0) {
                            JsonObject seg0 = segments.get(0).getAsJsonObject();
                            if (seg0.has("distance")) distance = seg0.get("distance").getAsDouble();
                            if (seg0.has("duration")) duration = seg0.get("duration").getAsDouble();
                        }
                    } else if (props.has("summary")) {
                        JsonObject summary = props.getAsJsonObject("summary");
                        if (summary.has("distance")) distance = summary.get("distance").getAsDouble();
                        if (summary.has("duration")) duration = summary.get("duration").getAsDouble();
                    }
                }

                routeOptions.add(new Route(from, to, points, distance, duration));
            }
        }

        // Fallback: OSRM-like responses (routes array)
        if (routeOptions.isEmpty() && root.has("routes")) {
            JsonArray routes = root.getAsJsonArray("routes");
            if (routes == null || routes.isEmpty()) {
                throw new IOException("No route returned");
            }

            for (int routeIndex = 0; routeIndex < routes.size(); routeIndex++) {
                JsonObject routeObject = routes.get(routeIndex).getAsJsonObject();

                // geometry may be an object with coordinates or a primitive (encoded polyline) -> prefer coordinates
                List<LatLng> points = new ArrayList<>();
                try {
                    if (routeObject.has("geometry") && routeObject.get("geometry").isJsonObject()) {
                        JsonArray coordinates = routeObject.getAsJsonObject("geometry").getAsJsonArray("coordinates");
                        for (int i = 0; i < coordinates.size(); i++) {
                            JsonArray coordinate = coordinates.get(i).getAsJsonArray();
                            points.add(new LatLng(coordinate.get(1).getAsDouble(), coordinate.get(0).getAsDouble()));
                        }
                    }
                } catch (Exception ex) {
                    // ignore and continue; if geometry is encoded polyline we don't decode here
                }

                double distance = routeObject.has("distance") ? routeObject.get("distance").getAsDouble() : 0.0;
                double duration = routeObject.has("duration") ? routeObject.get("duration").getAsDouble() : 0.0;

                routeOptions.add(new Route(from, to, points, distance, duration));
            }
        }

        if (routeOptions.isEmpty()) {
            throw new IOException("No route returned");
        }

        routeOptions.sort(Comparator
                .comparingDouble(Route::getDurationSeconds)
                .thenComparingDouble(Route::getDistanceMeters));
        if (routeOptions.size() > 3) {
            return new ArrayList<>(routeOptions.subList(0, 3));
        }
        return routeOptions;
    }
}
