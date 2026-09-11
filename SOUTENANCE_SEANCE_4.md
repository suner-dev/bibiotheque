# SOUTENANCE SÉANCE 4 - Sécurisation du module Réservation

## 1. Architecture de sécurité

L'architecture de sécurité mise en place suit le modèle suivant :

```
FRONTEND (Angular)
   ↓
Login → JWT (stocké dans localStorage)
   ↓
Authorization: Bearer <token>
   ↓
SPRING SECURITY (JwtRequestFilter)
   ↓
Authentification (vérification token JWT)
   ↓
Autorisation (@PreAuthorize sur les contrôleurs)
   ↓
CONTROLLER (ReservationController)
   ↓
SERVICE (ReservationService + CurrentUserService)
   ↓
VÉRIFICATION DE PROPRIÉTÉ (RS-03)
   ↓
REPOSITORY (filtrage par adherentId)
   ↓
POSTGRESQL
```

**Point clé** : L'identité de l'utilisateur provient TOUJOURS du token JWT (via `CurrentUserService`), JAMAIS du corps de la requête.

## 2. Comment fonctionne JWT

1. **Login** : `POST /authenticate` avec username/password → retourne `{ user, jwtToken }`
2. **Stockage** : Le token est stocké dans `localStorage` côté frontend
3. **Envoi** : Chaque requête inclut `Authorization: Bearer <token>`
4. **Validation** : `JwtRequestFilter` extrait et valide le token
5. **Extraction** : Le username est extrait du token et utilisé pour charger les droits

## 3. Pourquoi 401 et 403 sont différents

| Code | Signification | Cas d'usage |
|------|---------------|-------------|
| **401 Unauthorized** | "Je ne sais pas qui vous êtes" | Token absent, invalide, expiré ou malformé |
| **403 Forbidden** | "Je sais qui vous êtes, mais vous n'avez pas le droit" | Rôle insuffisant OU ressource appartenant à un autre utilisateur |

