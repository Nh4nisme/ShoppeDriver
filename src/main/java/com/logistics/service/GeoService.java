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
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";
    private static final String PHOTON_BASE_URL = "https://photon.komoot.io";

    private final HttpClient httpClient;
    private final Map<String, LatLng> geoCache = new ConcurrentHashMap<>();

    private GeoService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
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

        LatLng point = geocodeWithFallback(normalized);
        geoCache.put(normalized, point);
        Logger.log("GEO", "Geocoded: " + normalized);
        return point;
    }

    private LatLng geocodeWithFallback(String normalized) throws IOException, InterruptedException {
        IOException lastIoException = null;
        IllegalArgumentException lastArgumentException = null;

        try {
            return geocodeWithNominatim(normalized);
        } catch (IOException e) {
            lastIoException = e;
            Logger.error("GEO", "Nominatim geocode failed, fallback to Photon: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            lastArgumentException = e;
            Logger.error("GEO", "Nominatim geocode empty, fallback to Photon: " + e.getMessage());
        }

        try {
            return geocodeWithPhoton(normalized);
        } catch (IOException e) {
            lastIoException = e;
        } catch (IllegalArgumentException e) {
            lastArgumentException = e;
        }

        if (lastIoException != null) {
            throw lastIoException;
        }
        if (lastArgumentException != null) {
            throw lastArgumentException;
        }
        throw new IOException("Geocode request failed");
    }

    private LatLng geocodeWithNominatim(String address) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = NOMINATIM_BASE_URL + "/search?format=jsonv2&limit=1&countrycodes=vn&q=" + encoded;

        HttpResponse<String> response = sendGet(url);
        if (response.statusCode() != 200) {
            throw new IOException("Nominatim geocode request failed: " + response.statusCode());
        }

        JsonArray results = JsonParser.parseString(response.body()).getAsJsonArray();
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Invalid address");
        }

        JsonObject first = results.get(0).getAsJsonObject();
        return new LatLng(first.get("lat").getAsDouble(), first.get("lon").getAsDouble());
    }

    private LatLng geocodeWithPhoton(String address) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = PHOTON_BASE_URL + "/api/?limit=1&lang=vi&q=" + encoded;

        HttpResponse<String> response = sendGet(url);
        if (response.statusCode() != 200) {
            throw new IOException("Photon geocode request failed: " + response.statusCode());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray features = root.getAsJsonArray("features");
        if (features == null || features.isEmpty()) {
            throw new IllegalArgumentException("Invalid address");
        }

        JsonArray coordinates = features.get(0).getAsJsonObject()
                .getAsJsonObject("geometry")
                .getAsJsonArray("coordinates");
        return new LatLng(coordinates.get(1).getAsDouble(), coordinates.get(0).getAsDouble());
    }

    private HttpResponse<String> sendGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "ShoppeDriver/1.0")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public LatLng geocodeStructured(String street, String number, String ward, String district, String city) throws IOException, InterruptedException {
        // Construct full address from components
        StringBuilder addressBuilder = new StringBuilder();
        if (number != null && !number.isBlank()) {
            addressBuilder.append(number).append(" ");
        }
        if (street != null && !street.isBlank()) {
            addressBuilder.append(street).append(", ");
        }
        if (ward != null && !ward.isBlank()) {
            addressBuilder.append(ward).append(", ");
        }
        if (district != null && !district.isBlank()) {
            addressBuilder.append(district).append(", ");
        }
        if (city != null && !city.isBlank()) {
            addressBuilder.append(city);
        }

        String fullAddress = addressBuilder.toString().replaceAll(", $", "");
        return geocode(fullAddress);
    }
}
