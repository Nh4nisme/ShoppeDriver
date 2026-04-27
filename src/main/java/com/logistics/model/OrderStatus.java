package com.logistics.model;

public enum OrderStatus {
    PENDING("Pending"),
    IN_DELIVERY("In Delivery"),
    DONE("Done"),
    FAILED("Failed");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

