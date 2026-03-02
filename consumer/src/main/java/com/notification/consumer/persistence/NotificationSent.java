package com.notification.consumer.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_sent")
public class NotificationSent {

    @Id
    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    protected NotificationSent() {
    }

    public NotificationSent(UUID notificationId, String eventType, String customerName) {
        this.notificationId = notificationId;
        this.eventType = eventType;
        this.customerName = customerName;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
