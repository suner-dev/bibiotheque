package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "livre_id", nullable = false)
    private Books livre;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "adherent_id", nullable = false)
    private Users adherent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date dateReservation;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus statut;
}
