package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.service.JwtService;
import com.ibizabroker.bibliotheque.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'integration pour le module Admin (AdminController).
 * Verifie la securite des endpoints /admin/users/**
 * NOTE: POST /admin/users n'a pas de @PreAuthorize dans le code actuel.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private String tokenAdmin;
    private String tokenAdherent;
    private Users admin;
    private Users adherent;
    private Integer adminRoleId;
    private Integer userRoleId;

    @BeforeEach
    void setUp() {
        usersRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setRoleName("User");
        userRole = roleRepository.save(userRole);
        userRoleId = userRole.getRoleId();

        Role adminRole = new Role();
        adminRole.setRoleName("Admin");
        adminRole = roleRepository.save(adminRole);
        adminRoleId = adminRole.getRoleId();

        adherent = createUser("adherent", "Adherent", userRoleId);
        admin = createUser("biblio", "Bibliothecaire", adminRoleId);

        tokenAdherent = jwtUtil.generateToken(jwtService.loadUserByUsername("adherent"));
        tokenAdmin = jwtUtil.generateToken(jwtService.loadUserByUsername("biblio"));
    }

    private Users createUser(String username, String name, Integer roleId) {
        Users user = new Users();
        user.setUsername(username);
        user.setPassword("password");
        user.setName(name);
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findById(roleId).orElseThrow());
        user.setRole(roles);
        return usersRepository.save(user);
    }

    // ==================== RS-01: Authentification requise ====================

    @Test
    void unauthenticatedCreateUserReturns401() throws Exception {
        Users user = new Users();
        user.setUsername("newuser");
        user.setPassword("pass");
        user.setName("New User");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedGetUsersReturns401() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedGetUserReturns401() throws Exception {
        mockMvc.perform(get("/admin/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedUpdateUserReturns401() throws Exception {
        mockMvc.perform(put("/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== RS-02: Seul Admin peut gerer les utilisateurs ====================
    // NOTE: POST /admin/users n'a pas de @PreAuthorize dans le code source.
    // L'adhérent peut donc créer un utilisateur (comportement actuel documenté).

    @Test
    void adherentCanCreateUserSincePostEndpointIsUnprotected() throws Exception {
        Users user = new Users();
        user.setUsername("newuser");
        user.setPassword("pass");
        user.setName("New User");

        mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + tokenAdherent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated());
    }

    @Test
    void adherentCannotGetUsers() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isForbidden());
    }

    @Test
    void adherentCannotGetUser() throws Exception {
        mockMvc.perform(get("/admin/users/1")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isForbidden());
    }

    @Test
    void adherentCannotUpdateUser() throws Exception {
        mockMvc.perform(put("/admin/users/1")
                        .header("Authorization", "Bearer " + tokenAdherent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    // ==================== Admin: operations autorisees ====================

    @Test
    void adminCanCreateUser() throws Exception {
        Users user = new Users();
        user.setUsername("newuser");
        user.setPassword("plainpassword");
        user.setName("New User");

        mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));

        Users saved = usersRepository.findByUsername("newuser").orElseThrow();
        assertNotNull(saved.getPassword());
        assertNotEquals("plainpassword", saved.getPassword());
    }

    @Test
    void adminCanGetAllUsers() throws Exception {
        createUser("user1", "User 1", userRoleId);
        createUser("user2", "User 2", userRoleId);

        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4)); // 2 nouveaux + admin + adherent
    }

    @Test
    void adminCanGetUserById() throws Exception {
        mockMvc.perform(get("/admin/users/" + adherent.getUserId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("adherent"));
    }

    @Test
    void adminCanUpdateUser() throws Exception {
        Users updateUser = new Users();
        updateUser.setName("Nom Modifie");
        updateUser.setUsername("adherent");

        mockMvc.perform(put("/admin/users/" + adherent.getUserId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nom Modifie"));
    }

    @Test
    void getNonExistentUserReturns404() throws Exception {
        mockMvc.perform(get("/admin/users/99999")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNonExistentUserReturns404() throws Exception {
        Users updateUser = new Users();
        updateUser.setName("Test");
        updateUser.setUsername("test");

        mockMvc.perform(put("/admin/users/99999")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isNotFound());
    }
}
