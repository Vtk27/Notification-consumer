package com.notification.consumer.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSentRepository extends JpaRepository<NotificationSent, UUID> {
}
