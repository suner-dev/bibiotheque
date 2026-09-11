# TABLEAU DE CONFORMITÉ - Sécurisation du module Réservation

## Résumé des règles de sécurité

| Règle | Description | Implémentation | Test | Résultat |
|-------|-------------|----------------|------|----------|
| **RS-01** | Authentification obligatoire | `/api/reservations/**` retiré de `permitAll()` dans `WebSecurityConfiguration` | `unauthenticatedRequestReturns401`, `invalidTokenReturns401`, `unauthenticatedPostReturns401`, `unauthenticatedDeleteReturns401`, `unauthenticatedPatchReturns401` | ✅ PASS |
| **RS-02** | Autorisation par rôle | `@PreAuthorize("hasRole('Admin')")` sur DELETE dans `ReservationController` | `adherentCannotDeleteReservation`, `deleteWithoutAdminRoleReturns403` | ✅ PASS |
| **RS-03** | Protection par propriété | Vérification `adherentId == currentUserId` dans `ReservationService` | `adherentCannotAccessAnotherAdherentsReservation` | ✅ PASS |
| **RS-04** | Identité depuis token | `adherentId` ignoré du body pour ADHERENT dans `ReservationController` | `adherentCannotCreateReservationForAnotherAdherent` | ✅ PASS |
| **RS-05** | Filtrage backend | `findByAdherentUserId(currentUserId)` pour ADHERENT dans `ReservationService` | `authenticatedAdherentCanAccessGetReservations` | ✅ PASS |
| **RG-03** | Limite 3 réservations actives | Vérification dans `ReservationService.createReservation` | `shouldAllowThirdActiveReservationWhenAdherentHasTwoActiveReservations`, `shouldRejectReservationWhenAdherentAlreadyHasThreeActiveReservations` | ✅ PASS |

## Tests unitaires RG-03 (Limite 3 réservations actives)

| Test | Scénario | Résultat attendu | Résultat |
|------|----------|------------------|----------|
| `shouldAllowThirdActiveReservationWhenAdherentHasTwoActiveReservations` | 2 actives → 3ème | Succès (201) | ✅ PASS |
| `shouldRejectReservationWhenAdherentAlreadyHasThreeActiveReservations` | 3 actives → 4ème | Conflit (409) | ✅ PASS |
| `shouldAllowFirstReservationWhenAdherentHasNoActiveReservations` | 0 active → 1ère | Succès (201) | ✅ PASS |
| `shouldAllowSecondReservationWhenAdherentHasOneActiveReservation` | 1 active → 2ème | Succès (201) | ✅ PASS |

## Tests d'intégration sécurité

| Test | Règle | Résultat |
|------|-------|----------|
| `unauthenticatedRequestReturns401` | RS-01 | ✅ PASS |
| `invalidTokenReturns401` | RS-01 | ✅ PASS |
| `unauthenticatedPostReturns401` | RS-01 | ✅ PASS |
| `unauthenticatedDeleteReturns401` | RS-01 | ✅ PASS |
| `unauthenticatedPatchReturns401` | RS-01 | ✅ PASS |
| `adherentCannotDeleteReservation` | RS-02 | ✅ PASS |
| `deleteWithoutAdminRoleReturns403` | RS-02 | ✅ PASS |
| `adherentCannotAccessAnotherAdherentsReservation` | RS-03 | ✅ PASS |
| `adherentCannotCreateReservationForAnotherAdherent` | RS-04 | ✅ PASS |
| `authenticatedAdherentCanAccessGetReservations` | RS-05 | ✅ PASS |
| `authenticatedBibliothecaireCanAccessGetReservations` | RS-05 | ✅ PASS |
| `authenticatedBibliothecaireCanDeleteReservation` | RS-02 | ✅ PASS |

## Fichiers modifiés

### Backend
| Fichier | Modification |
|---------|--------------|
| `WebSecurityConfiguration.java` | Retrait de `/api/reservations/**` de `permitAll()`, ajout `CustomAccessDeniedHandler` |
| `ReservationController.java` | Ajout `@PreAuthorize`, implémentation RS-04 |
| `ReservationService.java` | Ajout vérification propriété (RS-03), filtrage backend (RS-05) |
| `CurrentUserService.java` | **Nouveau** - Extraction identité depuis token |
| `CustomAccessDeniedHandler.java` | **Nouveau** - Gestion 403 JSON |
| `JwtAuthenticationEntryPoint.java` | Amélioration réponse 401 JSON |
| `ForbiddenException.java` | **Nouveau** - Exception métier 403 |
| `GlobalExceptionHandler.java` | Ajout gestion `ForbiddenException` |
| `ReservationRequest.java` | `adherentId` rendu optionnel |
| `OpenApiConfiguration.java` | Documentation JWT améliorée |

### Tests
| Fichier | Modification |
|---------|--------------|
| `ReservationServiceTest.java` | Ajout mock `CurrentUserService`, 4 tests RG-03 |
| `ReservationSecurityIntegrationTest.java` | **Nouveau** - 10 tests d'intégration sécurité |

### Frontend
| Fichier | Modification |
|---------|--------------|
| `create-reservation.component.ts` | RS-04 : adhérent masqué pour ADHERENT |
| `create-reservation.component.html` | RS-04 : champ adhérent conditionnel |
| `reservation-list.component.ts` | RS-02 : bouton supprimer masqué pour ADHERENT |
| `reservation-list.component.html` | RS-02 : bouton supprimer conditionnel |
| `auth.interceptor.ts` | Amélioration gestion 401/403 |
| `reservation-container.component.ts` | Messages d'erreur améliorés |

### Documentation
| Fichier | Modification |
|---------|--------------|
| `README.md` | Section sécurité ajoutée |
| `SOUTENANCE_SEANCE_4.md` | **Nouveau** - Document de présentation |

## Résultats des builds

| Composant | Commande | Résultat |
|-----------|----------|----------|
| Backend | `./mvnw compile` | ✅ PASS |
| Backend | `./mvnw test` (37 tests) | ✅ PASS |
| Frontend | `npx tsc --noEmit` | ✅ PASS |

## Scénario de démonstration

1. ✅ **Sans authentification** : `GET /api/reservations` → 401
2. ✅ **Connexion ADHERENT A** : `GET /api/reservations` → 200 (ses réservations uniquement)
3. ✅ **ADHERENT A accède à réservation de B** → 403
4. ✅ **ADHERENT A tente DELETE** → 403
5. ✅ **ADHERENT A envoie adherentId frauduleux** → ignoré, identité du token utilisée
6. ✅ **Connexion BIBLIOTHECAIRE** : `GET /api/reservations` → 200 (toutes)
7. ✅ **BIBLIOTHECAIRE supprime** → 204
