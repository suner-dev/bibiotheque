package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour ReservationScheduler (expiration automatique des reservations).
 * Le scheduler n'est pas testable directement (pas de retour), on verifie les side-effects.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ReservationSchedulerTest {

    @Autowired
    private ReservationScheduler reservationScheduler;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        booksRepository.deleteAll();
        usersRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void expirerReservationsPerimeesMarkExpiredReservations() {
        Role userRole = new Role();
        userRole.setRoleName("User");
        userRole = roleRepository.save(userRole);

        Users user = new Users();
        user.setUsername("user1");
        user.setPassword("pass");
        user.setName("User 1");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRole(roles);
        user = usersRepository.save(user);

        Books book = new Books();
        book.setBookName("Book");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(0);
        book = booksRepository.save(book);

        Reservation expiredReservation = new Reservation();
        expiredReservation.setLivre(book);
        expiredReservation.setAdherent(user);
        expiredReservation.setDateReservation(new java.util.Date());
        expiredReservation.setDateExpiration(new java.util.Date(System.currentTimeMillis() - 1000));
        expiredReservation.setStatut(ReservationStatus.EN_ATTENTE);
        expiredReservation = reservationRepository.save(expiredReservation);

        reservationScheduler.expirerReservations();

        Reservation updated = reservationRepository.findById(expiredReservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.EXPIREE, updated.getStatut());
    }

    @Test
    void expirerReservationsWithNoExpiredReservationsDoesNothing() {
        Role userRole = new Role();
        userRole.setRoleName("User");
        userRole = roleRepository.save(userRole);

        Users user = new Users();
        user.setUsername("user2");
        user.setPassword("pass");
        user.setName("User 2");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRole(roles);
        user = usersRepository.save(user);

        Books book = new Books();
        book.setBookName("Book");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(0);
        book = booksRepository.save(book);

        Reservation reservation = new Reservation();
        reservation.setLivre(book);
        reservation.setAdherent(user);
        reservation.setDateReservation(new java.util.Date());
        reservation.setDateExpiration(new java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
        reservation.setStatut(ReservationStatus.EN_ATTENTE);
        reservationRepository.save(reservation);

        reservationScheduler.expirerReservations();

        Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.EN_ATTENTE, updated.getStatut());
    }

    @Test
    void expirerReservationsIsIdempotent() {
        // Setup avec reservations
        Role userRole = new Role();
        userRole.setRoleName("User");
        userRole = roleRepository.save(userRole);

        Users user = new Users();
        user.setUsername("user3");
        user.setPassword("pass");
        user.setName("User 3");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRole(roles);
        user = usersRepository.save(user);

        Books book = new Books();
        book.setBookName("Book");
        book.setBookAuthor("Auteur");
        book.setBookGenre("Roman");
        book.setNoOfCopies(0);
        book = booksRepository.save(book);

        Reservation expiredRes = new Reservation();
        expiredRes.setLivre(book);
        expiredRes.setAdherent(user);
        expiredRes.setDateReservation(new java.util.Date());
        expiredRes.setDateExpiration(new java.util.Date(System.currentTimeMillis() - 1000));
        expiredRes.setStatut(ReservationStatus.EN_ATTENTE);
        reservationRepository.save(expiredRes);

        // Premier appel
        reservationScheduler.expirerReservations();
        // Verification : expiredRes est bien EXPIREE
        Reservation updated = reservationRepository.findById(expiredRes.getId()).orElseThrow();
        assertEquals(ReservationStatus.EXPIREE, updated.getStatut());

        // Deuxieme appel (rien a expirer)
        reservationScheduler.expirerReservations();
        // Verification : toujours EXPIREE
        Reservation updated2 = reservationRepository.findById(expiredRes.getId()).orElseThrow();
        assertEquals(ReservationStatus.EXPIREE, updated2.getStatut());
    }
}
