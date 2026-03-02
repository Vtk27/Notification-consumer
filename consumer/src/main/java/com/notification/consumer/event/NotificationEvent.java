package com.notification.consumer.event;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka payload sent by the upstream notification service.
 * Keep this in sync with the producer contract (field names and types).
 */
public class NotificationEvent {

    private UUID notificationId;
    private String userEmail;
    private String customerName;
    private String eventType;
    private String referenceId;
    private Map<String, Object> metadata;

    public NotificationEvent() {
    }

    public NotificationEvent(UUID notificationId, String userEmail, String customerName, String eventType, String referenceId,
            Map<String, Object> metadata) {
        this.notificationId = notificationId;
        this.userEmail = userEmail;
        this.customerName = customerName;
        this.eventType = eventType;
        this.referenceId = referenceId;
        this.metadata = metadata;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
