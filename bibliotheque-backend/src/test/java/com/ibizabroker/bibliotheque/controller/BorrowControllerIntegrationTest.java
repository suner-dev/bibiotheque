package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
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
 * Tests d'integration pour le module Emprunts (BorrowController).
 * Verifie RS-01 a RS-04, RG-06 et la gestion des erreurs.
 * NOTE: Le retour se fait via PUT /borrow avec le body contenant borrowId.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BorrowControllerIntegrationTest {

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
    private BorrowRepository borrowRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private String tokenAdmin;
    private String tokenAdherent1;
    private String tokenAdherent2;
    private Users adherent1;
    private Users adherent2;
    private Users admin;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        borrowRepository.deleteAll();
        booksRepository.deleteAll();
        usersRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setRoleName("User");
        userRole = roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setRoleName("Admin");
        adminRole = roleRepository.save(adminRole);

        adherent1 = createUser("adherent1", "Adherent 1", userRole.getRoleId());
        adherent2 = createUser("adherent2", "Adherent 2", userRole.getRoleId());
        admin = createUser("biblio", "Bibliothecaire", adminRole.getRoleId());

        tokenAdherent1 = jwtUtil.generateToken(jwtService.loadUserByUsername("adherent1"));
        tokenAdherent2 = jwtUtil.generateToken(jwtService.loadUserByUsername("adherent2"));
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

    private Books createAvailableBook(String name) {
        Books book = new Books();
        book.setBookName(name);
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(3);
        return booksRepository.save(book);
    }

    private Borrow createBorrowFor(Users user, Books book) {
        Borrow borrow = new Borrow();
        borrow.setUserId(user.getUserId());
        borrow.setBookId(book.getBookId());
        borrow.setIssueDate(new java.util.Date());
        return borrowRepository.save(borrow);
    }

    // ==================== RS-01: Authentification requise ====================

    @Test
    void unauthenticatedBorrowReturns401() throws Exception {
        Books book = createAvailableBook("Test Book");
        Borrow borrow = new Borrow();
        borrow.setUserId(adherent1.getUserId());
        borrow.setBookId(book.getBookId());

        mockMvc.perform(post("/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedGetBorrowsReturns401() throws Exception {
        mockMvc.perform(get("/borrow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedReturnBookReturns401() throws Exception {
        Books book = createAvailableBook("Test Book");
        Borrow borrow = createBorrowFor(adherent1, book);

        mockMvc.perform(put("/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== RS-02: Bibliothecaire peut tout faire ====================

    @Test
    void bibliothecaireCanBorrowForAnyUser() throws Exception {
        Books book = createAvailableBook("Book for Admin Borrow");

        Borrow borrow = new Borrow();
        borrow.setUserId(adherent2.getUserId());
        borrow.setBookId(book.getBookId());

        mockMvc.perform(post("/borrow")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isOk());

        Books updated = booksRepository.findById(book.getBookId()).orElseThrow();
        assertEquals(2, updated.getNoOfCopies());
    }

    @Test
    void bibliothecaireCanViewAllBorrows() throws Exception {
        Books book1 = createAvailableBook("Book 1");
        createBorrowFor(adherent1, book1);

        Books book2 = createAvailableBook("Book 2");
        createBorrowFor(adherent2, book2);

        mockMvc.perform(get("/borrow")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void bibliothecaireCanReturnAnyBorrow() throws Exception {
        Books book = createAvailableBook("Book for Return");
        int copiesBefore = book.getNoOfCopies();
        Borrow borrow = createBorrowFor(adherent1, book);

        mockMvc.perform(put("/borrow")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isOk());

        Books updated = booksRepository.findById(book.getBookId()).orElseThrow();
        assertEquals(copiesBefore + 1, updated.getNoOfCopies());
    }

    // ==================== RS-03: Adherent voit/retourne ses propres emprunts ====================

    @Test
    void adherentCannotReturnOtherAdherentBorrow() throws Exception {
        Books book = createAvailableBook("Restricted Book 2");
        Borrow borrow = createBorrowFor(adherent2, book);

        mockMvc.perform(put("/borrow")
                        .header("Authorization", "Bearer " + tokenAdherent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adherentCanReturnOwnBorrow() throws Exception {
        Books book = createAvailableBook("My Book 2");
        int copiesBefore = book.getNoOfCopies();
        Borrow borrow = createBorrowFor(adherent1, book);

        mockMvc.perform(put("/borrow")
                        .header("Authorization", "Bearer " + tokenAdherent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isOk());

        Books updated = booksRepository.findById(book.getBookId()).orElseThrow();
        assertEquals(copiesBefore + 1, updated.getNoOfCopies());
    }

    @Test
    void adherentCanViewOwnBorrowsViaUserEndpoint() throws Exception {
        Books book1 = createAvailableBook("User Book 1");
        Books book2 = createAvailableBook("User Book 2");
        createBorrowFor(adherent1, book1);
        createBorrowFor(adherent1, book2);
        createBorrowFor(adherent2, book1);

        mockMvc.perform(get("/borrow/user/" + adherent1.getUserId())
                        .header("Authorization", "Bearer " + tokenAdherent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void adherentCannotViewOtherUserBorrowViaUserEndpoint() throws Exception {
        Books book = createAvailableBook("Other User Book");
        createBorrowFor(adherent2, book);

        mockMvc.perform(get("/borrow/user/" + adherent2.getUserId())
                        .header("Authorization", "Bearer " + tokenAdherent1))
                .andExpect(status().isForbidden());
    }

    // ==================== RS-04: Identite depuis token ====================

    @Test
    void adherentBorrowIgnoresBodyUserId() throws Exception {
        Books book = createAvailableBook("Token Identity Book");

        Borrow borrow = new Borrow();
        borrow.setUserId(adherent2.getUserId());
        borrow.setBookId(book.getBookId());

        mockMvc.perform(post("/borrow")
                        .header("Authorization", "Bearer " + tokenAdherent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isOk());

        Borrow saved = borrowRepository.findAll().stream()
                .filter(b -> b.getBookId().equals(book.getBookId()))
                .findFirst().orElseThrow();
        assertEquals(adherent1.getUserId(), saved.getUserId());
    }

    // ==================== Gestion des erreurs ====================

    @Test
    void borrowUnavailableBookReturns409() throws Exception {
        Books book = new Books();
        book.setBookName("Epuise");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(0);
        book = booksRepository.save(book);

        Borrow borrow = new Borrow();
        borrow.setUserId(adherent1.getUserId());
        borrow.setBookId(book.getBookId());

        mockMvc.perform(post("/borrow")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isConflict());
    }

    @Test
    void borrowNonExistentBookReturns404() throws Exception {
        Borrow borrow = new Borrow();
        borrow.setUserId(adherent1.getUserId());
        borrow.setBookId(99999);

        mockMvc.perform(post("/borrow")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnNonExistentBorrowReturns404() throws Exception {
        Borrow borrow = new Borrow();
        borrow.setBorrowId(99999);
        borrow.setBookId(1);

        mockMvc.perform(put("/borrow")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedUserEndpointReturns401() throws Exception {
        mockMvc.perform(get("/borrow/user/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedAdherentCannotAccessGetAllBorrows() throws Exception {
        mockMvc.perform(get("/borrow")
                        .header("Authorization", "Bearer " + tokenAdherent1))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedAdherentCanAccessBookHistory() throws Exception {
        Books book = createAvailableBook("Book History");
        createBorrowFor(adherent1, book);

        mockMvc.perform(get("/borrow/book/" + book.getBookId())
                        .header("Authorization", "Bearer " + tokenAdherent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
