package com.sonifoy.user.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_activity_logs")
public class UserActivityLog {
    @PrimaryKey
    private UUID id;
    private String userId;
    private String activityType; // LOGIN, LOGOUT, PROFILE_VIEW, STREAM_START
    private String description;
    private Instant timestamp;
    private String deviceData;
    private String ipAddress;
}
