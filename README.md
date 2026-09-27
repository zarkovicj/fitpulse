# FitPulse

FitPulse je web aplikacija za praćenje treninga u teretani. Korisnik pravi template-ove treninga od gotovih ili sopstvenih vežbi, pokreće trening i beleži serije (ponavljanja, kilaža, odmor između serija), a aplikacija sama prepoznaje lične rekorde i prikazuje napredak kroz vreme.

**Glavne funkcionalnosti**

- registracija i prijava (JWT), zaboravljena lozinka preko maila, brisanje naloga
- katalog vežbi po mišićnim grupama, sa slikom, opisom i YouTube videom
- template-i treninga i trening uživo: tajmer odmora posle svake serije, a vrednosti se preuzimaju iz prethodnog treninga
- istorija treninga, lični rekordi (maksimalna kilaža, procenjeni 1RM), grafik telesne mase i cilj
- administrator: upravlja sistemskim vežbama i template-ima, blokira i briše korisničke naloge

**Tehnologije:** Java 21, Spring Boot 4, PostgreSQL 16, Flyway, Angular 21, Tailwind CSS, nginx, Docker Compose. Testovi: JUnit 5, Testcontainers, Vitest.

---

## Pokretanje

- [1. Pregled](#1-pregled)
- [2. Preduslovi](#2-preduslovi)
- [3. Lokalno pokretanje](#3-lokalno-pokretanje)
- [4. Konfiguracija](#4-konfiguracija)
- [5. Razvojni režim](#5-razvojni-režim)
- [6. Testovi](#6-testovi)
- [7. Održavanje](#7-održavanje)
- [8. Rešavanje problema](#8-rešavanje-problema)

## 1. Pregled

Aplikaciju čine četiri kontejnera, opisana u `docker-compose.yml`:

```
                    ┌──────────────── Docker Compose ─────────────────────────┐
                    │                                                         │
 browser ──:8081──► │  frontend (nginx)  ──/api──►  backend (Spring Boot)     │
                    │   Angular build                :8080                    │
                    │                                  │           │          │
                    │                                  ▼           ▼          │
                    │                           db (PostgreSQL)  mailpit      │
                    │                               :5432        :1025 SMTP   │
                    └─────────────────────────────────────────────────────────┘
```

| Kontejner | Uloga |
|-----------|-------|
| **frontend** | nginx servira produkcijski build Angular aplikacije i prosleđuje sve zahteve na `/api/...` backendu. Frontend i API su tako na istoj adresi, pa CORS nije potreban. Nepoznate putanje (npr. `/workouts/5`) vraćaju `index.html`, a rutu rešava Angular. |
| **backend** | Spring Boot REST API. Pri startu Flyway sam kreira i ažurira šemu baze. Nije izložen na računaru, do njega se stiže samo preko nginx-a. |
| **db** | PostgreSQL 16. Podaci su u Docker volume-u `pgdata` i ostaju sačuvani posle gašenja kontejnera. |
| **mailpit** | Lažni SMTP server. Hvata sve poslate mailove (npr. link za zaboravljenu lozinku) i prikazuje ih na `http://localhost:8025`. |

Kontejneri se podižu redom, uz proveru zdravlja (healthcheck): prvo baza, backend tek kad baza prihvata konekcije, a frontend tek kad backend odgovara na `/actuator/health`.

## 2. Preduslovi

Za pokretanje:

- **Docker Desktop** (Windows/macOS) ili **Docker Engine + Compose v2** (Linux)
- **Git**

Samo za razvoj i testove:

- **JDK 21**
- **Node.js 24** i npm

Maven ne mora da bude instaliran, jer projekat sadrži Maven Wrapper (`mvnw` / `mvnw.cmd`).

## 3. Lokalno pokretanje

### 3.1 Preuzimanje koda

```bash
git clone https://github.com/zarkovicj/fitpulse.git
cd fitpulse
```

### 3.2 Podešavanje `.env`

Tajne vrednosti (lozinka baze, JWT ključ) se ne čuvaju u repozitorijumu. Potrebno je napraviti lokalni `.env` od šablona:

```bash
cp .env.example .env          # Linux/macOS/Git Bash
Copy-Item .env.example .env   # PowerShell
```

U `.env` popuni obavezne vrednosti:

```ini
POSTGRES_PASSWORD=neka-lozinka
JWT_SECRET=<najmanje 32 karaktera>
```

JWT ključ se može generisati ovako:

```bash
openssl rand -base64 32
```
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

Opciono, za administratorski nalog koji se automatski kreira pri prvom startu:

```ini
ADMIN_MAIL=admin@fitpulse.local
ADMIN_PASSWORD=admin12345
```

### 3.3 Pokretanje

```bash
docker compose up --build
```

Prvo pokretanje traje nekoliko minuta, jer Docker preuzima slike i builduje backend (Maven) i frontend (npm). Kasnija pokretanja su brža zbog keša.

Šta se dešava pri startu:

1. **db** se podiže i prijavljuje kao `healthy`.
2. **backend** se povezuje na bazu, a Flyway primenjuje migracije `V1`–`V3`: kreira tabele i ubacuje sistemske podatke (23 vežbe i 3 šablona treninga).
3. Ako su `ADMIN_MAIL` i `ADMIN_PASSWORD` postavljeni, a taj nalog ne postoji, kreira se administrator.
4. **frontend** (nginx) počinje da prima zahteve kad je backend `healthy`.

Za rad u pozadini, bez ispisa logova u terminalu:

```bash
docker compose up -d --build
```

### 3.4 Provera

```bash
docker compose ps
```

Svi servisi treba da budu `running`, a `db` i `backend` označeni kao `(healthy)`.

| Adresa | Šta je tamo |
|--------|-------------|
| http://localhost:8081 | aplikacija (registracija, prijava) |
| http://localhost:8025 | Mailpit, primljeni mailovi |
| `localhost:5433` | PostgreSQL, za IntelliJ / pgAdmin (korisnik i lozinka iz `.env`) |

Portovi 5433 (baza), 1025 i 8025 (Mailpit) otvoreni su radi lakšeg razvoja i nisu namenjeni produkciji.


## 4. Konfiguracija

Sva podešavanja se zadaju kroz `.env` u korenu projekta. Docker Compose ih prosleđuje kontejnerima.

| Promenljiva | Obavezna | Podrazumevano | Značenje |
|-------------|----------|---------------|----------|
| `POSTGRES_DB` | da | `fitpulse` | ime baze |
| `POSTGRES_USER` | da | `fitpulse` | korisnik baze |
| `POSTGRES_PASSWORD` | **da** | — | lozinka baze |
| `JWT_SECRET` | **da** | — | ključ za potpisivanje JWT tokena, najmanje 32 karaktera |
| `JWT_EXPIRATION_MS` | ne | `86400000` (24 h) | trajanje tokena za prijavu |
| `APP_PORT` | ne | `8081` | port na računaru na kom je aplikacija dostupna |
| `ADMIN_MAIL` | ne | — | mail administratora koji se kreira pri startu |
| `ADMIN_PASSWORD` | ne | — | lozinka administratora, najmanje 8 karaktera |
| `MAIL_HOST` | ne | `mailpit` | SMTP server |
| `MAIL_PORT` | ne | `1025` | SMTP port |
| `MAIL_USERNAME` | ne | — | SMTP korisnik |
| `MAIL_PASSWORD` | ne | — | SMTP lozinka |
| `MAIL_FROM` | ne | `FitPulse <no-reply@fitpulse.local>` | pošiljalac mailova |

Promenljive `MAIL_*` u `.env.example` su namerno zakomentarisane. Postavljaju se samo za pravi SMTP server (npr. Gmail), jer prazna vrednost u `.env` poništava podrazumevanu.

Posle izmene `.env` dovoljno je ponovo pokrenuti:

```bash
docker compose up -d
```

## 5. Razvojni režim

Tokom razvoja backend i frontend se pokreću van Docker-a (brže izmene, debug), a u Docker-u ostaju samo baza i Mailpit.

```bash
docker compose up -d db mailpit
```

**Backend** (port 8080), iz IntelliJ-a ili iz terminala. Podrazumevane vrednosti u `application.yml` već pokazuju na bazu `localhost:5433` i Mailpit `localhost:1025`, pa treba postaviti samo JWT ključ i lozinku baze:

```powershell
cd backend
$env:JWT_SECRET="<isti kao u .env>"
$env:SPRING_DATASOURCE_PASSWORD="<POSTGRES_PASSWORD iz .env>"
.\mvnw.cmd spring-boot:run
```

```bash
cd backend
JWT_SECRET=... SPRING_DATASOURCE_PASSWORD=... ./mvnw spring-boot:run
```

**Frontend** (port 4200):

```bash
cd frontend
npm install
npm start
```

Aplikacija je na http://localhost:4200. `proxy.conf.json` prosleđuje `/api` na `localhost:8080`, isto kao nginx u Docker verziji, pa kod frontenda ne zavisi od režima rada.

## 6. Testovi

**Backend**: 35 unit i 28 integracionih testova. Integracioni testovi pomoću Testcontainers-a podižu pravi PostgreSQL u Docker-u, pa **Docker mora biti pokrenut**.

```powershell
cd backend
.\mvnw.cmd verify
```

**Frontend**: 82 testa (Vitest).

```bash
cd frontend
npm test
```

Docker build backenda preskače testove (`-DskipTests`), jer unutar build kontejnera nema Docker-a za Testcontainers. Testovi se zato pokreću ovim komandama.

## 7. Održavanje

| Komanda | Šta radi |
|---------|----------|
| `docker compose logs -f backend` | prati logove backenda (Ctrl+C za izlaz) |
| `docker compose logs --tail 100 frontend` | poslednjih 100 linija nginx loga |
| `docker compose restart backend` | restartuje jedan servis |
| `docker compose stop` / `start` | zaustavlja / pokreće sve, bez brisanja |
| `docker compose down` | gasi i uklanja kontejnere, **podaci ostaju** |
| `docker compose down -v` | gasi i **briše bazu** (volume `pgdata`), sledeći start kreće od prazne baze |
| `docker compose exec db psql -U fitpulse -d fitpulse` | SQL konzola u bazi |

Posle izmene koda, `docker compose up -d --build` ponovo builduje samo ono što se promenilo. Nove Flyway migracije (npr. `V4__...sql`) primenjuju se automatski pri startu backenda, a postojeći podaci ostaju.


## 8. Rešavanje problema

**`port is already allocated`**
Port 8081, 5433, 1025 ili 8025 već koristi drugi program. Za aplikaciju promeni `APP_PORT` u `.env`. Za ostale portove potrebno je zaustaviti program koji ih zauzima (npr. lokalni PostgreSQL).

**Backend se ne podiže / `backend is unhealthy`**
Pogledati `docker compose logs backend`. Najčešći uzroci:
- prazan ili prekratak `JWT_SECRET` (mora imati najmanje 32 karaktera)
- pogrešna lozinka baze. Ako je `POSTGRES_PASSWORD` promenjen posle prvog pokretanja, postojeći volume i dalje ima staru lozinku. Vratiti staru vrednost ili, ako podaci nisu bitni, pokrenuti `docker compose down -v`.

**Mail za promenu lozinke ne stiže**
Mailovi ne idu u pravo sanduče, nego u Mailpit na http://localhost:8025.

**Upload slike vraća 413**
Slika je veća od 2 MB. Frontend smanjuje sliku pre slanja, pa se ovo javlja samo kod direktnih API poziva.

**Stranica je prazna posle ažuriranja**
Browser drži stari `index.html`. Osvežiti sa Ctrl+F5.

**Integracioni testovi padaju sa `Could not find a valid Docker environment`**
Testcontainers ne vidi Docker. Pokrenuti Docker Desktop i ponovi `.\mvnw.cmd verify`.