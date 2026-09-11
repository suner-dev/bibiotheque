#!/usr/bin/env bash
# ============================================================
# start.sh — Lance la bibliothèque en local (backend + frontend)
# ============================================================
# Usage :
#   ./scripts/start.sh          Lance tout
#   ./scripts/start.sh --seed   Lance tout + injecte les données de test
#   ./scripts/start.sh --stop   Arrête les processus lancés
# ============================================================

set -euo pipefail

# ── Configuration ───────────────────────────────────────────
BACKEND_PORT=8087
FRONTEND_PORT=4200
PG_HOST=localhost
PG_PORT=5432
PG_USER=postgres
PG_DB=bibliotheque

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/bibliotheque-backend"
FRONTEND_DIR="$PROJECT_ROOT/bibliotheque-frontend"
LOG_DIR="$PROJECT_ROOT/logs"
PID_DIR="$PROJECT_ROOT/.pids"

# ── Couleurs ────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()   { echo -e "${RED}[ERR]${NC}   $*"; }

# ── Gestion de l'arrêt ─────────────────────────────────────
cleanup() {
    echo ""
    info "Arrêt des services..."
    for pidfile in "$PID_DIR"/*.pid; do
        [ -f "$pidfile" ] || continue
        pid=$(cat "$pidfile")
        name=$(basename "$pidfile" .pid)
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null && ok "$name (PID $pid) arrêté"
        fi
        rm -f "$pidfile"
    done
    info "Terminé."
}
trap cleanup EXIT INT TERM

stop_all() {
    info "Arrêt de tous les processus..."
    if [ -d "$PID_DIR" ]; then
        for pidfile in "$PID_DIR"/*.pid; do
            [ -f "$pidfile" ] || continue
            pid=$(cat "$pidfile")
            name=$(basename "$pidfile" .pid)
            if kill -0 "$pid" 2>/dev/null; then
                kill "$pid" 2>/dev/null && ok "$name (PID $pid) arrêté"
            fi
            rm -f "$pidfile"
        done
    fi
    # Nettoyage des processus orphelins
    pkill -f "bibliotheque-0.0.1-SNAPSHOT.jar" 2>/dev/null && ok "Backend orphelin arrêté" || true
    pkill -f "ng serve" 2>/dev/null && ok "Frontend orphelin arrêté" || true
    ok "Tous les processus arrêtés."
    exit 0
}

# ── Vérification des prérequis ──────────────────────────────
check_prereqs() {
    info "Vérification des prérequis..."
    local ok=true

    command -v java   >/dev/null 2>&1 || { err "Java non trouvé. Installez JDK 17+."; ok=false; }
    command -v mvn    >/dev/null 2>&1 || { err "Maven non trouvé."; ok=false; }
    command -v node   >/dev/null 2>&1 || { err "Node.js non trouvé."; ok=false; }
    command -v npm    >/dev/null 2>&1 || { err "npm non trouvé."; ok=false; }
    command -v psql   >/dev/null 2>&1 || { warn "psql non trouvé — vérification PostgreSQL impossible."; }

    $ok || exit 1
    ok "Prérequis OK (Java $(java -version 2>&1 | head -1 | cut -d'"' -f2), Node $(node -v))"
}

# ── Vérification PostgreSQL ─────────────────────────────────
check_postgres() {
    info "Vérification de PostgreSQL..."
    if command -v pg_isready >/dev/null 2>&1; then
        if pg_isready -h "$PG_HOST" -p "$PG_PORT" >/dev/null 2>&1; then
            ok "PostgreSQL accessible sur $PG_HOST:$PG_PORT"
            # Vérifier que la base existe
            if PGPASSWORD=postgres psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB" -c "SELECT 1" >/dev/null 2>&1; then
                ok "Base '$PG_DB' accessible"
            else
                warn "Base '$PG_DB' introuvable. Créez-la avec :"
                echo "    sudo -u postgres createdb -O postgres $PG_DB"
                exit 1
            fi
        else
            err "PostgreSQL n'est pas démarré sur $PG_HOST:$PG_PORT"
            echo "    Démarrez-le avec : sudo systemctl start postgresql"
            exit 1
        fi
    else
        warn "pg_isready non trouvé, on suppose que PostgreSQL tourne."
    fi
}

# ── Injection des données de test ───────────────────────────
seed_data() {
    info "Injection des données de test (scripts/data.sql)..."
    if PGPASSWORD=postgres psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB" \
        -f "$PROJECT_ROOT/scripts/data.sql" 2>&1; then
        ok "Données injectées avec succès"
    else
        warn "Erreur lors de l'injection des données (peut-être déjà présentes)"
    fi
}

# ── Construction du backend ─────────────────────────────────
build_backend() {
    info "Construction du backend (mvn package)..."
    cd "$BACKEND_DIR"
    if mvn package -DskipTests -q 2>&1; then
        ok "Backend compilé avec succès"
    else
        err "Échec de la compilation backend"
        exit 1
    fi
}

# ── Démarrage du backend ────────────────────────────────────
start_backend() {
    info "Démarrage du backend sur le port $BACKEND_PORT..."
    mkdir -p "$LOG_DIR" "$PID_DIR"

    cd "$BACKEND_DIR"
    java -jar target/bibliotheque-0.0.1-SNAPSHOT.jar \
        --spring.datasource.url="jdbc:postgresql://$PG_HOST:$PG_PORT/$PG_DB" \
        --spring.datasource.username="$PG_USER" \
        --spring.datasource.password="postgres" \
        > "$LOG_DIR/backend.log" 2>&1 &

    echo $! > "$PID_DIR/backend.pid"
    ok "Backend lancé (PID $(cat "$PID_DIR/backend.pid"))"

    # Attendre que le backend soit prêt
    info "Attente du démarrage du backend..."
    for i in $(seq 1 30); do
        if curl -s "http://localhost:$BACKEND_PORT/admin/books" >/dev/null 2>&1; then
            ok "Backend prêt sur http://localhost:$BACKEND_PORT"
            return 0
        fi
        sleep 1
    done
    err "Backend n'a pas démarré en 30s. Voir $LOG_DIR/backend.log"
    exit 1
}

# ── Installation des dépendances frontend ───────────────────
install_frontend() {
    if [ -d "$FRONTEND_DIR/node_modules" ]; then
        info "node_modules déjà installé, on saute npm install"
    else
        info "Installation des dépendances frontend..."
        cd "$FRONTEND_DIR"
        npm install --silent 2>&1
        ok "Dépendances frontend installées"
    fi
}

# ── Démarrage du frontend ───────────────────────────────────
start_frontend() {
    info "Démarrage du frontend sur le port $FRONTEND_PORT..."
    mkdir -p "$LOG_DIR" "$PID_DIR"

    cd "$FRONTEND_DIR"
    npx ng serve --host 0.0.0.0 --port "$FRONTEND_PORT" \
        > "$LOG_DIR/frontend.log" 2>&1 &

    echo $! > "$PID_DIR/frontend.pid"
    ok "Frontend lancé (PID $(cat "$PID_DIR/frontend.pid"))"

    # Attendre que le frontend soit prêt
    info "Attente du démarrage du frontend..."
    for i in $(seq 1 60); do
        if curl -s "http://localhost:$FRONTEND_PORT" >/dev/null 2>&1; then
            ok "Frontend prêt sur http://localhost:$FRONTEND_PORT"
            return 0
        fi
        sleep 1
    done
    err "Frontend n'a pas démarré en 60s. Voir $LOG_DIR/frontend.log"
    exit 1
}

# ── Point d'entrée ──────────────────────────────────────────
case "${1:-}" in
    --stop)
        stop_all
        ;;
    --seed)
        SEED=1
        ;;
    --help|-h)
        echo "Usage: $0 [--seed] [--stop] [--help]"
        echo ""
        echo "Options :"
        echo "  --seed   Injecte les données de test (scripts/data.sql) avant le lancement"
        echo "  --stop   Arrête les processus backend et frontend"
        echo "  --help   Affiche cette aide"
        echo ""
        echo "Lancement par défaut :"
        echo "  Backend  → http://localhost:$BACKEND_PORT"
        echo "  Frontend → http://localhost:$FRONTEND_PORT"
        echo "  Swagger  → http://localhost:$BACKEND_PORT/swagger-ui.html"
        echo ""
        echo "Comptes de test :"
        echo "  a1 / password (Admin)"
        echo "  a2 / password (User)"
        echo "  a3 / password (User)"
        exit 0
        ;;
esac

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   📚  BIBLIOTHÈQUE — Lancement local    ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════╝${NC}"
echo ""

check_prereqs
check_postgres

if [ "${SEED:-0}" = "1" ]; then
    seed_data
fi

build_backend
start_backend
install_frontend
start_frontend

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          ✅  TOUT EST PRÊT !            ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  Frontend : http://localhost:$FRONTEND_PORT       ║${NC}"
echo -e "${GREEN}║  Backend  : http://localhost:$BACKEND_PORT        ║${NC}"
echo -e "${GREEN}║  Swagger  : http://localhost:$BACKEND_PORT/swagger-ui.html ║${NC}"
echo -e "${GREEN}║                                          ║${NC}"
echo -e "${GREEN}║  Comptes :                               ║${NC}"
echo -e "${GREEN}║    a1 / password  (Admin)                ║${NC}"
echo -e "${GREEN}║    a2 / password  (User)                 ║${NC}"
echo -e "${GREEN}║    a3 / password  (User)                 ║${NC}"
echo -e "${GREEN}║                                          ║${NC}"
echo -e "${GREEN}║  Logs : logs/backend.log                 ║${NC}"
echo -e "${GREEN}║         logs/frontend.log                ║${NC}"
echo -e "${GREEN}║                                          ║${NC}"
echo -e "${GREEN}║  Ctrl+C pour arrêter                     ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════╝${NC}"
echo ""

# Garder le script vivant et attendre les signaux
wait
