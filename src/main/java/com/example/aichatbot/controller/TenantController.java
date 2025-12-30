package com.example.aichatbot.controller;

import com.example.aichatbot.dto.LoginRequestDto;
import com.example.aichatbot.dto.TenantRegistrationDto;
import com.example.aichatbot.dto.UserUpdateDto;
import com.example.aichatbot.enums.Role;
import com.example.aichatbot.exception.UserNotFoundException;
import com.example.aichatbot.model.Tenant;
import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.TenantRepository;
import com.example.aichatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.aichatbot.service.DocumentService documentService;
    private final com.example.aichatbot.repository.ConversationRepository conversationRepository;

    @PostMapping("/register")
    public ResponseEntity<Tenant> registerTenant(@RequestBody TenantRegistrationDto request) {
        log.info("Received tenant registration request for: {}", request.tenantName());
        try {
            if (userRepository.findByUsername(request.adminUsername()).isPresent()) {
                log.warn("Registration failed: Username {} already exists", request.adminUsername());
                return ResponseEntity.badRequest().build();
            }

            Tenant tenant = new Tenant();
            tenant.setName(request.tenantName());
            tenant = tenantRepository.save(tenant);
            log.info("Created tenant: {} (ID: {})", tenant.getName(), tenant.getId());

            User admin = new User();
            admin.setUsername(request.adminUsername());
            admin.setHashedPassword(passwordEncoder.encode(request.adminPassword()));
            admin.setTenant(tenant);
            admin.setRoles(Set.of(Role.ADMIN, Role.USER));
            userRepository.save(admin);
            log.info("Created admin user: {}", admin.getUsername());

            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            log.error("CRITICAL ERROR during tenant registration: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> createUser(@RequestBody LoginRequestDto request, Principal principal) {
        log.info("Admin {} is creating a new user: {}", principal.getName(), request.username());

        User admin = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException("User", principal.getName()));

        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setHashedPassword(passwordEncoder.encode(request.password()));
        newUser.setTenant(admin.getTenant());
        newUser.setRoles(Set.of(Role.USER));

        return ResponseEntity.ok(userRepository.save(newUser));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> listUsers(Principal principal) {
        User admin = getAdmin(principal);
        return ResponseEntity.ok(userRepository.findByTenantId(admin.getTenant().getId()));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUser(@PathVariable String userId, Principal principal) {
        User admin = getAdmin(principal);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User", userId));

        if (!user.getTenant().getId().equals(admin.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody UserUpdateDto request,
            Principal principal) {
        User admin = getAdmin(principal);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User", userId));

        if (!user.getTenant().getId().equals(admin.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        if (request.username() != null)
            user.setUsername(request.username());
        if (request.roles() != null)
            user.setRoles(request.roles());
        if (request.status() != null)
            user.setStatus(request.status());

        return ResponseEntity.ok(userRepository.save(user));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable String userId, Principal principal) {
        User admin = getAdmin(principal);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User", userId));

        if (!user.getTenant().getId().equals(admin.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        documentService.deleteUserContent(userId);

        conversationRepository.deleteByUserId(userId);

        userRepository.delete(user);

        return ResponseEntity.noContent().build();
    }

    private User getAdmin(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UserNotFoundException("User", principal.getName()));
    }
}
