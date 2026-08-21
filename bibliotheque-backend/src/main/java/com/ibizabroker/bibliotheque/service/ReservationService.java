package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.*;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final int MAX_ACTIVE_RESERVATIONS = 3;
    private static final int RESERVATION_DURATION_DAYS = 7;
    private static final List<ReservationStatus> ACTIVE_STATUSES = Arrays.asList(
            ReservationStatus.EN_ATTENTE,
            ReservationStatus.DISPONIBLE
    );
    private static final List<ReservationStatus> TERMINAL_STATUSES = Arrays.asList(
            ReservationStatus.ANNULEE,
            ReservationStatus.EXPIREE,
            ReservationStatus.HONOREE
    );

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        // Validate inputs first
        if (request.getLivreId() == null) {
            throw new IllegalArgumentException("livreId is required");
        }
        if (request.getAdherentId() == null) {
            throw new IllegalArgumentException("adherentId is required");
        }

        // Lock the book row first, then the adherent row (consistent order prevents deadlocks).
        // Pessimistic locks serialize concurrent creations so RG-01/RG-02/RG-03 cannot be raced.
        Books livre = booksRepository.findByIdForUpdate(request.getLivreId())
                .orElseThrow(() -> new NotFoundException("Book with id " + request.getLivreId() + " does not exist."));

        Users adherent = usersRepository.findByIdForUpdate(request.getAdherentId())
                .orElseThrow(() -> new NotFoundException("User with id " + request.getAdherentId() + " does not exist."));

        // RG-01: Book must be unavailable
        if (isBookAvailable(livre)) {
            throw new ConflictException("RG-01 : impossible de réserver un livre disponible.");
        }

        // RG-02: No duplicate active reservation
        if (reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                adherent.getUserId(), livre.getBookId(), ACTIVE_STATUSES)) {
            throw new ConflictException("RG-02 : cet adhérent possède déjà une réservation active pour ce livre.");
        }

        // RG-03: Max 3 active reservations
        long activeCount = reservationRepository.countByAdherentUserIdAndStatutIn(
                adherent.getUserId(), ACTIVE_STATUSES);
        if (activeCount >= MAX_ACTIVE_RESERVATIONS) {
            throw new ConflictException("RG-03 : un adhérent ne peut pas avoir plus de 3 réservations actives.");
        }

        // RG-04: Server-generated dates
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DATE, RESERVATION_DURATION_DAYS);
        Date expiration = cal.getTime();

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(now);
        reservation.setDateExpiration(expiration);
        reservation.setStatut(ReservationStatus.EN_ATTENTE);

        Reservation saved = reservationRepository.save(reservation);
        return toResponse(saved);
    }

    public List<ReservationResponse> getAllReservations(ReservationStatus statut, Integer adherentId) {
        List<Reservation> reservations;

        if (statut != null && adherentId != null) {
            reservations = reservationRepository.findByAdherentUserIdAndStatut(adherentId, statut);
        } else if (statut != null) {
            reservations = reservationRepository.findByStatut(statut);
        } else if (adherentId != null) {
            reservations = reservationRepository.findByAdherentUserId(adherentId);
        } else {
            reservations = reservationRepository.findAll();
        }

        return reservations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReservationResponse getReservationById(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation with id " + id + " does not exist."));
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse annulerReservation(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation with id " + id + " does not exist."));

        // RG-05: Can only cancel EN_ATTENTE or DISPONIBLE
        if (!ACTIVE_STATUSES.contains(reservation.getStatut())) {
            throw new ConflictException("RG-05 : cette réservation ne peut plus être annulée.");
        }

        reservation.setStatut(ReservationStatus.ANNULEE);
        Reservation updated = reservationRepository.save(reservation);
        return toResponse(updated);
    }

    @Transactional
    public void deleteReservation(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation with id " + id + " does not exist."));
        reservationRepository.delete(reservation);
    }

    private boolean isBookAvailable(Books book) {
        // A book is available only when it has a positive number of copies (null-safe).
        return book.getNoOfCopies() != null && book.getNoOfCopies() > 0;
    }

    private ReservationResponse toResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setLivreId(reservation.getLivre().getBookId());
        response.setLivreName(reservation.getLivre().getBookName());
        response.setAdherentId(reservation.getAdherent().getUserId());
        response.setAdherentName(reservation.getAdherent().getName());
        response.setDateReservation(reservation.getDateReservation());
        response.setDateExpiration(reservation.getDateExpiration());
        response.setStatut(reservation.getStatut());
        return response;
    }
}
