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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RouteService {
    private static final RouteService instance = new RouteService();
    private static final String DEFAULT_BASE_URL = "https://router.project-osrm.org";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Map<String, Route> routeCache = new ConcurrentHashMap<>();

    private RouteService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.baseUrl = System.getProperty("route.baseUrl", DEFAULT_BASE_URL);
    }

    public static RouteService getInstance() {
        return instance;
    }

    public Route getRoute(LatLng from, LatLng to) throws IOException, InterruptedException {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Invalid address");
        }

        String cacheKey = from.latitude() + "_" + from.longitude() + "_" + to.latitude() + "_" + to.longitude();
        Route cached = routeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String url = baseUrl + "/route/v1/driving/"
                + from.longitude() + "," + from.latitude() + ";"
                + to.longitude() + "," + to.latitude()
                + "?overview=full&geometries=geojson";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "ShoppeDriver/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Route request failed");
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray routes = root.getAsJsonArray("routes");
        if (routes == null || routes.isEmpty()) {
            throw new IOException("No route returned");
        }

        JsonObject firstRoute = routes.get(0).getAsJsonObject();
        JsonArray coordinates = firstRoute.getAsJsonObject("geometry").getAsJsonArray("coordinates");

        List<LatLng> points = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            JsonArray coordinate = coordinates.get(i).getAsJsonArray();
            points.add(new LatLng(coordinate.get(1).getAsDouble(), coordinate.get(0).getAsDouble()));
        }

        Route route = new Route(
                from,
                to,
                points,
                firstRoute.get("distance").getAsDouble(),
                firstRoute.get("duration").getAsDouble()
        );
        routeCache.put(cacheKey, route);
        Logger.log("ROUTE", "Loaded route with " + points.size() + " points");
        return route;
    }
}
