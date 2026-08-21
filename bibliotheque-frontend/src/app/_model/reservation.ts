export class Reservation {
    id: number;
    livreId: number;
    livreName: string;
    adherentId: number;
    adherentName: string;
    dateReservation: Date;
    dateExpiration: Date;
    statut: string;
}

export class ReservationRequest {
    livreId: number;
    adherentId: number;
}
