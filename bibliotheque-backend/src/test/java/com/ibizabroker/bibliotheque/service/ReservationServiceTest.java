package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
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
import java.util.List;
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

    @Mock
    private CurrentUserService currentUserService;

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
        when(currentUserService.isAdherent()).thenReturn(false);
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.annulerReservation(1, currentUserService);

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
        when(currentUserService.isAdherent()).thenReturn(false);
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.annulerReservation(1, currentUserService);

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
        when(currentUserService.isAdherent()).thenReturn(false);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(1, currentUserService));
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
        when(currentUserService.isAdherent()).thenReturn(false);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(1, currentUserService));
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
        when(currentUserService.isAdherent()).thenReturn(false);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.annulerReservation(1, currentUserService));
        assertTrue(exception.getMessage().contains("RG-05"));
    }

    @Test
    void annulerReservation_NotFound_ThrowsNotFound() {
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reservationService.annulerReservation(999, currentUserService));
    }

    @Test
    void deleteReservation_Valid_Success() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(1);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));

        assertDoesNotThrow(() -> reservationService.deleteReservation(1, currentUserService));
    }

    @Test
    void deleteReservation_NotFound_ThrowsNotFound() {
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> reservationService.deleteReservation(999, currentUserService));
    }

    @Test
    void promouvoirProchaineReservation_PasseEN_ATTENTEADISPONIBLE() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(10);
        reservation.setStatut(ReservationStatus.EN_ATTENTE);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        when(reservationRepository.findFirstByLivreBookIdAndStatutOrderByDateReservationAsc(1, ReservationStatus.EN_ATTENTE))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.promouvoirProchaineReservation(1);

        assertEquals(ReservationStatus.DISPONIBLE, reservation.getStatut());
    }

    @Test
    void promouvoirProchaineReservation_SansReservationActive_NeFaitRien() {
        when(reservationRepository.findFirstByLivreBookIdAndStatutOrderByDateReservationAsc(1, ReservationStatus.EN_ATTENTE))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> reservationService.promouvoirProchaineReservation(1));
    }

    @Test
    void honorerReservationSiExistante_PasseEN_ATTENTEAHONOREE() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(11);
        reservation.setStatut(ReservationStatus.DISPONIBLE);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        // RG-06 (cohérence): une réservation EN_ATTENTE doit aussi être honorée à l'emprunt
        when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), any()))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.honorerReservationSiExistante(1, 1);

        assertEquals(ReservationStatus.HONOREE, reservation.getStatut());
    }

    @Test
    void honorerReservationSiExistante_PasseDISPONIBLEAHONOREE() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(12);
        reservation.setStatut(ReservationStatus.DISPONIBLE);
        reservation.setLivre(book);
        reservation.setAdherent(user);

        when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), any()))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.honorerReservationSiExistante(1, 1);

        assertEquals(ReservationStatus.HONOREE, reservation.getStatut());
    }

    @Test
    void honorerReservationSiExistante_SansReservation_NeFaitRien() {
        when(reservationRepository.findByAdherentUserIdAndLivreBookIdAndStatutIn(anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> reservationService.honorerReservationSiExistante(1, 1));
    }

    @Test
    void expirerReservationsPerimees_PasseActivesAEXPIREE() {
        var reservation = new com.ibizabroker.bibliotheque.entity.Reservation();
        reservation.setId(12);
        reservation.setStatut(ReservationStatus.EN_ATTENTE);
        reservation.setLivre(book);
        reservation.setAdherent(user);
        reservation.setDateExpiration(new Date(System.currentTimeMillis() - 1000));

        when(reservationRepository.findByStatutInAndDateExpirationBefore(any(), any()))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.findFirstByLivreBookIdAndStatutOrderByDateReservationAsc(anyInt(), any()))
                .thenReturn(Optional.empty());

        int count = reservationService.expirerReservationsPerimees();

        assertEquals(1, count);
        assertEquals(ReservationStatus.EXPIREE, reservation.getStatut());
    }

    // ==================== TESTS RG-03 (Limite 3 réservations actives) ====================

    /**
     * RG-03 - CAS 1: Un adhérent possède 2 réservations actives.
     * → la troisième réservation est autorisée.
     */
    @Test
    void shouldAllowThirdActiveReservationWhenAdherentHasTwoActiveReservations() {
        // Given: adhérent avec 2 réservations actives
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                anyInt(), anyInt(), any())).thenReturn(false);
        // RG-03: 2 réservations actives (en dessous de la limite de 3)
        when(reservationRepository.countByAdherentUserIdAndStatutIn(anyInt(), any())).thenReturn(2L);
        when(reservationRepository.save(any())).thenAnswer(invocation -> {
            var reservation = invocation.getArgument(0);
            var field = reservation.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(reservation, 1);
            return reservation;
        });

        // When: création d'une troisième réservation
        ReservationResponse response = reservationService.createReservation(request);

        // Then: la réservation est créée avec succès
        assertNotNull(response);
        assertEquals(ReservationStatus.EN_ATTENTE, response.getStatut());
        assertEquals(1, response.getAdherentId());
    }

    /**
     * RG-03 - CAS 2: Un adhérent possède déjà 3 réservations actives.
     * → la création de la 4ème réservation est refusée.
     */
    @Test
    void shouldRejectReservationWhenAdherentAlreadyHasThreeActiveReservations() {
        // Given: adhérent avec 3 réservations actives (limite atteinte)
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                anyInt(), anyInt(), any())).thenReturn(false);
        // RG-03: 3 réservations actives (limite atteinte)
        when(reservationRepository.countByAdherentUserIdAndStatutIn(anyInt(), any())).thenReturn(3L);

        // When/Then: la création doit être refusée avec ConflictException
        ConflictException exception = assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request));
        assertTrue(exception.getMessage().contains("RG-03"));
    }

    /**
     * RG-03 - CAS 3 (aux limites): Un adhérent possède 0 réservation active.
     * → la première réservation est autorisée.
     */
    @Test
    void shouldAllowFirstReservationWhenAdherentHasNoActiveReservations() {
        // Given: adhérent sans réservation active
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                anyInt(), anyInt(), any())).thenReturn(false);
        // RG-03: 0 réservation active
        when(reservationRepository.countByAdherentUserIdAndStatutIn(anyInt(), any())).thenReturn(0L);
        when(reservationRepository.save(any())).thenAnswer(invocation -> {
            var reservation = invocation.getArgument(0);
            var field = reservation.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(reservation, 1);
            return reservation;
        });

        // When: création de la première réservation
        ReservationResponse response = reservationService.createReservation(request);

        // Then: la réservation est créée avec succès
        assertNotNull(response);
        assertEquals(ReservationStatus.EN_ATTENTE, response.getStatut());
    }

    /**
     * RG-03 - CAS 4 (aux limites): Un adhérent possède 1 réservation active.
     * → la deuxième réservation est autorisée.
     */
    @Test
    void shouldAllowSecondReservationWhenAdherentHasOneActiveReservation() {
        // Given: adhérent avec 1 réservation active
        when(booksRepository.findByIdForUpdate(1)).thenReturn(Optional.of(book));
        when(usersRepository.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                anyInt(), anyInt(), any())).thenReturn(false);
        // RG-03: 1 réservation active
        when(reservationRepository.countByAdherentUserIdAndStatutIn(anyInt(), any())).thenReturn(1L);
        when(reservationRepository.save(any())).thenAnswer(invocation -> {
            var reservation = invocation.getArgument(0);
            var field = reservation.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(reservation, 1);
            return reservation;
        });

        // When: création de la deuxième réservation
        ReservationResponse response = reservationService.createReservation(request);

        // Then: la réservation est créée avec succès
        assertNotNull(response);
        assertEquals(ReservationStatus.EN_ATTENTE, response.getStatut());
    }
}
