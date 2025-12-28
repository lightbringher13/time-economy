package com.timeeconomy.notification.adapter.out.jpa.notification.repository;

import com.timeeconomy.notification.adapter.out.jpa.notification.entity.NotificationDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryJpaRepository extends JpaRepository<NotificationDeliveryEntity, Long> {
}