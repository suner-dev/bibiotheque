package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour JwtService (generation/Validation de tokens JWT).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void createJwtTokenWithValidCredentialsReturnsJwtResponse() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        JwtResponse response = jwtService.createJwtToken(request);

        assertNotNull(response);
        assertNotNull(response.getJwtToken());
        assertTrue(response.getJwtToken().length() > 0);
        assertEquals("testuser", response.getUser().getUsername());
    }

    @Test
    void createJwtTokenWithInvalidPasswordThrowsException() {
        JwtRequest request = new JwtRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> jwtService.createJwtToken(request));

        assertTrue(exception.getMessage().contains("INVALID_CREDENTIALS"));
    }

    @Test
    void createJwtTokenWithNonExistentUserThrowsException() {
        JwtRequest request = new JwtRequest();
        request.setUsername("nobody");
        request.setPassword("password");

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> jwtService.createJwtToken(request));

        assertTrue(exception.getMessage().contains("INVALID_CREDENTIALS"));
    }

    @Test
    void loadUserByUsernameReturnsUserDetails() throws Exception {
        UserDetails userDetails = jwtService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertNotNull(userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_User")));
    }

    @Test
    void loadUserByUsernameWithNonExistentUserThrowsException() {
        assertThrows(UsernameNotFoundException.class,
                () -> jwtService.loadUserByUsername("nobody"));
    }

    @Test
    void generateTokenReturnsValidToken() {
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "testuser",
                        "password",
                        java.util.Collections.singletonList(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_User")
                        )
                );

        String token = jwtUtil.generateToken(userDetails);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void validateTokenWithValidTokenReturnsTrue() {
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "testuser",
                        "password",
                        java.util.Collections.singletonList(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_User")
                        )
                );

        String token = jwtUtil.generateToken(userDetails);

        assertTrue(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    void validateTokenWithInvalidTokenReturnsFalse() {
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "testuser",
                        "password",
                        java.util.Collections.singletonList(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_User")
                        )
                );

        assertFalse(jwtUtil.validateToken("invalid.token.here", userDetails));
    }

    @Test
    void getUsernameFromTokenReturnsCorrectUsername() {
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "testuser",
                        "password",
                        java.util.Collections.singletonList(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_User")
                        )
                );

        String token = jwtUtil.generateToken(userDetails);
        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    void generateTokenWithAdminRole() throws Exception {
        usersRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = new Role();
        adminRole.setRoleName("Admin");
        adminRole = roleRepository.save(adminRole);

        Users admin = new Users();
        admin.setUsername("adminuser");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setName("Admin User");
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRole(roles);
        usersRepository.save(admin);

        JwtRequest request = new JwtRequest();
        request.setUsername("adminuser");
        request.setPassword("adminpass");

        JwtResponse response = jwtService.createJwtToken(request);

        assertNotNull(response);
        assertNotNull(response.getJwtToken());
        assertEquals("adminuser", response.getUser().getUsername());
    }
}
