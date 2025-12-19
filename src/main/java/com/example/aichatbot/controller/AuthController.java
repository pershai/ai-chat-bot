package com.example.aichatbot.controller;

import com.example.aichatbot.dto.AuthResponseDto;
import com.example.aichatbot.dto.LoginRequestDto;
import com.example.aichatbot.exception.UserNotFoundException;
import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.aichatbot.enums.UserStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody LoginRequestDto request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        User user = new User();
        user.setUsername(request.username());
        user.setHashedPassword(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        String token = jwtTokenProvider.generateToken(authentication, user.getId(), tenantId, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        return ResponseEntity.ok(new AuthResponseDto(token, refreshToken, user.getId(), user.getUsername(), roles));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponseDto> refreshToken(@RequestBody java.util.Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        String username = jwtTokenProvider.getUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            return ResponseEntity.status(401).build();
        }

        String tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        // Create a temporary authentication object for token generation
        Authentication authentication = new UsernamePasswordAuthenticationToken(username, null,
                roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList()));

        String newToken = jwtTokenProvider.generateToken(authentication, user.getId(), tenantId, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        return ResponseEntity
                .ok(new AuthResponseDto(newToken, newRefreshToken, user.getId(), user.getUsername(), roles));
    }

}
