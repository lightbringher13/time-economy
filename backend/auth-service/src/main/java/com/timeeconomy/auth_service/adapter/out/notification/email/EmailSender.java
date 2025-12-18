package com.timeeconomy.auth_service.adapter.out.notification.email;

import com.timeeconomy.auth_service.domain.common.notification.model.NotificationMessage;

public interface EmailSender {
    void send(NotificationMessage message);
}