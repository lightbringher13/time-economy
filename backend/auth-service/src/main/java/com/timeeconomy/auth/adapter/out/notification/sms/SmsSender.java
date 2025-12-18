package com.timeeconomy.auth.adapter.out.notification.sms;

import com.timeeconomy.auth.domain.common.notification.model.NotificationMessage;

public interface SmsSender {
    void send(NotificationMessage message);
}