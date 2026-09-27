-- sistemske vežbe (created_by = NULL), dostupne svim korisnicima
INSERT INTO exercise (name, muscle_group, description, created_by) VALUES
('Bench Press', 'CHEST', 'Potisak sa šipkom u ležećem položaju', NULL),
('Incline Bench Press', 'CHEST', 'Potisak sa šipkom na kosoj klupi', NULL),
('Push-ups', 'CHEST', 'Klasični sklekovi sa telesnom težinom', NULL),
('Dumbbell Fly', 'CHEST', 'Fly pokret sa bučicama u ležećem položaju', NULL),

('Deadlift', 'BACK', 'Dizanje šipke sa poda uz ispravljena leđa', NULL),
('Pull-ups', 'BACK', 'Zgibovi na vratilu nathvatom', NULL),
('Barbell Row', 'BACK', 'Povlačenje šipke uz nagnut trup', NULL),
('Lat Pulldown', 'BACK', 'Povlačenje na sajli za leđa', NULL),

('Squat', 'LEGS', 'Čučanj sa šipkom na leđima', NULL),
('Leg Press', 'LEGS', 'Potisak na spravi za noge', NULL),
('Dumbbell Lunge', 'LEGS', 'Naizmenični ispadni koraci sa bučicama', NULL),
('Leg Curl', 'LEGS', 'Pregibanje nogu na spravi za zadnju ložu', NULL),

('Overhead Press', 'SHOULDERS', 'Potisak šipke ili bučica iznad glave', NULL),
('Lateral Raise', 'SHOULDERS', 'Bočno podizanje bučica do visine ramena', NULL),
('Front Raise', 'SHOULDERS', 'Podizanje bučica ispred tela', NULL),

('Biceps Curl', 'ARMS', 'Pregib ruku sa šipkom ili bučicama', NULL),
('Triceps Dips', 'ARMS', 'Propadanja na paralelnim rukohvatima za triceps', NULL),
('Hammer Curl', 'ARMS', 'Pregib bučica neutralnim hvatom', NULL),

('Plank', 'CORE', 'Izometrijsko držanje položaja za stomak', NULL),
('Crunches', 'CORE', 'Klasični trbušnjaci', NULL),
('Hanging Leg Raise', 'CORE', 'Podizanje nogu dok visite na vratilu', NULL),

('Running', 'CARDIO', 'Trčanje na traci ili napolju', NULL),
('Jump Rope', 'CARDIO', 'Kardio vežba sa konopcem za preskakanje', NULL);
