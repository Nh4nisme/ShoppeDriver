package com.logistics.model;

public enum OrderStatus {
    PENDING("Pending"),
    IN_BATCH("In Batch"),
    DELIVERING("Delivering"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}