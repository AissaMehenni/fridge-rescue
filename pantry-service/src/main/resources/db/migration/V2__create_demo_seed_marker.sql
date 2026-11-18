-- Marqueur garantissant un seul chargement des données de démo,
-- même si plusieurs répliques démarrent en même temps (clé primaire = verrou).
CREATE TABLE demo_seed (
    id         INTEGER                     NOT NULL PRIMARY KEY,
    seeded_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
