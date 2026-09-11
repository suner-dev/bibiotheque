package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.service.CurrentUserService;
import com.ibizabroker.bibliotheque.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Contrôleur REST pour la gestion des réservations.
 *
 * Matrice des autorisations (RS-01 à RS-05):
 *
 * | Endpoint | Anonyme | ADHERENT | BIBLIOTHECAIRE |
 * |----------|---------|----------|----------------|
 * | POST /api/reservations | NON | OUI, pour lui-même | OUI, pour tous |
 * | GET /api/reservations | NON | OUI, ses réservations | OUI, toutes |
 * | GET /api/reservations/{id} | NON | OUI, si propriétaire | OUI, toutes |
 * | PATCH /api/reservations/{id}/annuler | NON | OUI, si propriétaire | OUI, toutes |
 * | DELETE /api/reservations/{id} | NON | NON | OUI |
 *
 * RS-04 : L'identité de l'adhérent provient TOUJOURS du token JWT,
 *         jamais du corps de la requête pour un ADHERENT.
 */
@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Gestion des réservations de livres - Sécurisé par JWT")
public class ReservationController {

    private final ReservationService reservationService;
    private final CurrentUserService currentUserService;

    public ReservationController(ReservationService reservationService, CurrentUserService currentUserService) {
        this.reservationService = reservationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'User')")
    @Operation(summary = "Créer une réservation",
               description = "RS-01, RS-04: l'identité vient du token pour les adhérents.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Réservation créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Livre ou adhérent introuvable"),
            @ApiResponse(responseCode = "409", description = "Conflit métier (RG-01, RG-02, RG-03)")
    })
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request) {
        // RS-04 : Pour un ADHERENT, on ignore adherentId du body et on utilise l'identité du token
        if (currentUserService.isAdherent()) {
            request.setAdherentId(currentUserService.getCurrentUserId());
        }
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'User')")
    @Operation(summary = "Lister les réservations",
               description = "RS-05: ADHERENT voit ses réservations, BIBLIOTHECAIRE voit tout.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    public ResponseEntity<List<ReservationResponse>> getAllReservations(
            @Parameter(description = "Filtrer par statut") @RequestParam(required = false) ReservationStatus statut,
            @Parameter(description = "Filtrer par identifiant adhérent") @RequestParam(name = "adherentId", required = false) Integer adherentId) {
        List<ReservationResponse> reservations = reservationService.getAllReservations(statut, adherentId, currentUserService);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'User')")
    @Operation(summary = "Consulter une réservation",
               description = "RS-03: ADHERENT ne peut voir que ses propres réservations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Réservation appartenant à un autre adhérent"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<ReservationResponse> getReservationById(
            @Parameter(description = "Identifiant de la réservation") @PathVariable Integer id) {
        ReservationResponse response = reservationService.getReservationById(id, currentUserService);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('Admin', 'User')")
    @Operation(summary = "Annuler une réservation",
               description = "RS-03: ADHERENT ne peut annuler que ses propres réservations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réservation annulée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Réservation appartenant à un autre adhérent"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable"),
            @ApiResponse(responseCode = "409", description = "Transition interdite (RG-05)")
    })
    public ResponseEntity<ReservationResponse> annulerReservation(
            @Parameter(description = "Identifiant de la réservation") @PathVariable Integer id) {
        ReservationResponse response = reservationService.annulerReservation(id, currentUserService);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Supprimer une réservation",
               description = "RS-02: Seul le BIBLIOTHECAIRE peut supprimer une réservation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Réservation supprimée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Action réservée au BIBLIOTHECAIRE"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<Void> deleteReservation(
            @Parameter(description = "Identifiant de la réservation") @PathVariable Integer id) {
        reservationService.deleteReservation(id, currentUserService);
        return ResponseEntity.noContent().build();
    }
}
