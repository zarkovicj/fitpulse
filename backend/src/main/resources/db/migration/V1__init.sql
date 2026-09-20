CREATE TABLE korisnik (
    id              BIGSERIAL PRIMARY KEY,
    ime             VARCHAR(100) NOT NULL,
    prezime         VARCHAR(100) NOT NULL,
    mail            VARCHAR(255) NOT NULL UNIQUE,
    hash_password   VARCHAR(255) NOT NULL,
    datum_rodjenja  DATE,
    masa            NUMERIC(5,2),
    visina          NUMERIC(5,2),
    ciljna_masa     NUMERIC(5,2),
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE vezba (
    id              BIGSERIAL PRIMARY KEY,
    naziv           VARCHAR(150) NOT NULL,
    misicna_grupa   VARCHAR(30) NOT NULL,
    slika           VARCHAR(500),
    opis            TEXT,
    created_by      BIGINT REFERENCES korisnik(id)
);

CREATE TABLE template (
    id              BIGSERIAL PRIMARY KEY,
    naziv           VARCHAR(150) NOT NULL,
    opis            TEXT,
    created_by      BIGINT REFERENCES korisnik(id)
);

CREATE TABLE template_vezba (
    id                  BIGSERIAL PRIMARY KEY,
    id_template         BIGINT NOT NULL REFERENCES template(id) ON DELETE CASCADE,
    id_vezbe            BIGINT NOT NULL REFERENCES vezba(id),
    broj_serija         INT NOT NULL,
    broj_ponavljanja    INT NOT NULL,
    kilaza              NUMERIC(6,2),
    redni_broj          INT NOT NULL,
    UNIQUE (id_template, redni_broj)
);

CREATE TABLE trening (
    id              BIGSERIAL PRIMARY KEY,
    id_template     BIGINT REFERENCES template(id),
    id_korisnika    BIGINT NOT NULL REFERENCES korisnik(id),
    datum           DATE NOT NULL,
    started_at      TIMESTAMP NOT NULL,
    finished_at     TIMESTAMP,
    status          VARCHAR(20) NOT NULL
);

CREATE TABLE trening_vezba (
    id              BIGSERIAL PRIMARY KEY,
    id_treninga     BIGINT NOT NULL REFERENCES trening(id) ON DELETE CASCADE,
    id_vezbe        BIGINT NOT NULL REFERENCES vezba(id),
    broj_serija     INT NOT NULL,
    redni_broj      INT NOT NULL,
    completed       BOOLEAN NOT NULL DEFAULT false,
    rest_after      INT
);

CREATE TABLE trening_serija (
    id                  BIGSERIAL PRIMARY KEY,
    id_trening_vezbe    BIGINT NOT NULL REFERENCES trening_vezba(id) ON DELETE CASCADE,
    redni_broj          INT NOT NULL,
    broj_ponavljanja    INT NOT NULL,
    kilaza              NUMERIC(6,2),
    completed           BOOLEAN NOT NULL DEFAULT false,
    rest_after          INT
);

CREATE TABLE licni_rekord (
    id              BIGSERIAL PRIMARY KEY,
    id_korisnika    BIGINT NOT NULL REFERENCES korisnik(id),
    id_vezbe        BIGINT NOT NULL REFERENCES vezba(id),
    tip             VARCHAR(30) NOT NULL,
    kilaza          NUMERIC(6,2) NOT NULL,
    ponavljanja     INT NOT NULL,
    estimated_1rm   NUMERIC(6,2),
    achieved_at     TIMESTAMP NOT NULL,
    id_treninga     BIGINT REFERENCES trening(id),
    UNIQUE (id_korisnika, id_vezbe, tip)
);

CREATE TABLE body_goal (
    id                  BIGSERIAL PRIMARY KEY,
    id_korisnika        BIGINT NOT NULL UNIQUE REFERENCES korisnik(id),
    masa                NUMERIC(5,2),
    procenat_masti      NUMERIC(4,1)
);

CREATE TABLE body_masa_log (
    id              BIGSERIAL PRIMARY KEY,
    id_korisnika    BIGINT NOT NULL REFERENCES korisnik(id),
    masa            NUMERIC(5,2) NOT NULL,
    datum           DATE NOT NULL,
    UNIQUE (id_korisnika, datum)
);
