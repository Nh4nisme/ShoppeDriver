package com.logistics.model;

public enum BatchStatus {
    PENDING("Pending"),
    ASSIGNED("Assigned"),
    IN_DELIVERY("In Delivery"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CREATED("Created"),;

    private final String displayName;

    BatchStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

