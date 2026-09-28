-- Idempotent seed data: safe to run on every start-up against a persistent PostgreSQL database
INSERT INTO person (first_name, last_name, title)
SELECT 'James', 'Kirk', 'Capt'
WHERE NOT EXISTS (SELECT 1 FROM person WHERE first_name = 'James' AND last_name = 'Kirk');
