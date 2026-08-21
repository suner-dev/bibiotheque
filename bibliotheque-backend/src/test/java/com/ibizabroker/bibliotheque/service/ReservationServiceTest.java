package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Books book;
    private Users user;
    private ReservationRequest request;

    @BeforeEach
    void setUp() {
        book = new Books();
        book.setBookId(1);
        book.setBookName("Le Petit Prince");
        book.setBookAuthor("Antoine de Saint-Exupéry");
        book.setBookGenre("Conte");
        book.setNoOfCopies(0); // Unavailable

        user = new Users();
        user.setUserId(1);
        user.setName("Jean Dupont");
        user.setUsername("jean");

        request = new ReservationRequest();
        request.setLivreId(1);
        request.setAdherentId(1);
    }

    @Test
    void createReservation_ValidRequest_Success() {
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                anyInt(), anyInt(), any())).thenReturn(false);
        when(reservationRepository.countByAdherentUserIdAndStatutIn(anyInt(), any())).thenReturn(0L);
        when(reservationRepository.save(any())).thenAnswer(invocation -> {
            var reservation = invocation.getArgument(0);
            var field = reservation.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(reservation, 1);
            return reservation;
        });

        ReservationResponse response = reservationService.createReservation(request);

        assertNotNull(response);
        assertEquals(1, response.getLivreId());
        assertEquals(1, response.getAdherentId());
        assertEquals(ReservationStatus.EN_ATTENTE, response.getStatut());
        assertNotNull(response.getDateReservation());
        assertNotNull(response.getDateExpiration());

        // Verify expiration is +7 days
        Calendar cal = Calendar.getInstance();
        cal.setTime(response.getDateReservation());
        cal.add(Calendar.DATE, 7);
        assertEquals(cal.getTime(), response.getDateExpiration());
    }

    @Test
    void createReservation_LivreIdNull_ThrowsIllegalArgument() {
        request.setLivreId(null);
        assertThrows(IllegalArgumentException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void createReservation_AdherentIdNull_ThrowsIllegalArgument() {
        request.setAdherentId(null);
        assertThrows(IllegalArgumentException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void createReservation_BookNotFound_ThrowsNotFound() {
        when(booksRepository.findByIdForUpdate(999)).thenReturn(Optional.empty());
        request.setLivreId(999);
        assertThrows(NotFoundException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void createReservation_UserNotFound_ThrowsNotFound() {
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(999)).thenReturn(Optional.empty());
        request.setAdherentId(999);
        assertThrows(NotFoundException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void createReservation_BookAvailable_ThrowsConflict_RG01() {
        book.setNoOfCopies(5); // Available
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request));
        assertTrue(exception.getMessage().contains("RG-01"));
    }

    @Test
    void createReservation_DuplicateActiveReservation_ThrowsConflict_RG02() {
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                anyInt(), anyInt(), any())).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request));
        assertTrue(exception.getMessage().contains("RG-02"));
    }

    @Test
    void createReservation_MaxActiveReservations_ThrowsConflict_RG03() {
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                anyInt(), anyInt(), any())).thenReturn(false);
        when(reservationRepository.countByAdherentUserIdAndStatutIn(anyInt(), any())).thenReturn(3L);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request));
        assertTrue(exception.getMessage().contains("RG-03"));
    }

    @Test
    void createReservation_RG03_ThreeActiveAllowed_FourthRejected() {
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                anyInt(), anyInt(), any())).thenReturn(false);
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // 0, 1 and 2 active reservations: the 1st, 2nd and 3rd creations are allowed.
        for (long activeCount = 0; activeCount < 3; activeCount++) {
            when(reservationRepository.countByAdherentUserIdAndStatutIn(anyInt(), any())).thenReturn(activeCount);
            ReservationResponse response = reservationService.createReservation(request);
            assertEquals(ReservationStatus.EN_ATTENTE, response.getStatut());
        }

        // A 4th active reservation would exceed the limit of 3: rejected with RG-03.
        when(reservationRepository.countByAdherentUserIdAndStatutIn(anyInt(), any())).thenReturn(3L);
        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request));
        assertTrue(exception.getMessage().contains("RG-03"));
    }

    @Test
    void createReservation_DatesGenerated_ServerSide_RG04() {
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                anyInt(), anyInt(), any())).thenReturn(false);
        when(reservationRepository.countByAdherentUserIdAndStatutIn(anyInt(), any())).thenReturn(0L);
        when(reservationRepository.save(any())).thenAnswer(invocation -> {
            var reservation = invocation.getArgument(0);
            var field = reservation.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(reservation, 1);
            return reservation;
        });

        Date before = new Date();
        ReservationResponse response = reservationService.createReservation(request);
        Date after = new Date();

        // dateReservation should be between before and after
        assertFalse(response.getDateReservation().before(before));
        assertFalse(response.getDateReservation().after(after));

        // dateExpiration should be dateReservation + 7 days
        Calendar cal = Calendar.getInstance();
        cal.setTime(response.getDateReservation());
        cal.add(Calendar.DATE, 7);
        assertEquals(cal.getTime(), response.getDateExpiration());
    }

    @Test
    void annulerReservation_EnAttente_Success() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(1);
        reservation.setStatut(ReservationStatus.EN_ATTENTE);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.annulerReservation(1);

        assertEquals(ReservationStatus.ANNULEE, response.getStatut());
    }

    @Test
    void annulerReservation_Disponible_Success() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(1);
        reservation.setStatut(ReservationStatus.DISPONIBLE);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.annulerReservation(1);

        assertEquals(ReservationStatus.ANNULEE, response.getStatut());
    }

    @Test
    void annulerReservation_Annulee_ThrowsConflict_RG05() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(1);
        reservation.setStatut(ReservationStatus.ANNULEE);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(1));
        assertTrue(exception.getMessage().contains("RG-05"));
    }

    @Test
    void annulerReservation_Expirée_ThrowsConflict_RG05() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(1);
        reservation.setStatut(ReservationStatus.EXPIREE);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(1));
        assertTrue(exception.getMessage().contains("RG-05"));
    }

    @Test
    void annulerReservation_Honorée_ThrowsConflict_RG05() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(1);
        reservation.setStatut(ReservationStatus.HONOREE);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(1));
        assertTrue(exception.getMessage().contains("RG-05"));
    }

    @Test
    void annulerReservation_NotFound_ThrowsNotFound() {
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reservationService.annulerReservation(999));
    }

    @Test
    void deleteReservation_Valid_Success() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(1);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));

        assertDoesNotThrow(() -> reservationService.deleteReservation(1));
    }

    @Test
    void deleteReservation_NotFound_ThrowsNotFound() {
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reservationService.deleteReservation(999));
    }
}
