package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour CurrentUserService (recuperation de l'utilisateur courant).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CurrentUserServiceTest {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        usersRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setRoleName("User");
        userRole = roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setRoleName("Admin");
        adminRole = roleRepository.save(adminRole);

        Users user = new Users();
        user.setUsername("adherent");
        user.setPassword("pass");
        user.setName("Adherent");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRole(roles);
        usersRepository.save(user);

        Users admin = new Users();
        admin.setUsername("admin");
        admin.setPassword("pass");
        admin.setName("Admin");
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        admin.setRole(adminRoles);
        usersRepository.save(admin);
    }

    private void setSecurityContext(String username, boolean isAdmin) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (isAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_Admin"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_User"));
        }

        UserDetails userDetails = new User(username, "password", authorities);
        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getCurrentUsernameReturnsNullWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        String username = currentUserService.getCurrentUsername();

        assertNull(username);
    }

    @Test
    void getCurrentUsernameReturnsUsernameWhenAuthenticated() {
        setSecurityContext("adherent", false);

        String username = currentUserService.getCurrentUsername();

        assertEquals("adherent", username);
    }

    @Test
    void getCurrentUserReturnsUserWhenAuthenticated() {
        setSecurityContext("adherent", false);

        Users user = currentUserService.getCurrentUser();

        assertNotNull(user);
        assertEquals("adherent", user.getUsername());
        assertEquals("Adherent", user.getName());
    }

    @Test
    void getCurrentUserReturnsNullWhenUsernameNotFound() {
        setSecurityContext("unknown", false);

        Users user = currentUserService.getCurrentUser();

        assertNull(user);
    }

    @Test
    void getCurrentUserIdReturnsUserIdWhenAuthenticated() {
        setSecurityContext("adherent", false);

        Integer userId = currentUserService.getCurrentUserId();

        assertNotNull(userId);
        assertEquals(usersRepository.findByUsername("adherent").get().getUserId(), userId);
    }

    @Test
    void getCurrentUserIdReturnsNullWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        Integer userId = currentUserService.getCurrentUserId();

        assertNull(userId);
    }

    @Test
    void isBibliothecaireReturnsTrueForAdmin() {
        setSecurityContext("admin", true);

        assertTrue(currentUserService.isBibliothecaire());
    }

    @Test
    void isBibliothecaireReturnsFalseForAdherent() {
        setSecurityContext("adherent", false);

        assertFalse(currentUserService.isBibliothecaire());
    }

    @Test
    void isBibliothecaireReturnsFalseWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertFalse(currentUserService.isBibliothecaire());
    }

    @Test
    void isAdherentReturnsTrueForAdherent() {
        setSecurityContext("adherent", false);

        assertTrue(currentUserService.isAdherent());
    }

    @Test
    void isAdherentReturnsFalseForAdmin() {
        setSecurityContext("admin", true);

        assertFalse(currentUserService.isAdherent());
    }

    @Test
    void isAdherentReturnsFalseWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertFalse(currentUserService.isAdherent());
    }

    @Test
    void canAccessAllReservationsReturnsTrueForAdmin() {
        setSecurityContext("admin", true);

        assertTrue(currentUserService.canAccessAllReservations());
    }

    @Test
    void canAccessAllReservationsReturnsFalseForAdherent() {
        setSecurityContext("adherent", false);

        assertFalse(currentUserService.canAccessAllReservations());
    }

    @Test
    void canAccessAllReservationsReturnsFalseWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertFalse(currentUserService.canAccessAllReservations());
    }
}
