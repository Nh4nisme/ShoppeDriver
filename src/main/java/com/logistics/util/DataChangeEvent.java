package com.logistics.util;

public class DataChangeEvent {
    public static final String DATA_CHANGED = "DATA_CHANGED";
    public static final String SHIPPER_LOCATION_UPDATED = "SHIPPER_LOCATION_UPDATED";
    public static final String BATCH_UPDATED = "BATCH_UPDATED";

    private final String type;
    private final int sourceId;
    private final Object data;

    public DataChangeEvent(String type, int sourceId, Object data) {
        this.type = type;
        this.sourceId = sourceId;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public int getSourceId() {
        return sourceId;
    }

    public Object getData() {
        return data;
    }

    public boolean isType(String eventType) {
        return eventType != null && eventType.equals(type);
    }

    public static DataChangeEvent generic() {
        return new DataChangeEvent(DATA_CHANGED, 0, null);
    }
}
