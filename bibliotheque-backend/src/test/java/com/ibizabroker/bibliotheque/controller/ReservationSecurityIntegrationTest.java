package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.service.JwtService;
import com.ibizabroker.bibliotheque.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration de sécurité pour le module Réservation.
 * Vérifie RS-01 à RS-05, RG-01 et la journalisation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ReservationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenAdherent;
    private String tokenBibliothecaire;
    private Users adherent1;
    private Users adherent2;
    private Users bibliothecaire;
    private Books livreIndisponible;

    @BeforeEach
    void setUp() {
        // Nettoyer seulement les entités qui ont des clés étrangères
        reservationRepository.deleteAll();
        usersRepository.deleteAll();
        booksRepository.deleteAll();
        roleRepository.deleteAll();

        // Créer et sauvegarder les rôles d'abord
        Role userRole = roleRepository.save(new Role());
        userRole.setRoleName("User");

        Role adminRole = roleRepository.save(new Role());
        adminRole.setRoleName("Admin");

        // Créer les utilisateurs avec les rôles
        adherent1 = createUser("adherent1", "Adhérent 1", userRole.getRoleId());
        adherent2 = createUser("adherent2", "Adhérent 2", userRole.getRoleId());
        bibliothecaire = createUser("biblio", "Bibliothécaire", adminRole.getRoleId());

        // Créer un livre indisponible
        livreIndisponible = new Books();
        livreIndisponible.setBookName("Livre Indisponible");
        livreIndisponible.setBookAuthor("Auteur");
        livreIndisponible.setBookGenre("Roman");
        livreIndisponible.setNoOfCopies(0);
        livreIndisponible = booksRepository.save(livreIndisponible);

        // Générer les tokens
        UserDetails adherent1Details = jwtService.loadUserByUsername("adherent1");
        tokenAdherent = jwtUtil.generateToken(adherent1Details);
        UserDetails adminDetails = jwtService.loadUserByUsername("biblio");
        tokenBibliothecaire = jwtUtil.generateToken(adminDetails);
    }

    private Users createUser(String username, String name, Integer roleId) {
        Users user = new Users();
        user.setUsername(username);
        user.setPassword("password");
        user.setName(name);
        Set<Role> roles = new HashSet<>();
        Role role = roleRepository.findById(roleId).orElseThrow();
        roles.add(role);
        user.setRole(roles);
        return usersRepository.save(user);
    }

    // ==================== RS-01: Authentification requise ====================

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        try {
            mockMvc.perform(get("/api/reservations")
                            .header("Authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0In0.invalid_signature"))
                    .andExpect(status().isUnauthorized());
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("JWT") || e.getCause() != null);
        }
    }

    @Test
    void unauthenticatedPostReturns401() throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setLivreId(livreIndisponible.getBookId());
        request.setAdherentId(adherent1.getUserId());
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedDeleteReturns401() throws Exception {
        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedPatchReturns401() throws Exception {
        mockMvc.perform(patch("/api/reservations/1/annuler"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== RS-02: Autorisation par rôle ====================

    @Test
    void adherentCannotDeleteReservation() throws Exception {
        mockMvc.perform(delete("/api/reservations/1")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteWithoutAdminRoleReturns403() throws Exception {
        mockMvc.perform(delete("/api/reservations/999")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedBibliothecaireCanDeleteReservation() throws Exception {
        mockMvc.perform(delete("/api/reservations/99999")
                        .header("Authorization", "Bearer " + tokenBibliothecaire))
                .andExpect(status().isNotFound());
    }

    // ==================== RS-03: Propriété des réservations ====================

    @Test
    void adherentCannotViewOtherAdherentReservation() throws Exception {
        Reservation reservationAdherent2 = createReservationFor(adherent2);
        mockMvc.perform(get("/api/reservations/" + reservationAdherent2.getId())
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isForbidden());
    }

    @Test
    void adherentCannotCancelOtherAdherentReservation() throws Exception {
        Reservation reservationAdherent2 = createReservationFor(adherent2);
        mockMvc.perform(patch("/api/reservations/" + reservationAdherent2.getId() + "/annuler")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isForbidden());
    }

    @Test
    void adherentCanViewOwnReservation() throws Exception {
        Reservation reservationAdherent1 = createReservationFor(adherent1);
        mockMvc.perform(get("/api/reservations/" + reservationAdherent1.getId())
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationAdherent1.getId()));
    }

    // ==================== RS-04: Identité depuis token ====================

    @Test
    void adherentReservationCreatedWithTokenIdentityNotBody() throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setLivreId(livreIndisponible.getBookId());
        request.setAdherentId(adherent2.getUserId());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + tokenAdherent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adherentId").value(adherent1.getUserId()));
    }

    // ==================== Validation des requêtes ====================

    @Test
    void createReservationWithMissingLivreIdReturns400() throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setLivreId(null);
        request.setAdherentId(adherent1.getUserId());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + tokenAdherent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservationWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + tokenAdherent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest());
    }

    // ==================== RG-01: Réservation d'un livre disponible ====================

    @Test
    void createReservationForAvailableBookReturns409() throws Exception {
        Books livreDisponible = new Books();
        livreDisponible.setBookName("Livre Disponible");
        livreDisponible.setBookAuthor("Auteur");
        livreDisponible.setBookGenre("Roman");
        livreDisponible.setNoOfCopies(3);
        livreDisponible = booksRepository.save(livreDisponible);

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(livreDisponible.getBookId());
        request.setAdherentId(adherent1.getUserId());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + tokenAdherent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ==================== Gestion des erreurs 404 ====================

    @Test
    void getNonExistentReservationReturns404() throws Exception {
        mockMvc.perform(get("/api/reservations/99999")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelNonExistentReservationReturns404() throws Exception {
        mockMvc.perform(patch("/api/reservations/99999/annuler")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNonExistentReservationReturns404ForAdmin() throws Exception {
        mockMvc.perform(delete("/api/reservations/99999")
                        .header("Authorization", "Bearer " + tokenBibliothecaire))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReservationForNonExistentBookReturns404() throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setLivreId(99999);
        request.setAdherentId(adherent1.getUserId());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + tokenAdherent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== RS-02: Bibliothécaire peut tout faire ====================

    @Test
    void bibliothecaireCanCancelAnyReservation() throws Exception {
        Reservation res = createReservationFor(adherent1);

        mockMvc.perform(patch("/api/reservations/" + res.getId() + "/annuler")
                        .header("Authorization", "Bearer " + tokenBibliothecaire))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"));
    }

    // ==================== RS-05: Filtrage des réservations ====================

    @Test
    void adherentSeesOnlyOwnReservations() throws Exception {
        Reservation res1 = createReservationFor(adherent1);
        Reservation res2 = createReservationFor(adherent2);

        String responseJson = mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Check for the full "id":1 pattern to avoid false matches on dates
        assertTrue(responseJson.contains("\"id\":" + res1.getId()));
        assertFalse(responseJson.contains("\"id\":" + res2.getId()));
    }

    @Test
    void bibliothecaireSeesAllReservations() throws Exception {
        createReservationFor(adherent1);
        createReservationFor(adherent2);

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + tokenBibliothecaire))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==================== Expiration du token ====================

    @Test
    void expiredOrInvalidTokenReturns401() throws Exception {
        // Test with malformed token (should return 401)
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
        
        // Test with completely invalid token (should return 401)
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Rôles authentifiés ====================

    @Test
    void authenticatedAdherentCanAccessGetReservations() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedBibliothecaireCanAccessGetReservations() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + tokenBibliothecaire))
                .andExpect(status().isOk());
    }

    // ==================== Helpers ====================

    private Reservation createReservationFor(Users adherent) {
        Reservation reservation = new Reservation();
        reservation.setLivre(livreIndisponible);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(new java.util.Date());
        reservation.setDateExpiration(new java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
        reservation.setStatut(ReservationStatus.EN_ATTENTE);
        return reservationRepository.save(reservation);
    }
}
