-- brisanje korisnika (ON DELETE CASCADE) briše i sve njegove podatke
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    birth_date          DATE,
    weight              NUMERIC(5,2),
    height              NUMERIC(5,2),
    role                VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    -- JWT izdat pre promene lozinke više ne važi
    password_changed_at TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE image (
    storage_key   VARCHAR(36) PRIMARY KEY,
    data          BYTEA       NOT NULL,
    content_type  VARCHAR(30) NOT NULL,
    size_bytes    INTEGER     NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- created_by = NULL znači sistemska vežba (vidljiva svima, menja je samo admin)
CREATE TABLE exercise (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    muscle_group  VARCHAR(30)  NOT NULL,
    description   TEXT,
    video_url     VARCHAR(500),
    image_key     VARCHAR(36)  REFERENCES image (storage_key),
    created_by    BIGINT       REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE template (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    description  TEXT,
    created_by   BIGINT       REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE template_exercise (
    id           BIGSERIAL PRIMARY KEY,
    template_id  BIGINT  NOT NULL REFERENCES template (id) ON DELETE CASCADE,
    exercise_id  BIGINT  NOT NULL REFERENCES exercise (id),
    set_count    INT     NOT NULL,
    reps         INT     NOT NULL,
    weight       NUMERIC(6,2),
    position     INT     NOT NULL,
    UNIQUE (template_id, position)
);

CREATE TABLE workout (
    id           BIGSERIAL PRIMARY KEY,
    template_id  BIGINT      REFERENCES template (id) ON DELETE SET NULL,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    date         DATE        NOT NULL,
    started_at   TIMESTAMPTZ NOT NULL,
    finished_at  TIMESTAMPTZ,
    status       VARCHAR(20) NOT NULL
);

-- najviše jedan trening u toku po korisniku
CREATE UNIQUE INDEX uq_workout_active_per_user ON workout (user_id) WHERE status = 'IN_PROGRESS';

CREATE TABLE workout_exercise (
    id           BIGSERIAL PRIMARY KEY,
    workout_id   BIGINT  NOT NULL REFERENCES workout (id) ON DELETE CASCADE,
    exercise_id  BIGINT  NOT NULL REFERENCES exercise (id),
    set_count    INT     NOT NULL,
    position     INT     NOT NULL,
    completed    BOOLEAN NOT NULL DEFAULT FALSE,
    rest_after   INT
);

CREATE TABLE workout_set (
    id                   BIGSERIAL PRIMARY KEY,
    workout_exercise_id  BIGINT  NOT NULL REFERENCES workout_exercise (id) ON DELETE CASCADE,
    position             INT     NOT NULL,
    reps                 INT     NOT NULL,
    weight               NUMERIC(6,2),
    completed            BOOLEAN NOT NULL DEFAULT FALSE,
    rest_after           INT
);

-- weight može biti prazan: rekord u ponavljanjima bez tega (zgibovi, sklekovi)
CREATE TABLE personal_record (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id    BIGINT      NOT NULL REFERENCES exercise (id),
    type           VARCHAR(30) NOT NULL,
    weight         NUMERIC(6,2),
    reps           INT         NOT NULL,
    estimated_1rm  NUMERIC(6,2),
    achieved_at    TIMESTAMPTZ NOT NULL,
    workout_id     BIGINT      REFERENCES workout (id),
    UNIQUE (user_id, exercise_id, type)
);

CREATE TABLE body_goal (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    weight            NUMERIC(5,2),
    body_fat_percent  NUMERIC(4,1)
);

CREATE TABLE body_weight_log (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    weight   NUMERIC(5,2) NOT NULL,
    date     DATE         NOT NULL,
    UNIQUE (user_id, date)
);

-- jednokratni linkovi za promenu zaboravljene lozinke; čuva se samo heš tokena
CREATE TABLE password_reset_token (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exercise_created_by ON exercise (created_by);
CREATE INDEX idx_template_created_by ON template (created_by);
CREATE INDEX idx_template_exercise_exercise ON template_exercise (exercise_id);
CREATE INDEX idx_workout_user ON workout (user_id);
CREATE INDEX idx_workout_template ON workout (template_id);
CREATE INDEX idx_workout_exercise_workout ON workout_exercise (workout_id);
CREATE INDEX idx_workout_exercise_exercise ON workout_exercise (exercise_id);
CREATE INDEX idx_workout_set_workout_exercise ON workout_set (workout_exercise_id);
CREATE INDEX idx_personal_record_exercise ON personal_record (exercise_id);
CREATE INDEX idx_password_reset_token_user ON password_reset_token (user_id);
