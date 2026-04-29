package com.logistics.model;

import java.io.Serializable;

public class AddressSuggestion implements Serializable {
    private final String displayText;
    private final double latitude;
    private final double longitude;
    private final String rawAddress;
    private final String street;
    private final String number;
    private final String ward;
    private final String district;
    private final String city;

    public AddressSuggestion(String displayText, double latitude, double longitude, String rawAddress) {
        this(displayText, latitude, longitude, rawAddress, "", "", "", "", "");
    }

    public AddressSuggestion(String displayText, double latitude, double longitude, String rawAddress,
                            String street, String number, String ward, String district, String city) {
        this.displayText = displayText;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rawAddress = rawAddress;
        this.street = street;
        this.number = number;
        this.ward = ward;
        this.district = district;
        this.city = city;
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

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }

    public String getWard() {
        return ward;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }
}
