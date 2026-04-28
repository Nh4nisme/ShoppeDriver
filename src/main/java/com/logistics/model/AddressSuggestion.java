package com.logistics.model;

import java.io.Serializable;

public class AddressSuggestion implements Serializable {
    private final String displayText;
    private final double latitude;
    private final double longitude;
    private final String rawAddress;

    public AddressSuggestion(String displayText, double latitude, double longitude, String rawAddress) {
        this.displayText = displayText;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rawAddress = rawAddress;
    }

    public String getDisplayText() {
        return displayText;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getRawAddress() {
        return rawAddress;
    }
}
