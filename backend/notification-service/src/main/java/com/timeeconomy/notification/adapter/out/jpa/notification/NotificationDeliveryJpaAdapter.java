package com.timeeconomy.notification.adapter.out.jpa.notification;

import com.timeeconomy.notification.adapter.out.jpa.notification.mapper.NotificationDeliveryMapper;
import com.timeeconomy.notification.adapter.out.jpa.notification.repository.NotificationDeliveryJpaRepository;
import com.timeeconomy.notification.domain.notification.model.NotificationDelivery;
import com.timeeconomy.notification.domain.notification.port.out.NotificationDeliveryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryJpaAdapter implements NotificationDeliveryRepositoryPort {

    private final NotificationDeliveryJpaRepository repo;

    @Override
    @Transactional
    public NotificationDelivery save(NotificationDelivery delivery) {
        repo.save(NotificationDeliveryMapper.toEntity(delivery));
        return delivery;
    }
}