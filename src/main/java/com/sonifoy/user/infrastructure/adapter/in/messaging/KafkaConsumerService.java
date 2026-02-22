package com.sonifoy.user.infrastructure.adapter.in.messaging;

import com.sonifoy.user.infrastructure.adapter.in.messaging.dto.UserRegisteredEvent;
import com.sonifoy.user.infrastructure.adapter.out.persistence.UserActivityLog;
import com.sonifoy.user.infrastructure.adapter.out.persistence.UserActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserActivityLogRepository activityLogRepository;

    @KafkaListener(topics = "user-events", groupId = "user-service-group")
    public void consumeUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for email: {}", event.getEmail());

        UserActivityLog logEntry = UserActivityLog.builder()
                .id(UUID.randomUUID())
                .userId(event.getUserId())
                .activityType("REGISTRATION")
                .description("User registered with email: " + event.getEmail())
                .timestamp(event.getTimestamp())
                .build();

        activityLogRepository.save(logEntry)
                .doOnSuccess(saved -> log.info("Successfully logged activity to Cassandra for User ID: {}",
                        event.getUserId()))
                .subscribe();
    }
}
