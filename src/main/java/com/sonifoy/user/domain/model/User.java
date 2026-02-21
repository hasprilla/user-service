package com.sonifoy.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id; // Monolith used String for ID in domain model? Let's check. Yes.
    private String email;
    private String name;
    private String password;
    private String avatarUrl;
    private Set<Role> roles;

    // Audit Data
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
    private Long lastSessionDuration;

    // Partition Key
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;

    public enum Role {
        ADMIN,
        ARTISTA,
        SELLO,
        USER,
        DEVELOPER
    }
}
