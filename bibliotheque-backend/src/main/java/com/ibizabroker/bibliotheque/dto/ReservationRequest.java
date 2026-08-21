package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "DTO d'entrée pour la création d'une réservation")
public class ReservationRequest {

    @NotNull(message = "livreId is required")
    @Schema(description = "Identifiant du livre à réserver", example = "1")
    private Integer livreId;

    @NotNull(message = "adherentId is required")
    @Schema(description = "Identifiant de l'adhérent effectuant la réservation", example = "1")
    private Integer adherentId;
}
