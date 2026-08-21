-- ============================================================
-- JEU DE DONNÉES DE TEST - ÉPREUVE KFOKAM48
-- Réservation
-- ============================================================

-- ------------------------------------------------------------
-- 1. LIVRES
-- ------------------------------------------------------------
INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies)
VALUES
    (1001, 'L1', 'Auteur L1', 'Test', 1),
    (1002, 'L2', 'Auteur L2', 'Test', 0),
    (1003, 'L3', 'Auteur L3', 'Test', 0),
    (1004, 'L4', 'Auteur L4', 'Test', 0),
    (1005, 'L5', 'Auteur L5', 'Test', 0)
    ON DUPLICATE KEY UPDATE
                         book_name = VALUES(book_name),
                         book_author = VALUES(book_author),
                         book_genre = VALUES(book_genre),
                         no_of_copies = VALUES(no_of_copies);


-- ------------------------------------------------------------
-- 2. ADHÉRENTS
-- ------------------------------------------------------------
INSERT INTO users (user_id, username, name, password)
VALUES
    (2001, 'a1', 'A1', 'password'),
    (2002, 'a2', 'A2', 'password'),
    (2003, 'a3', 'A3', 'password')
    ON DUPLICATE KEY UPDATE
                         username = VALUES(username),
                         name = VALUES(name),
                         password = VALUES(password);


-- ------------------------------------------------------------
-- 3. EMPRUNTS
-- ------------------------------------------------------------
-- A3 (user_id 2003) détient L2, L3, L4 et L5.
-- return_date = NULL => emprunt non rendu
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
        3001,
        1002,
        2003,
        CURRENT_TIMESTAMP - INTERVAL 3 DAY,
        NULL,
        CURRENT_TIMESTAMP + INTERVAL 11 DAY
    ),
    (
        3002,
        1003,
        2003,
        CURRENT_TIMESTAMP - INTERVAL 3 DAY,
        NULL,
        CURRENT_TIMESTAMP + INTERVAL 11 DAY
    ),
    (
        3003,
        1004,
        2003,
        CURRENT_TIMESTAMP - INTERVAL 2 DAY,
        NULL,
        CURRENT_TIMESTAMP + INTERVAL 12 DAY
    ),
    (
        3004,
        1005,
        2003,
        CURRENT_TIMESTAMP - INTERVAL 1 DAY,
        NULL,
        CURRENT_TIMESTAMP + INTERVAL 13 DAY
    )
    ON DUPLICATE KEY UPDATE
                         book_id = VALUES(book_id),
                         user_id = VALUES(user_id),
                         issue_date = VALUES(issue_date),
                         return_date = VALUES(return_date),
                         due_date = VALUES(due_date);