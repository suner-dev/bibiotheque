package com.ibizabroker.bibliotheque.dto;

import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "DTO de sortie représentant une réservation")
public class ReservationResponse {

    @Schema(description = "Identifiant de la réservation", example = "1")
    private Integer id;

    @Schema(description = "Identifiant du livre réservé", example = "1")
    private Integer livreId;

    @Schema(description = "Nom du livre réservé", example = "Le Petit Prince")
    private String livreName;

    @Schema(description = "Identifiant de l'adhérent", example = "1")
    private Integer adherentId;

    @Schema(description = "Nom de l'adhérent", example = "Jean Dupont")
    private String adherentName;

    @Schema(description = "Date et heure de la réservation")
    private Date dateReservation;

    @Schema(description = "Date et heure d'expiration de la réservation (+7 jours)")
    private Date dateExpiration;

    @Schema(description = "Statut de la réservation", example = "EN_ATTENTE")
    private ReservationStatus statut;
}
