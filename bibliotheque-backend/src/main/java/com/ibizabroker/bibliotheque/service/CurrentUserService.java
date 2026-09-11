package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Users;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service permettant d'obtenir l'utilisateur authentifié depuis le SecurityContext.
 * L'identité provient TOUJOURS du token JWT, jamais du corps de la requête.
 *
 * Implémente RS-04 : l'identité de l'utilisateur authentifié est récupérée
 * depuis Spring Security, pas depuis le body de la requête.
 */
@Service
public class CurrentUserService {

    private final UsersRepository usersRepository;

    public CurrentUserService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    /**
     * Retourne le username (subject du JWT) de l'utilisateur authentifié.
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    /**
     * Retourne l'entité Users de l'utilisateur authentifié.
     */
    public Users getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        return usersRepository.findByUsername(username).orElse(null);
    }

    /**
     * Retourne l'ID de l'utilisateur authentifié.
     */
    public Integer getCurrentUserId() {
        Users user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * Vérifie si l'utilisateur authentifié a le rôle BIBLIOTHECAIRE.
     */
    public boolean isBibliothecaire() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_Admin"));
    }

    /**
     * Vérifie si l'utilisateur authentifié a le rôle ADHERENT.
     */
    public boolean isAdherent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_User"));
    }

    /**
     * Retourne true si l'utilisateur authentifié peut agir sur les réservations
     * de n'importe quel adhérent (bibliothécaire).
     */
    public boolean canAccessAllReservations() {
        return isBibliothecaire();
    }
}
