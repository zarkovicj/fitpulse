INSERT INTO template (naziv, opis, created_by) VALUES
('Push dan', 'Grudi, ramena i triceps', NULL),
('Pull dan', 'Leđa i biceps', NULL),
('Dan za noge', 'Noge i stomak', NULL);

INSERT INTO template_vezba (id_template, id_vezbe, broj_serija, broj_ponavljanja, kilaza, redni_broj)
SELECT t.id, v.id, x.serije, x.ponavljanja, NULL, x.rb
FROM (VALUES
    ('Push dan', 'Bench Press', 4, 8, 1),
    ('Push dan', 'Incline Bench Press', 3, 10, 2),
    ('Push dan', 'Potisak iznad glave', 3, 10, 3),
    ('Push dan', 'Bočno podizanje', 3, 12, 4),
    ('Push dan', 'Triceps sklekovi', 3, 12, 5),
    ('Pull dan', 'Mrtvo dizanje', 4, 5, 1),
    ('Pull dan', 'Zgibovi', 3, 8, 2),
    ('Pull dan', 'Veslanje sa šipkom', 3, 10, 3),
    ('Pull dan', 'Lat povlačenje', 3, 12, 4),
    ('Pull dan', 'Pregib za bicepse', 3, 12, 5),
    ('Dan za noge', 'Čučanj', 4, 8, 1),
    ('Dan za noge', 'Presa za noge', 3, 10, 2),
    ('Dan za noge', 'Ispadni korak', 3, 12, 3),
    ('Dan za noge', 'Pregib nogu', 3, 12, 4),
    ('Dan za noge', 'Plank', 3, 1, 5)
) AS x(template, vezba, serije, ponavljanja, rb)
JOIN template t ON t.naziv = x.template AND t.created_by IS NULL
JOIN vezba v ON v.naziv = x.vezba AND v.created_by IS NULL;
