package com.logistics.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private int shipperId;
    private int adminId;
    private String senderName;
    private String messageContent;
    private LocalDateTime timestamp;
    private boolean isFromAdmin;

    public ChatMessage(int shipperId, String senderName, String messageContent, LocalDateTime timestamp, boolean isFromAdmin) {
        this(shipperId, 0, senderName, messageContent, timestamp, isFromAdmin);
    }

    public ChatMessage(int shipperId, int adminId, String senderName, String messageContent, LocalDateTime timestamp, boolean isFromAdmin) {
        this.shipperId = shipperId;
        this.adminId = adminId;
        this.senderName = senderName;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
        this.isFromAdmin = isFromAdmin;
    }

    public int getShipperId() {
        return shipperId;
    }

    public void setShipperId(int shipperId) {
        this.shipperId = shipperId;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFromAdmin() {
        return isFromAdmin;
    }

    public void setFromAdmin(boolean fromAdmin) {
        isFromAdmin = fromAdmin;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + senderName + ": " + messageContent;
    }
}

