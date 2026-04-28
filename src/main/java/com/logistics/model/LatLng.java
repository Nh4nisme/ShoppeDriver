package com.logistics.model;

import java.io.Serializable;

public record LatLng(double latitude, double longitude) implements Serializable {
}
