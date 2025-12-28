package com.timeeconomy.notification.domain.notification.port.out;

import com.timeeconomy.notification.domain.notification.model.NotificationDelivery;

public interface NotificationDeliveryRepositoryPort {
    NotificationDelivery save(NotificationDelivery delivery);
}