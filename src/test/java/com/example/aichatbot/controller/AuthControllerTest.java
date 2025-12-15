package com.example.aichatbot.controller;

import com.example.aichatbot.dto.LoginRequestDto;
import com.example.aichatbot.model.User;
import com.example.aichatbot.repository.UserRepository;
import com.example.aichatbot.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureJson
@Import({JacksonAutoConfiguration.class, TestSecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.example.aichatbot.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_ValidCredentials_ReturnsToken() throws Exception {
        // Arrange
        LoginRequestDto request = new LoginRequestDto("testuser", "password123");

        // Create a mock user
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");
        mockUser.setHashedPassword("hashed-password");

        // Mock authentication
        Authentication mockAuth =
                new UsernamePasswordAuthenticationToken("testuser", "password123");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        // Mock user repository to return the test user
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(mockUser));

        // Mock token generation
        when(jwtTokenProvider.generateToken(mockAuth))
                .thenReturn("mock-jwt-token");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.userId").value(1))  // Changed from $.id to $.userId
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void register_NewUser_ReturnsUser() throws Exception {
        // Arrange
        LoginRequestDto request = new LoginRequestDto("testuser", "password123");

        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setUsername("newuser");
        savedUser.setHashedPassword("hashed-password");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void register_ExistingUsername_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequestDto request = new LoginRequestDto("existinguser", "password123");

        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setUsername("existinguser");
        existingUser.setHashedPassword("hashed-password");

        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}