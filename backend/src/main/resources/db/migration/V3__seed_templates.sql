-- sistemski šabloni treninga, sastavljeni od sistemskih vežbi
INSERT INTO template (name, description, created_by) VALUES
('Push Day', 'Grudi, ramena i triceps', NULL),
('Pull Day', 'Leđa i biceps', NULL),
('Leg Day', 'Noge i stomak', NULL);

INSERT INTO template_exercise (template_id, exercise_id, set_count, reps, weight, position)
SELECT t.id, e.id, x.set_count, x.reps, NULL, x.position
FROM (VALUES
    ('Push Day', 'Bench Press', 4, 8, 1),
    ('Push Day', 'Incline Bench Press', 3, 10, 2),
    ('Push Day', 'Overhead Press', 3, 10, 3),
    ('Push Day', 'Lateral Raise', 3, 12, 4),
    ('Push Day', 'Triceps Dips', 3, 12, 5),
    ('Pull Day', 'Deadlift', 4, 5, 1),
    ('Pull Day', 'Pull-ups', 3, 8, 2),
    ('Pull Day', 'Barbell Row', 3, 10, 3),
    ('Pull Day', 'Lat Pulldown', 3, 12, 4),
    ('Pull Day', 'Biceps Curl', 3, 12, 5),
    ('Leg Day', 'Squat', 4, 8, 1),
    ('Leg Day', 'Leg Press', 3, 10, 2),
    ('Leg Day', 'Dumbbell Lunge', 3, 12, 3),
    ('Leg Day', 'Leg Curl', 3, 12, 4),
    ('Leg Day', 'Plank', 3, 1, 5)
) AS x(template_name, exercise_name, set_count, reps, position)
JOIN template t ON t.name = x.template_name AND t.created_by IS NULL
JOIN exercise e ON e.name = x.exercise_name AND e.created_by IS NULL;
