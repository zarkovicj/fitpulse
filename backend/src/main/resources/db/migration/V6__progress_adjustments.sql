-- rekord "najviše ponavljanja" može biti bez tega (zgibovi, sklekovi)
ALTER TABLE licni_rekord ALTER COLUMN kilaza DROP NOT NULL;

-- ciljna masa se vodi samo u body_goal
ALTER TABLE korisnik DROP COLUMN ciljna_masa;
