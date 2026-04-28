package com.logistics.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.model.LatLng;
import com.logistics.util.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeoService {
    private static final GeoService instance = new GeoService();
    private static final String DEFAULT_BASE_URL = "https://nominatim.openstreetmap.org";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Map<String, LatLng> geoCache = new ConcurrentHashMap<>();

    private GeoService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.baseUrl = System.getProperty("geo.baseUrl", DEFAULT_BASE_URL);
    }

    public static GeoService getInstance() {
        return instance;
    }

    public LatLng geocode(String address) throws IOException, InterruptedException {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Invalid address");
        }

        String normalized = address.trim();
        LatLng cached = geoCache.get(normalized);
        if (cached != null) {
            return cached;
        }

        String encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8);
        String url = baseUrl + "/search?format=jsonv2&limit=1&q=" + encoded;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "ShoppeDriver/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Geocode request failed");
        }

        JsonArray results = JsonParser.parseString(response.body()).getAsJsonArray();
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Invalid address");
        }

        JsonObject first = results.get(0).getAsJsonObject();
        LatLng point = new LatLng(first.get("lat").getAsDouble(), first.get("lon").getAsDouble());
        geoCache.put(normalized, point);
        Logger.log("GEO", "Geocoded: " + normalized);
        return point;
    }
}
