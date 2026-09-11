package com.ibizabroker.bibliotheque.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tâche planifiée : applique RG-06 en passant automatiquement à EXPIREE
 * les réservations actives dont la date d'expiration (dateReservation + 7 jours) est dépassée.
 */
@Component
public class ReservationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationScheduler.class);

    @Autowired
    private ReservationService reservationService;

    /** Toutes les 5 minutes. */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void expirerReservations() {
        int count = reservationService.expirerReservationsPerimees();
        if (count > 0) {
            log.info("{} réservation(s) expirée(s) automatiquement (RG-06).", count);
        }
    }
}