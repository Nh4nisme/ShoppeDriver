package com.logistics.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.model.AddressSuggestion;
import com.logistics.util.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AddressSuggestService {
    private static final AddressSuggestService instance = new AddressSuggestService();
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org";
    private static final String PHOTON_URL = "https://photon.komoot.io";

    private final HttpClient httpClient;
    private final Map<String, List<AddressSuggestion>> cache = new ConcurrentHashMap<>();

    private AddressSuggestService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static AddressSuggestService getInstance() {
        return instance;
    }

    public List<AddressSuggestion> suggest(String keyword) throws IOException, InterruptedException {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String normalized = keyword.trim().toLowerCase();
        List<AddressSuggestion> cached = cache.get(normalized);
        if (cached != null) {
            return cached;
        }

        List<AddressSuggestion> suggestions = suggestFromNominatim(keyword.trim());
        if (suggestions.isEmpty()) {
            suggestions = suggestFromPhoton(keyword.trim());
        }

        cache.put(normalized, suggestions);
        return suggestions;
    }

    private List<AddressSuggestion> suggestFromNominatim(String keyword) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = NOMINATIM_URL + "/search?format=jsonv2&addressdetails=1&limit=5&countrycodes=vn&q=" + encoded;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "ShoppeDriver/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            Logger.error("GEO", "Nominatim suggest failed: " + response.statusCode());
            return List.of();
        }

        JsonArray results = JsonParser.parseString(response.body()).getAsJsonArray();
        List<AddressSuggestion> suggestions = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JsonObject item = results.get(i).getAsJsonObject();
            String display = item.get("display_name").getAsString();
            JsonObject address = item.has("address") ? item.getAsJsonObject("address") : new JsonObject();
            suggestions.add(new AddressSuggestion(
                    display,
                    item.get("lat").getAsDouble(),
                    item.get("lon").getAsDouble(),
                    display,
                    pickFirst(address, "road", "pedestrian", "residential", "street"),
                    pickFirst(address, "house_number"),
                    pickFirst(address, "suburb", "quarter", "neighbourhood", "hamlet"),
                    pickFirst(address, "city_district", "district", "county", "town"),
                    pickFirst(address, "city", "state", "province")
            ));
        }
        return suggestions;
    }

    private List<AddressSuggestion> suggestFromPhoton(String keyword) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = PHOTON_URL + "/api/?limit=5&lang=vi&q=" + encoded;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "ShoppeDriver/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            Logger.error("GEO", "Photon suggest failed: " + response.statusCode());
            return List.of();
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray features = root.getAsJsonArray("features");
        List<AddressSuggestion> suggestions = new ArrayList<>();
        for (int i = 0; i < features.size(); i++) {
            JsonObject feature = features.get(i).getAsJsonObject();
            JsonObject properties = feature.getAsJsonObject("properties");
            JsonArray coordinates = feature.getAsJsonObject("geometry").getAsJsonArray("coordinates");
            String name = properties.has("name") ? properties.get("name").getAsString() : "";
            String city = properties.has("city") ? properties.get("city").getAsString() : "";
            String country = properties.has("country") ? properties.get("country").getAsString() : "";
            String display = String.join(", ", List.of(name, city, country).stream().filter(value -> value != null && !value.isBlank()).toList());
            suggestions.add(new AddressSuggestion(
                    display,
                    coordinates.get(1).getAsDouble(),
                    coordinates.get(0).getAsDouble(),
                    display,
                    name,
                    "",
                    getString(properties, "district"),
                    firstNonBlank(getString(properties, "county"), getString(properties, "state_district")),
                    city
            ));
        }
        return suggestions;
    }

    private String pickFirst(JsonObject source, String... keys) {
        for (String key : keys) {
            if (source.has(key) && !source.get(key).isJsonNull()) {
                String value = source.get(key).getAsString();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    private String getString(JsonObject source, String key) {
        return source.has(key) && !source.get(key).isJsonNull() ? source.get(key).getAsString() : "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
