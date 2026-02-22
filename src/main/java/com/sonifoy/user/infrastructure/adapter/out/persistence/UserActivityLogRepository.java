package com.sonifoy.user.infrastructure.adapter.out.persistence;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface UserActivityLogRepository extends ReactiveCassandraRepository<UserActivityLog, UUID> {
    Flux<UserActivityLog> findAllByUserId(String userId);
}
