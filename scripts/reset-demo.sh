#!/usr/bin/env bash
# ============================================================
# Reinitialise la base a l'etat initial de la demo (data.sql) :
#  - 5 livres tous indisponibles (0 copie)
#  - 5 emprunts actifs detenus par a3 (return_date NULL)
#  - aucune reservation
# Usage : ./scripts/reset-demo.sh [host] [port]
# ============================================================
set -e

HOST="${1:-localhost}"
PORT="${2:-5433}"
export PGPASSWORD="${PGPASSWORD:-postgres}"

psql -h "$HOST" -p "$PORT" -U postgres -d bibliotheque << 'SQL'
-- Supprime les donnees de test creees pendant les demos
TRUNCATE reservation RESTART IDENTITY;
TRUNCATE borrow RESTART IDENTITY;
SQL

# Reinjecte l'etat initial (livres a 0 copie, emprunts d'a3, roles, users)
psql -h "$HOST" -p "$PORT" -U postgres -d bibliotheque -f "$(dirname "$0")/data.sql" > /dev/null

echo "Base reinitialisee : 5 livres (0 copie), 5 emprunts actifs (a3), 0 reservation."
echo "Verifie l'etat :"
psql -h "$HOST" -p "$PORT" -U postgres -d bibliotheque -c \
  "SELECT (SELECT count(*) FROM books) AS livres, (SELECT count(*) FROM borrow) AS emprunts, (SELECT count(*) FROM reservation) AS reservations;"
