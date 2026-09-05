-- ============================================================
-- JEU DE DONNÉES DE TEST - ÉPREUVE KFOKAM48 (Modifié : L1 emprunté)
-- Réservation
-- PostgreSQL
-- ============================================================


-- ------------------------------------------------------------
-- 1. LIVRES
--  L1-L4 : indisponibles (0 exemplaire) -> réservables (RG-01)
--  L5    : DISPONIBLE (2 exemplaires)   -> empruntable / RG-01 si on le réserve
-- ------------------------------------------------------------

INSERT INTO books (
    book_id,
    book_name,
    book_author,
    book_genre,
    no_of_copies
)
VALUES
    (1001, 'L1', 'Auteur L1', 'Test', 0),
    (1002, 'L2', 'Auteur L2', 'Test', 0),
    (1003, 'L3', 'Auteur L3', 'Test', 0),
    (1004, 'L4', 'Auteur L4', 'Test', 0),
    (1005, 'L5', 'Auteur L5', 'Test', 2)
    ON CONFLICT (book_id)
DO UPDATE SET
    book_name = EXCLUDED.book_name,
           book_author = EXCLUDED.book_author,
           book_genre = EXCLUDED.book_genre,
           no_of_copies = EXCLUDED.no_of_copies;


-- ------------------------------------------------------------
-- 2. RÔLES
-- ------------------------------------------------------------

INSERT INTO role (role_id, role_name)
VALUES
    (1, 'Admin'),
    (2, 'User')
    ON CONFLICT (role_id)
DO UPDATE SET role_name = EXCLUDED.role_name;


-- ------------------------------------------------------------
-- 3. ADHÉRENTS (mots de passe hashés en BCrypt)
-- Mot de passe pour tous : password
-- ------------------------------------------------------------

INSERT INTO users (
    user_id,
    username,
    name,
    password
)
VALUES
    (2001, 'a1', 'A1', '$2b$12$2Mbx7uKt93nD31NsoadDBe9ZiD4/soAy0ENPeiuVb1SF8C13n0JRu'),
    (2002, 'a2', 'A2', '$2b$12$2Mbx7uKt93nD31NsoadDBe9ZiD4/soAy0ENPeiuVb1SF8C13n0JRu'),
    (2003, 'a3', 'A3', '$2b$12$2Mbx7uKt93nD31NsoadDBe9ZiD4/soAy0ENPeiuVb1SF8C13n0JRu')
    ON CONFLICT (user_id)
DO UPDATE SET
    username = EXCLUDED.username,
           name = EXCLUDED.name,
           password = EXCLUDED.password;


-- ------------------------------------------------------------
-- 4. ASSOCIATIONS UTILISATEUR ↔ RÔLE
-- ------------------------------------------------------------

INSERT INTO user_role (user_id, role_id)
VALUES
    (2001, 1),  -- a1 → Admin
    (2002, 2),  -- a2 → User
    (2003, 2)   -- a3 → User
    ON CONFLICT (user_id, role_id)
DO NOTHING;


-- ------------------------------------------------------------
-- 5. EMPRUNTS (historique d'emprunt)
--  A3 (2003) : L1 actif, L2 actif             -> L1, L2 indisponibles
--  A2 (2002) : L3 actif, L4 actif, + L5 rendu -> L3, L4 indisponibles, L5 rendu (dispo)
--  A2 (2002) : L1 déjà rendu (historique ancien)
-- ------------------------------------------------------------

INSERT INTO borrow (
    borrow_id,
    book_id,
    user_id,
    issue_date,
    return_date,
    due_date
)
VALUES
    (
        3000,
        1001,
        2003,
        CURRENT_TIMESTAMP - INTERVAL '4 days',
        NULL,
        CURRENT_TIMESTAMP + INTERVAL '10 days'
    ),
    (
        3001,
        1002,
        2003,
        CURRENT_TIMESTAMP - INTERVAL '3 days',
        NULL,
        CURRENT_TIMESTAMP + INTERVAL '11 days'
    ),
    (
        3002,
        1003,
        2002,
        CURRENT_TIMESTAMP - INTERVAL '3 days',
        NULL,
        CURRENT_TIMESTAMP + INTERVAL '11 days'
    ),
    (
        3003,
        1004,
        2002,
        CURRENT_TIMESTAMP - INTERVAL '2 days',
        NULL,
        CURRENT_TIMESTAMP + INTERVAL '12 days'
    ),
    (
        3004,
        1005,
        2002,
        CURRENT_TIMESTAMP - INTERVAL '20 days',
        CURRENT_TIMESTAMP - INTERVAL '15 days',
        CURRENT_TIMESTAMP - INTERVAL '13 days'
    ),
    (
        3005,
        1001,
        2002,
        CURRENT_TIMESTAMP - INTERVAL '40 days',
        CURRENT_TIMESTAMP - INTERVAL '33 days',
        CURRENT_TIMESTAMP - INTERVAL '31 days'
    )
    ON CONFLICT (borrow_id)
DO UPDATE SET
    book_id = EXCLUDED.book_id,
           user_id = EXCLUDED.user_id,
           issue_date = EXCLUDED.issue_date,
           return_date = EXCLUDED.return_date,
           due_date = EXCLUDED.due_date;