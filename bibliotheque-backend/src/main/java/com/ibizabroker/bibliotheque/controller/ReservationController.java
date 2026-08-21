package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Gestion des réservations de livres")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Créer une réservation", description = "Crée une nouvelle réservation pour un livre indisponible. RG-01, RG-02, RG-03 sont appliquées.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Réservation créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide (champs manquants)"),
            @ApiResponse(responseCode = "404", description = "Livre ou adhérent introuvable"),
            @ApiResponse(responseCode = "409", description = "Conflit métier (RG-01, RG-02, RG-03)")
    })
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lister les réservations", description = "Retourne la liste des réservations avec filtres optionnels par statut et/ou adhérent")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
            @ApiResponse(responseCode = "400", description = "Statut de filtre invalide")
    })
    public ResponseEntity<List<ReservationResponse>> getAllReservations(
            @Parameter(description = "Filtrer par statut") @RequestParam(required = false) ReservationStatus statut,
            @Parameter(description = "Filtrer par identifiant adhérent") @RequestParam(name = "adherentId", required = false) Integer adherentId) {
        List<ReservationResponse> reservations = reservationService.getAllReservations(statut, adherentId);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une réservation", description = "Retourne les détails d'une réservation par son identifiant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<ReservationResponse> getReservationById(
            @Parameter(description = "Identifiant de la réservation") @PathVariable Integer id) {
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/annuler")
    @Operation(summary = "Annuler une réservation", description = "Annule une réservation active (EN_ATTENTE ou DISPONIBLE). RG-05 est appliquée.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réservation annulée avec succès"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable"),
            @ApiResponse(responseCode = "409", description = "Transition interdite (RG-05)")
    })
    public ResponseEntity<ReservationResponse> annulerReservation(
            @Parameter(description = "Identifiant de la réservation") @PathVariable Integer id) {
        ReservationResponse response = reservationService.annulerReservation(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une réservation", description = "Supprime définitivement une réservation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Réservation supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<Void> deleteReservation(
            @Parameter(description = "Identifiant de la réservation") @PathVariable Integer id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
