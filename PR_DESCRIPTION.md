# PR — Écran de gestion des réservations (Séance 3)

**Branche :** `feature/reservation-ui-prenom-nom`

## Description

Écran unique de gestion des réservations (`/reservations`, accessible depuis la navigation pour le rôle Admin), consommant notre propre backend **uniquement via les services Angular** (aucun HttpClient/fetch dans les composants).

### Contenu

1. **Liste** : colonnes livre, adhérent, statut (badge coloré), date de réservation, date d'expiration, actions. Filtre par statut : Tous / EN_ATTENTE / DISPONIBLE / ANNULEE / EXPIREE / HONOREE (branché sur `GET /api/reservations?statut=`).
2. **Formulaire de création** : deux listes déroulantes alimentées par `GET /admin/books` et `GET /admin/users`. Bouton inactif tant que les deux champs ne sont pas renseignés. Après succès, la liste se rafraîchit sans rechargement.
3. **Annulation** : bouton visible uniquement pour EN_ATTENTE/DISPONIBLE, confirmation avant `PATCH /api/reservations/{id}/annuler`, mise à jour de la liste, message serveur affiché en cas de 409.
4. **Détails** : consomme `GET /api/reservations/{id}` et affiche un panneau (avec son propre état de chargement/erreur + Réessayer).
5. **Suppression** : consomme `DELETE /api/reservations/{id}` avec confirmation.

### Les quatre états

- **Chargement** : spinner visible pendant chaque appel.
- **Données** : tableau rempli depuis l'API.
- **Liste vide** : message explicite « Aucune réservation ».
- **Erreur** : message compréhensible (« serveur injoignable » si statut 0) + bouton **Réessayer**.

### Traitement des refus métier (messages réels du serveur, affichés à côté du formulaire)

| Situation | Code | Affichage |
|-----------|------|-----------|
| Livre disponible | 409 | « RG-01 : impossible de réserver un livre disponible. » |
| Réservation déjà active sur ce livre | 409 | « RG-02 : ... » |
| Quota de 3 atteint | 409 | « RG-03 : ... » |
| Champ manquant | 400 | Message de validation serveur |
| Livre/adhérent inexistant | 404 | « Livre ou adhérent introuvable » adapté |

### Compléments backend (RG-06, cycle de vie complet)

- Retour d'un livre (`PUT /borrow`) → la plus ancienne réservation `EN_ATTENTE` du livre passe à **DISPONIBLE**.
- Emprunt du livre réservé → la réservation passe à **HONOREE**.
- Expiration (dateReservation + 7 jours dépassés) → **EXPIREE** (tâche planifiée + vérification à la lecture).
- Le quota RG-03 ne compte que les réservations actives (EN_ATTENTE/DISPONIBLE).

## Captures d'écran

1. **État de chargement** : ![chargement](captures/01-chargement.png) — spinner « Chargement des réservations... » pendant l'appel (requête API retardée pour le figer).
2. **Liste remplie** : ![donnees](captures/02-liste-remplie.png) — tableau + badges de statut.
3. **Liste vide** : ![vide](captures/03-liste-vide.png) — message explicite « Aucune réservation ».
4. **Refus 409** : ![409](captures/04-refus-409.png) — message métier du serveur affiché à côté du formulaire.
5. **Swagger UI (vue globale)** : ![swagger](captures/05-swagger-ui.png) — documentation interactive, bouton Authorize.
6. **Swagger UI (endpoints réservations)** : ![swagger-reservations](captures/06-swagger-reservations.png) — `/api/reservations` + `/annuler`.

## Tests

- Backend : `mvn test` — tous verts (RG-01 à RG-05, dates +7j, transitions nouvelles).
- Matrice API vérifiée en live : 201/200/204/400/404/409 sur tous les endpoints.
- Parcours Full Stack vérifié : UI → service → HTTP → controller → service métier → repository → PostgreSQL.
