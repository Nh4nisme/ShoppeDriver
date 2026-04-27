package com.logistics.model;

public enum ShipperStatus {
    IDLE("Idle"),
    IN_DELIVERY("In Delivery"),
    ON_BREAK("On Break"),
    OFFLINE("Offline");

    private final String displayName;

    ShipperStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

