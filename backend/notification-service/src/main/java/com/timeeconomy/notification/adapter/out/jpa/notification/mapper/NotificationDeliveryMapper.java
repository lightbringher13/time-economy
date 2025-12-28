package com.timeeconomy.notification.adapter.out.jpa.notification.mapper;

import com.timeeconomy.notification.adapter.out.jpa.notification.entity.NotificationDeliveryEntity;
import com.timeeconomy.notification.domain.notification.model.NotificationDelivery;

public final class NotificationDeliveryMapper {
    private NotificationDeliveryMapper() {}

    public static NotificationDeliveryEntity toEntity(NotificationDelivery d) {
        NotificationDeliveryEntity e = new NotificationDeliveryEntity();
        e.setEventId(d.getEventId());
        e.setEventType(d.getEventType());
        e.setChannel(d.getChannel().name());
        e.setTemplate(d.getTemplate());
        e.setRecipient(d.getRecipient());
        e.setStatus(d.getStatus().name());
        e.setProvider(d.getProvider());
        e.setProviderMsgId(d.getProviderMsgId());
        e.setErrorMessage(d.getErrorMessage());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }
}