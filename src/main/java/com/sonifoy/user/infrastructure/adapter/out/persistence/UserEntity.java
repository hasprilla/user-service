package com.sonifoy.user.infrastructure.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Set;

@Table("users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id
    private Long id;
    private String email;
    private String name;
    private String password;
    private String avatarUrl;
    private Set<String> roles; // Stored as strings in DB

    // Audit and Profile Data
    private String ipAddress;
    private String city;
    private String country;
    private String deviceData;
    private String profileType;
    @Builder.Default
    private boolean verified = false;
    private String verificationCode;
    private java.time.Instant verificationCodeExpiresAt;

    private java.time.Instant lastLoginAt;
    private java.time.Instant lastActiveAt;
    private Long lastSessionDuration; // in seconds
}
