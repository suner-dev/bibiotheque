package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.*;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.ForbiddenException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de gestion des réservations.
 *
 * Implémente les règles de sécurité RS-01 à RS-05:
 * - RS-01: Authentification requise (géré par Spring Security)
 * - RS-02: Seul BIBLIOTHECAIRE peut supprimer (géré par @PreAuthorize)
 * - RS-03: ADHERENT ne peut voir/modifier que ses propres réservations
 * - RS-04: L'identité vient du token, pas du body
 * - RS-05: Filtrage côté backend (ADHERENT voit uniquement ses réservations)
 */
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

    /**
     * RS-05 : Retourne les réservations selon le rôle de l'utilisateur authentifié.
     * - BIBLIOTHECAIRE : toutes les réservations (avec filtres optionnels)
     * - ADHERENT : uniquement ses propres réservations
     */
    public List<ReservationResponse> getAllReservations(ReservationStatus statut, Integer adherentId,
                                                        CurrentUserService currentUserService) {
        // RG-06 : met à jour les réservations périmées avant toute lecture
        expirerReservationsPerimees();

        List<Reservation> reservations;

        // RS-05 : Filtrage côté backend selon le rôle
        if (currentUserService.isAdherent()) {
            // ADHERENT : voit uniquement ses propres réservations
            reservations = reservationRepository.findByAdherentUserId(currentUserService.getCurrentUserId());
        } else if (statut != null && adherentId != null) {
            reservations = reservationRepository.findByAdherentUserIdAndStatut(adherentId, statut);
        } else if (statut != null) {
            reservations = reservationRepository.findByStatut(statut);
        } else if (adherentId != null) {
            reservations = reservationRepository.findByAdherentUserId(adherentId);
        } else {
            // BIBLIOTHECAIRE sans filtre : toutes les réservations
            reservations = reservationRepository.findAll();
        }

        return reservations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * RS-03 : Retourne une réservation par son ID.
     * - BIBLIOTHECAIRE : peut voir n'importe quelle réservation
     * - ADHERENT : ne peut voir que ses propres réservations
     */
    public ReservationResponse getReservationById(Integer id, CurrentUserService currentUserService) {
        // RG-06 : met à jour les réservations périmées avant toute lecture
        expirerReservationsPerimees();

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation with id " + id + " does not exist."));

        // RS-03 : Vérification de propriété pour ADHERENT
        if (currentUserService.isAdherent() && !reservation.getAdherent().getUserId().equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("RS-03 : cette réservation ne vous appartient pas.");
        }

        return toResponse(reservation);
    }

    /**
     * RS-03 : Annule une réservation.
     * - BIBLIOTHECAIRE : peut annuler n'importe quelle réservation
     * - ADHERENT : ne peut annuler que ses propres réservations
     */
    @Transactional
    public ReservationResponse annulerReservation(Integer id, CurrentUserService currentUserService) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation with id " + id + " does not exist."));

        // RS-03 : Vérification de propriété pour ADHERENT
        if (currentUserService.isAdherent() && !reservation.getAdherent().getUserId().equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("RS-03 : cette réservation ne vous appartient pas.");
        }

        // RG-05: Can only cancel EN_ATTENTE or DISPONIBLE
        if (!ACTIVE_STATUSES.contains(reservation.getStatut())) {
            throw new ConflictException("RG-05 : cette réservation ne peut plus être annulée.");
        }

        reservation.setStatut(ReservationStatus.ANNULEE);
        Reservation updated = reservationRepository.save(reservation);
        return toResponse(updated);
    }

    /**
     * RS-02 : Supprime une réservation.
     * - BIBLIOTHECAIRE : peut supprimer n'importe quelle réservation
     * - ADHERENT : ne peut pas supprimer (bloqué par @PreAuthorize)
     */
    @Transactional
    public void deleteReservation(Integer id, CurrentUserService currentUserService) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation with id " + id + " does not exist."));
        // RS-02 : La vérification du rôle est faite par @PreAuthorize("hasRole('Admin')")
        reservationRepository.delete(reservation);
    }

    /**
     * RG-06 / cycle de vie : lorsqu'un exemplaire d'un livre redevient disponible
     * (retour d'emprunt), la plus ancienne réservation EN_ATTENTE de ce livre passe à DISPONIBLE.
     */
    @Transactional
    public void promouvoirProchaineReservation(Integer livreId) {
        reservationRepository.findFirstByLivreBookIdAndStatutOrderByDateReservationAsc(livreId, ReservationStatus.EN_ATTENTE)
                .ifPresent(reservation -> {
                    reservation.setStatut(ReservationStatus.DISPONIBLE);
                    reservationRepository.save(reservation);
                });
    }

    /**
     * RG-06 / cycle de vie : lorsqu'un adhérent emprunte effectivement le livre
     * qu'il avait réservé (statut DISPONIBLE pour son compte), la réservation passe à HONOREE.
     */
    @Transactional
    public void honorerReservationSiExistante(Integer livreId, Integer adherentId) {
        reservationRepository.findByAdherentUserIdAndStatut(adherentId, ReservationStatus.DISPONIBLE).stream()
                .filter(reservation -> reservation.getLivre().getBookId().equals(livreId))
                .findFirst()
                .ifPresent(reservation -> {
                    reservation.setStatut(ReservationStatus.HONOREE);
                    reservationRepository.save(reservation);
                });
    }

    /**
     * RG-06 / cycle de vie : les réservations actives dont la date d'expiration (dateReservation + 7 jours)
     * est dépassée passent à EXPIREE. Pour chaque livre concerné, la réservation suivante est promue.
     */
    @Transactional
    public int expirerReservationsPerimees() {
        Date now = new Date();
        List<Reservation> perimees = reservationRepository.findByStatutInAndDateExpirationBefore(ACTIVE_STATUSES, now);
        Set<Integer> livresConcernees = new HashSet<>();
        for (Reservation reservation : perimees) {
            reservation.setStatut(ReservationStatus.EXPIREE);
            reservationRepository.save(reservation);
            livresConcernees.add(reservation.getLivre().getBookId());
        }
        livresConcernees.forEach(this::promouvoirProchaineReservation);
        return perimees.size();
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
