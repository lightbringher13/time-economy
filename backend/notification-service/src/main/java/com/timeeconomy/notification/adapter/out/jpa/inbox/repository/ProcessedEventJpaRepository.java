package com.timeeconomy.notification.adapter.out.jpa.inbox.repository;

import com.timeeconomy.notification.adapter.out.jpa.inbox.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, Long> {
}