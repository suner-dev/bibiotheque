package com.ibizabroker.bibliotheque.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lorsqu'un utilisateur authentifié tente d'accéder à une ressource
 * qui ne lui appartient pas ou pour laquelle il n'a pas les droits.
 *
 * RS-03 : ADHERENT tente d'accéder à une réservation d'un autre adhérent.
 * Retourne HTTP 403 Forbidden.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
