package com.ibizabroker.bibliotheque.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire des erreurs 403 (Forbidden).
 * Journalise les tentatives d'accès refusées pourPermission.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String message = "Vous n'avez pas les droits nécessaires pour effectuer cette action.";
        if (accessDeniedException != null && accessDeniedException.getMessage() != null) {
            message = accessDeniedException.getMessage();
        }

        log.warn("Tentative d'accès refusée [403] - URI: {}, Méthode: {}, Utilisateur: {}, Raison: {}",
                request.getRequestURI(),
                request.getMethod(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "non authentifié",
                message);

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "FORBIDDEN");
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
