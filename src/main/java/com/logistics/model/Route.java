package com.logistics.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Route implements Serializable {
    private final LatLng from;
    private final LatLng to;
    private final List<LatLng> polyline;
    private final double distanceMeters;
    private final double durationSeconds;

    public Route(LatLng from, LatLng to, List<LatLng> polyline, double distanceMeters, double durationSeconds) {
        this.from = from;
        this.to = to;
        this.polyline = Collections.unmodifiableList(new ArrayList<>(polyline));
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }

    public LatLng getFrom() {
        return from;
    }

    public LatLng getTo() {
        return to;
    }

    public List<LatLng> getPolyline() {
        return polyline;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public double getMinLatitude() {
        return polyline.stream().mapToDouble(LatLng::latitude).min().orElse(Math.min(from.latitude(), to.latitude()));
    }

    public double getMaxLatitude() {
        return polyline.stream().mapToDouble(LatLng::latitude).max().orElse(Math.max(from.latitude(), to.latitude()));
    }

    public double getMinLongitude() {
        return polyline.stream().mapToDouble(LatLng::longitude).min().orElse(Math.min(from.longitude(), to.longitude()));
    }

    public double getMaxLongitude() {
        return polyline.stream().mapToDouble(LatLng::longitude).max().orElse(Math.max(from.longitude(), to.longitude()));
    }
}
