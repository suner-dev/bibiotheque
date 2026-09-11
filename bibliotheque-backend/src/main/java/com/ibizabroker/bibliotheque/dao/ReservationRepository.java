package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    List<Reservation> findByStatut(ReservationStatus statut);

    List<Reservation> findByAdherentUserId(Integer adherentId);

    List<Reservation> findByAdherentUserIdAndStatut(Integer adherentId, ReservationStatus statut);

    /** Réservations d'un adhérent sur un livre parmi des statuts donnés (RG-06 honorer). */
    List<Reservation> findByAdherentUserIdAndLivreBookIdAndStatutIn(Integer adherentId, Integer livreId, List<ReservationStatus> statuts);

    long countByAdherentUserIdAndStatutIn(Integer adherentId, List<ReservationStatus> statuts);

    boolean existsByAdherentUserIdAndLivreBookIdAndStatutIn(Integer adherentId, Integer livreId, List<ReservationStatus> statuts);

    /** Plus ancienne réservation d'un livre dans un statut donné (file d'attente FIFO). */
    Optional<Reservation> findFirstByLivreBookIdAndStatutOrderByDateReservationAsc(Integer livreId, ReservationStatus statut);

    /** Réservations dont la date d'expiration est passée parmi les statuts actifs. */
    List<Reservation> findByStatutInAndDateExpirationBefore(List<ReservationStatus> statuts, Date date);
}
