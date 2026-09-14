package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
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
 * Tests d'intégration pour le module Livres (BooksController).
 * Vérifie la sécurité des endpoints /admin/books/** et la règle RG-06.
 * NOTE: GET /admin/books n'a pas de @PreAuthorize — un trou de sécurité connu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BooksControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private BooksRepository booksRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private String tokenAdmin;
    private Users admin;
    private String tokenAdherent;
    private Users adherent;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        booksRepository.deleteAll();
        usersRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setRoleName("User");
        userRole = roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setRoleName("Admin");
        adminRole = roleRepository.save(adminRole);

        adherent = createUser("adherent_user", "Adherent Test", userRole.getRoleId());
        admin = createUser("admin_user", "Admin Test", adminRole.getRoleId());

        tokenAdherent = jwtUtil.generateToken(jwtService.loadUserByUsername("adherent_user"));
        tokenAdmin = jwtUtil.generateToken(jwtService.loadUserByUsername("admin_user"));
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
    void unauthenticatedGetBooksReturns401() throws Exception {
        mockMvc.perform(get("/admin/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedGetBookReturns401() throws Exception {
        mockMvc.perform(get("/admin/books/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedCreateBookReturns401() throws Exception {
        Books book = new Books();
        book.setBookName("Test");
        mockMvc.perform(post("/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedUpdateBookReturns401() throws Exception {
        Books book = new Books();
        book.setBookName("Test");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(5);
        book = booksRepository.save(book);
        book.setBookName("Modified");
        mockMvc.perform(put("/admin/books/" + book.getBookId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedDeleteBookReturns401() throws Exception {
        Books book = new Books();
        book.setBookName("Test");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(5);
        book = booksRepository.save(book);
        mockMvc.perform(delete("/admin/books/" + book.getBookId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== RS-02: Autorisation par rôle ====================

    /**
     * NOTE: GET /admin/books n'a pas de @PreAuthorize dans BooksController.
     * L'adhérent peut donc accéder à cette liste. C'est un trou de sécurité.
     * Le test documente ce comportement actuel.
     */
    @Test
    void adherentCanAccessBooksListSinceEndpointIsOpen() throws Exception {
        mockMvc.perform(get("/admin/books")
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isOk());
    }

    @Test
    void adherentCannotGetBook() throws Exception {
        Books book = new Books();
        book.setBookName("Test");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(5);
        book = booksRepository.save(book);

        mockMvc.perform(get("/admin/books/" + book.getBookId())
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isForbidden());
    }

    @Test
    void adherentCannotCreateBook() throws Exception {
        Books book = new Books();
        book.setBookName("Nouveau Livre");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(5);

        mockMvc.perform(post("/admin/books")
                        .header("Authorization", "Bearer " + tokenAdherent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adherentCannotUpdateBook() throws Exception {
        Books book = new Books();
        book.setBookName("Test");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(5);
        book = booksRepository.save(book);
        book.setBookName("Modified");

        mockMvc.perform(put("/admin/books/" + book.getBookId())
                        .header("Authorization", "Bearer " + tokenAdherent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adherentCannotDeleteBook() throws Exception {
        Books book = new Books();
        book.setBookName("Test");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(5);
        book = booksRepository.save(book);

        mockMvc.perform(delete("/admin/books/" + book.getBookId())
                        .header("Authorization", "Bearer " + tokenAdherent))
                .andExpect(status().isForbidden());
    }

    // ==================== Admin: opérations autorisées ====================

    @Test
    void adminCanGetAllBooks() throws Exception {
        Books b1 = new Books();
        b1.setBookName("Livre 1");
        b1.setBookAuthor("Auteur 1");
        b1.setBookGenre("Roman");
        b1.setNoOfCopies(3);
        booksRepository.save(b1);

        Books b2 = new Books();
        b2.setBookName("Livre 2");
        b2.setBookAuthor("Auteur 2");
        b2.setBookGenre("Conte");
        b2.setNoOfCopies(1);
        booksRepository.save(b2);

        mockMvc.perform(get("/admin/books")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void adminCanGetBookById() throws Exception {
        Books book = new Books();
        book.setBookName("Livre Test");
        book.setBookAuthor("Auteur Test");
        book.setBookGenre("Roman");
        book.setNoOfCopies(2);
        book = booksRepository.save(book);

        mockMvc.perform(get("/admin/books/" + book.getBookId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("Livre Test"));
    }

    @Test
    void adminCanCreateBook() throws Exception {
        Books book = new Books();
        book.setBookName("Nouveau Livre");
        book.setBookAuthor("Nouvel Auteur");
        book.setBookGenre("Science-fiction");
        book.setNoOfCopies(10);

        mockMvc.perform(post("/admin/books")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("Nouveau Livre"));
    }

    @Test
    void adminCanUpdateBook() throws Exception {
        Books book = new Books();
        book.setBookName("Livre Original");
        book.setBookAuthor("Auteur Original");
        book.setBookGenre("Roman");
        book.setNoOfCopies(0);
        book = booksRepository.save(book);

        book.setBookName("Livre Modifie");
        book.setNoOfCopies(2);

        mockMvc.perform(put("/admin/books/" + book.getBookId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookName").value("Livre Modifie"))
                .andExpect(jsonPath("$.noOfCopies").value(2));
    }

    @Test
    void adminCanDeleteBook() throws Exception {
        Books book = new Books();
        book.setBookName("Livre a supprimer");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(5);
        book = booksRepository.save(book);

        mockMvc.perform(delete("/admin/books/" + book.getBookId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }

    @Test
    void getNonExistentBookReturns404() throws Exception {
        mockMvc.perform(get("/admin/books/99999")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNonExistentBookReturns404() throws Exception {
        Books book = new Books();
        book.setBookName("Test");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(1);

        mockMvc.perform(put("/admin/books/99999")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNonExistentBookReturns404() throws Exception {
        mockMvc.perform(delete("/admin/books/99999")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    // ==================== RG-06: Promouvoir reservation lors du retour disponibilite ====================

    @Test
    void updatingBookToAvailablePromotesNextReservation() throws Exception {
        Books livre = new Books();
        livre.setBookName("Livre Reserve");
        livre.setBookAuthor("Auteur");
        livre.setBookGenre("Roman");
        livre.setNoOfCopies(0);
        livre = booksRepository.save(livre);

        Role userRole = roleRepository.findAll().stream()
                .filter(r -> "User".equals(r.getRoleName()))
                .findFirst().orElseThrow();
        Users user = createUser("reserver", "Reserver", userRole.getRoleId());

        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(user);
        reservation.setDateReservation(new java.util.Date());
        reservation.setDateExpiration(new java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
        reservation.setStatut(ReservationStatus.EN_ATTENTE);
        reservation = reservationRepository.save(reservation);

        livre.setNoOfCopies(1);
        mockMvc.perform(put("/admin/books/" + livre.getBookId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(livre)))
                .andExpect(status().isOk());

        Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.DISPONIBLE, updated.getStatut());
    }

    @Test
    void updatingBookToStillUnavailableDoesNotPromote() throws Exception {
        Books livre = new Books();
        livre.setBookName("Livre Brouillon");
        livre.setBookAuthor("Auteur");
        livre.setBookGenre("Roman");
        livre.setNoOfCopies(0);
        livre = booksRepository.save(livre);

        Role userRole = roleRepository.findAll().stream()
                .filter(r -> "User".equals(r.getRoleName()))
                .findFirst().orElseThrow();
        Users user = createUser("reserver2", "Reserver 2", userRole.getRoleId());

        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(user);
        reservation.setDateReservation(new java.util.Date());
        reservation.setDateExpiration(new java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
        reservation.setStatut(ReservationStatus.EN_ATTENTE);
        reservation = reservationRepository.save(reservation);

        livre.setBookName("Livre Mis a jour");
        livre.setNoOfCopies(0);
        mockMvc.perform(put("/admin/books/" + livre.getBookId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(livre)))
                .andExpect(status().isOk());

        Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.EN_ATTENTE, updated.getStatut());
    }
}
