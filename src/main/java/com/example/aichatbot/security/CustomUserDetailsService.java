package com.example.aichatbot.security;

import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

                var authorities = user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority(
                                                "ROLE_" + role.name()))
                                .collect(java.util.stream.Collectors.toList());

                boolean enabled = user.getStatus() == com.example.aichatbot.enums.UserStatus.ACTIVE;

                return new org.springframework.security.core.userdetails.User(
                                user.getUsername(),
                                user.getHashedPassword(),
                                enabled, true, true, true,
                                authorities);
        }
}
