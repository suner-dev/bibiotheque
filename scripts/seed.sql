-- Jeu de données de démonstration pour la bibliothèque.
-- Usage :
--   docker exec -i bibliotheque-db mysql -uroot -pmysql bibliotheque < scripts/seed.sql
--
-- NB : la stratégie de nommage physique de Spring Boot met les noms de tables
-- en minuscules => la table de l'entité Books s'appelle physiquement `books`.
--
-- Mots de passe : "admin123" (hash BCrypt identique pour les deux comptes).

INSERT IGNORE INTO users (user_id, name, password, username) VALUES
  (1, 'Administrateur Biblio', '$2a$10$KsKIySAKpkRMm6t4LwiX3uiK.oCTmgOQg/OxY/QzXgJEVwPVpcOhC', 'admin'),
  (2, 'Jean Dupont',           '$2a$10$KsKIySAKpkRMm6t4LwiX3uiK.oCTmgOQg/OxY/QzXgJEVwPVpcOhC', 'jean');

INSERT IGNORE INTO user_role (user_id, role_id) VALUES (1, 1), (2, 2);

-- Livres indisponibles (0 exemplaire => réservables, RG-01)
-- et un livre disponible (5 exemplaires => réservation refusée RG-01).
INSERT IGNORE INTO books (book_id, book_name, book_author, book_genre, no_of_copies) VALUES
  (1, 'Le Petit Prince', 'Antoine de Saint-Exupéry', 'Conte', 0),
  (2, 'L''Étranger',     'Albert Camus',             'Roman', 0),
  (3, '1984',            'George Orwell',            'Dystopie', 0),
  (4, 'Dune',            'Frank Herbert',            'Science-fiction', 5);

-- La séquence Hibernate (GenerationType.AUTO) doit rester au-dessus des ids insérés à la main.
UPDATE hibernate_sequence SET next_val = 1000 WHERE next_val < 1000;
