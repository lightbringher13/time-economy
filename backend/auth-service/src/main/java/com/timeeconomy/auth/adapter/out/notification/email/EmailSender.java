package com.timeeconomy.auth.adapter.out.notification.email;

import com.timeeconomy.auth.domain.common.notification.model.NotificationMessage;

public interface EmailSender {
    void send(NotificationMessage message);
}