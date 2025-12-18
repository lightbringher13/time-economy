package com.timeeconomy.auth_service.adapter.out.notification.sms;

import com.timeeconomy.auth_service.domain.common.notification.model.NotificationMessage;

public interface SmsSender {
    void send(NotificationMessage message);
}