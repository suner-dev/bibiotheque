package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'integration pour le module d'authentification (JwtController).
 * Verifie POST /authenticate avec credentials valides/invalides.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class JwtControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        usersRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setRoleName("User");
        userRole = roleRepository.save(userRole);

        Users user = new Users();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setName("Test User");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRole(roles);
        usersRepository.save(user);
    }

    @Test
    void authenticateWithValidCredentialsReturnsToken() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").isNotEmpty())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertTrue(responseJson.contains("jwtToken"));
        assertTrue(responseJson.contains("testuser"));
    }

    @Test
    void authenticateWithInvalidPasswordReturns401() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticateWithNonExistentUserReturns401() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("nobody");
        request.setPassword("password");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticateIsPublicEndpoint() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
