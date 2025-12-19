package com.example.aichatbot.dto;

import com.example.aichatbot.enums.Role;
import com.example.aichatbot.enums.UserStatus;
import java.util.Set;

public record UserUpdateDto(
        String username,
        Set<Role> roles,
        UserStatus status) {
}
