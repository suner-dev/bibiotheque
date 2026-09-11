package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * DTO d'entrée pour la création d'une réservation.
 *
 * RS-04 : adherentId est optionnel dans le body.
 * - Pour un ADHERENT, l'identité est déduite du token JWT (adherentId ignoré).
 * - Pour un BIBLIOTHECAIRE, adherentId peut être fourni pour créer une réservation
 *   au nom d'un autre adhérent.
 */
@Data
@Schema(description = "DTO d'entrée pour la création d'une réservation")
public class ReservationRequest {

    @NotNull(message = "livreId is required")
    @Schema(description = "Identifiant du livre à réserver", example = "1")
    private Integer livreId;

    @Schema(description = "Identifiant de l'adhérent (optionnel: déduit du token pour les adhérents)", example = "1")
    private Integer adherentId;
}
