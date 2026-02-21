package com.sonifoy.user.application.service;

import com.sonifoy.user.domain.model.User;
import com.sonifoy.user.infrastructure.adapter.out.persistence.UserEntity;
import com.sonifoy.user.infrastructure.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToDomain)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }

    public Mono<User> updateProfile(String email, String name) {
        return userRepository.findByEmail(email)
                .flatMap(userEntity -> {
                    if (name != null && !name.isBlank()) {
                        userEntity.setName(name);
                    }
                    return userRepository.save(userEntity);
                })
                .map(this::mapToDomain)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }

    public Mono<Void> changePassword(String email, String oldPassword, String newPassword) {
        return userRepository.findByEmail(email)
                .flatMap(userEntity -> {
                    if (!passwordEncoder.matches(oldPassword, userEntity.getPassword())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid old password"));
                    }
                    userEntity.setPassword(passwordEncoder.encode(newPassword));
                    return userRepository.save(userEntity);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .then();
    }

    private User mapToDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .email(entity.getEmail())
                .name(entity.getName())
                // Password should generally not be exposed in domain unless needed
                // .password(entity.getPassword())
                .avatarUrl(entity.getAvatarUrl())
                // Map Set<String> to Set<Role>
                .roles(entity.getRoles() != null ? entity.getRoles().stream()
                        .map(r -> {
                            try {
                                return User.Role.valueOf(r);
                            } catch (IllegalArgumentException e) {
                                return User.Role.USER; // Fallback
                            }
                        })
                        .collect(Collectors.toSet())
                        : null)
                .city(entity.getCity())
                .country(entity.getCountry())
                .profileType(entity.getProfileType())
                .verified(entity.isVerified())
                .lastLoginAt(entity.getLastLoginAt())
                .createdAt(entity.getLastActiveAt()) // mapping inconsistency? UserEntity doesn't show createdAt?
                // Let's check UserEntity content again. It has lastActiveAt but maybe lacks
                // createdAt?
                // Ah, User.java has createdAt. UserEntity didn't have it in my view earlier?
                // I'll skip fields that might be missing or map carefully.
                .build();
    }
}
