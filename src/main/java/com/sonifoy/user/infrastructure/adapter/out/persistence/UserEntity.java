package com.sonifoy.user.infrastructure.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
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
    @Column("avatar_url")
    private String avatarUrl;
    @Column("roles")
    private Set<String> roles; // Stored as strings in DB

    // Audit and Profile Data
    @Column("ip_address")
    private String ipAddress;
    private String city;
    private String country;
    @Column("device_data")
    private String deviceData;
    @Column("profile_type")
    private String profileType;
    @Builder.Default
    private boolean verified = false;
    @Column("verification_code")
    private String verificationCode;
    @Column("verification_code_expires_at")
    private java.time.Instant verificationCodeExpiresAt;

    @Column("last_login_at")
    private java.time.Instant lastLoginAt;
    @Column("last_active_at")
    private java.time.Instant lastActiveAt;
    @Column("last_session_duration")
    private Long lastSessionDuration; // in seconds

    @Column("created_at")
    private java.time.Instant createdAt;
    @Column("updated_at")
    private java.time.Instant updatedAt;
}
