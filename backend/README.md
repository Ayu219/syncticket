# Syncticket backend

Java 21 · Spring Boot 3.5 · **Gradle 9.7.1** (system `gradle`, no wrapper).

Backend packages follow **Spring MVC**: `controller` → `service` → `repository` / `model`, plus `dto`, `mapper`, `exception`, `config`.

## Build

```bash
cd backend
gradle test          # unit tests
gradle integrationTest
gradle check         # unit + integration
gradle bootRun --args='--spring.profiles.active=h2'
```

### PostgreSQL (Docker Compose)

From repo root:

```bash
cp .env.example .env
# edit .env — set POSTGRES_PASSWORD and DB_PASSWORD to the same value
docker compose up -d db
docker compose ps   # wait until db is healthy (host port **5433** by default — not 5432)
```

Run the app (`bootRun` loads repo-root `.env`; `POSTGRES_PASSWORD` and `DB_PASSWORD` must match):

```bash
cd backend
gradle bootRun
```

If you still get `password authentication failed` after changing `.env`, the Postgres volume may have been created with an old password: `docker compose down -v` then `docker compose up -d db` (wipes local DB data).

Stop DB: `docker compose down` (add `-v` to drop the data volume).

### Seed data (100 tickets)

PostgreSQL only (`db/migration/dev`). Activate **`dev`** profile (can combine with default DB config):

```bash
gradle bootRun --args='--spring.profiles.active=dev'
```

Skips insert if `Seed ticket #` rows already exist. Fresh DB: run once after V1–V3.

### H2 (no Docker)

`gradle bootRun --args='--spring.profiles.active=h2'`

API base: `http://localhost:8080/api` · OpenAPI: `/swagger-ui.html`