**Exemple concret** :
- Un utilisateur non connecté appelle `GET /api/reservations` → **401**
- Un ADHERENT appelle `DELETE /api/reservations/1` → **403** (il est authentifié mais n'a pas le rôle Admin)
- Un ADHERENT A appelle `GET /api/reservations/10` (appartenant à B) → **403** (il est authentifié mais ce n'est pas sa réservation)

## 4. Comment RS-03 est sécurisé

**RS-03** : Un ADHERENT ne peut consulter/modifier que ses propres réservations.

**Implémentation** :
```java
// Dans ReservationService
public ReservationResponse getReservationById(Integer id, CurrentUserService currentUserService) {
    Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("..."));

    // Vérification de propriété pour ADHERENT
    if (currentUserService.isAdherent() &&
        !reservation.getAdherent().getUserId().equals(currentUserService.getCurrentUserId())) {
        throw new ForbiddenException("RS-03 : cette réservation ne vous appartient pas.");
    }

    return toResponse(reservation);
}
```

**Tests** :
- `adherentCannotAccessAnotherAdherentsReservation` → vérifie le 403

## 5. Comment RS-04 est sécurisé

**RS-04** : L'identité de l'adhérent provient du token, pas du body.

**Problème** : Un ADHERENT malveillant pourrait envoyer :
```json
{
    "livreId": 12,
    "adherentId": 999  // ID d'un autre utilisateur
}
```

**Solution** :
```java
// Dans ReservationController
@PostMapping
@PreAuthorize("hasAnyRole('Admin', 'User')")
public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
    // RS-04 : Pour un ADHERENT, on ignore adherentId du body
    if (currentUserService.isAdherent()) {
        request.setAdherentId(currentUserService.getCurrentUserId());
    }
    // ...
}
```

**Double protection** :
1. Frontend : Le champ adhérent est masqué pour les ADHERENT
2. Backend : Même si le JSON est modifié, l'identité du token est utilisée

## 6. Pourquoi le frontend seul ne suffit pas pour sécuriser une API

**Principe fondamental** : Le frontend ne doit JAMAIS être considéré comme une couche de sécurité.

**Raisons** :
1. **Modification du JSON** : Un utilisateur peut modifier le corps de la requête avec Postman, curl, ou les devtools
2. **Modification du token** : Un utilisateur peut essayer de décoder/modifier le JWT
3. **Désactivation du JavaScript** : Le frontend peut être contourné
4. **Accès direct à l'API** : L'API peut être appelée depuis n'importe quel client

**Conséquence** : Toutes les vérifications de sécurité doivent être faites côté backend :
- Authentification (RS-01)
- Autorisation par rôle (RS-02)
- Vérification de propriété (RS-03)
- Identité depuis token (RS-04)
- Filtrage des données (RS-05)

## 7. Comment RG-03 est testé

**RG-03** : Un adhérent ne peut pas avoir plus de 3 réservations actives.

**Tests unitaires** (avec repository mocké) :

| Test | Description | Résultat attendu |
|------|-------------|------------------|
| `shouldAllowThirdActiveReservationWhenAdherentHasTwoActiveReservations` | 2 actives → 3ème | Succès (201) |
| `shouldRejectReservationWhenAdherentAlreadyHasThreeActiveReservations` | 3 actives → 4ème | Conflit (409) |
| `shouldAllowFirstReservationWhenAdherentHasNoActiveReservations` | 0 active → 1ère | Succès (201) |
| `shouldAllowSecondReservationWhenAdherentHasOneActiveReservation` | 1 active → 2ème | Succès (201) |

**Pourquoi c'est un test unitaire ?**
- Le repository est mocké (pas de vraie base de données)
- On teste uniquement la logique métier
- Exécution rapide

## 8. Scénario de démonstration en 8 minutes

### Minute 1-2 : Introduction
- Présenter l'architecture de sécurité
- Expliquer la matrice des autorisations

### Minute 3 : Démonstration RS-01
```bash
# Sans token
curl http://localhost:8087/api/reservations
# → 401 Unauthorized
```
**Explication** : "Le système ne sait pas qui je suis."

### Minute 4 : Démonstration RS-05
```bash
# Avec token ADHERENT
curl -H "Authorization: Bearer <tokenA>" http://localhost:8087/api/reservations
# → 200 OK (uniquement les réservations de A)
```
**Explication** : "Le filtrage est fait côté backend."

### Minute 5 : Démonstration RS-03
```bash
# ADHERENT A tente d'accéder à une réservation de B
curl -H "Authorization: Bearer <tokenA>" http://localhost:8087/api/reservations/<idDeB>
# → 403 Forbidden
```
**Explication** : "Le backend connaît mon identité, mais cette réservation ne m'appartient pas."

### Minute 6 : Démonstration RS-04
```bash
# ADHERENT A envoie adherentId frauduleux
curl -X POST -H "Authorization: Bearer <tokenA>" \
  -d '{"livreId": 12, "adherentId": 999}' \
  http://localhost:8087/api/reservations
# → La réservation est créée pour A (pas pour 999)
```
**Explication** : "L'identité vient du token, pas du JSON."

### Minute 7 : Démonstration RS-02
```bash
# ADHERENT tente DELETE
curl -X DELETE -H "Authorization: Bearer <tokenA>" \
  http://localhost:8087/api/reservations/1
# → 403 Forbidden

# BIBLIOTHECAIRE tente DELETE
curl -X DELETE -H "Authorization: Bearer <tokenAdmin>" \
  http://localhost:8087/api/reservations/1
# → 204 No Content
```
**Explication** : "Seul le bibliothécaire peut supprimer."

### Minute 8 : Conclusion
- Récapitulatif des règles de sécurité
- Importance de la vérification côté backend
- Questions

---

## Questions que l'examinateur pourrait poser

1. **Pourquoi 401 ici ?** → Token absent/invalide, le système ne connaît pas l'utilisateur
2. **Pourquoi 403 ici ?** → L'utilisateur est authentifié mais n'a pas les droits
3. **Où récupérez-vous l'identité ?** → Dans `CurrentUserService` depuis `SecurityContextHolder`
4. **Pourquoi ne faites-vous pas confiance à adherentId ?** → Il peut être modifié dans le JSON
5. **Où est vérifié le propriétaire ?** → Dans `ReservationService` (RS-03)
6. **Pourquoi filtrer dans le backend ?** → Pour ne pas envoyer de données inutiles et sensibles
7. **Pourquoi le frontend seul ne suffit-il pas ?** → Le frontend peut être contourné
8. **Où est vérifié le rôle ?** → Dans `@PreAuthorize` et `CurrentUserService.isAdherent()`
9. **Pourquoi le test RG-03 est-il unitaire ?** → Repository mocké, test rapide de la logique métier
10. **Pourquoi mocker le repository ?** → Pour isoler la logique métier de la base de données
11. **Pourquoi le test de sécurité est-il d'intégration ?** → Pour tester la vraie chaîne HTTP/Security
12. **Comment prouvez-vous qu'un adhérent ne peut pas supprimer ?** → Test `adherentCannotDeleteReservation`
13. **Comment prouvez-vous qu'un bibliothécaire peut agir sur toutes les réservations ?** → Test `authenticatedBibliothecaireCanDeleteReservation`
14. **Que se passe-t-il lorsque le JWT expire ?** → 401, nettoyage session, redirection login
15. **Que se passe-t-il si quelqu'un modifie le JSON ?** → Le backend vérifie l'identité du token
