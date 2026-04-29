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
    private static final String[] ROUTE_BASE_URLS = {
            "https://router.project-osrm.org",
            "https://routing.openstreetmap.de/routed-car"
    };

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
        return routes.getFirst();
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
        String url = baseUrl + "/route/v1/driving/"
                + from.longitude() + "," + from.latitude() + ";"
                + to.longitude() + "," + to.latitude()
                + "?overview=full&geometries=geojson&alternatives=true&steps=false";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "ShoppeDriver/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Route request failed: " + response.statusCode());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray routes = root.getAsJsonArray("routes");
        if (routes == null || routes.isEmpty()) {
            throw new IOException("No route returned");
        }

        List<Route> routeOptions = new ArrayList<>();
        for (int routeIndex = 0; routeIndex < routes.size(); routeIndex++) {
            JsonObject routeObject = routes.get(routeIndex).getAsJsonObject();
            JsonArray coordinates = routeObject.getAsJsonObject("geometry").getAsJsonArray("coordinates");

            List<LatLng> points = new ArrayList<>();
            for (int i = 0; i < coordinates.size(); i++) {
                JsonArray coordinate = coordinates.get(i).getAsJsonArray();
                points.add(new LatLng(coordinate.get(1).getAsDouble(), coordinate.get(0).getAsDouble()));
            }

            routeOptions.add(new Route(
                    from,
                    to,
                    points,
                    routeObject.get("distance").getAsDouble(),
                    routeObject.get("duration").getAsDouble()
            ));
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
