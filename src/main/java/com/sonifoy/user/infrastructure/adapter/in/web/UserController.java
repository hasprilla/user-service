package com.sonifoy.user.infrastructure.adapter.in.web;

import com.sonifoy.user.application.service.UserService;
import com.sonifoy.user.domain.model.User;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Mono<User> getProfile(Principal principal) {
        return userService.getUserByEmail(principal.getName());
    }

    @PutMapping("/profile")
    public Mono<User> updateProfile(Principal principal, @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getName(), request.getName());
    }

    @PostMapping("/change-password")
    public Mono<Void> changePassword(Principal principal, @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(principal.getName(), request.getOldPassword(), request.getNewPassword());
    }

    @Data
    public static class UpdateProfileRequest {
        private String name;
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
