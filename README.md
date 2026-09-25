# Syncticket

Support ticket management system: create and search tickets, enforce a fixed status lifecycle, add comments, and track status history. Behaviour is defined in **[`spec/`](spec/README.md)**; implementation lives in **`backend/`** (Spring Boot) and **`frontend/`** (React + Vite).

## Specifications

Start with [`spec/README.md`](spec/README.md) for reading order, requirement IDs (`FR-xx`, `AC-xx`), and how to change behaviour (spec first, then code).

| Area | Doc |
|------|-----|
| Features and acceptance criteria | [`spec/requirements.md`](spec/requirements.md) |
| Status transitions | [`spec/state-machine.md`](spec/state-machine.md) |
| REST API and errors | [`spec/api-contract.md`](spec/api-contract.md) |
| Tests | [`spec/test-strategy.md`](spec/test-strategy.md) |

## Repository layout

```
syncticket/
├── spec/              # Product and technical specifications (source of truth)
├── backend/           # Java 21, Spring Boot 3, Gradle, Flyway, PostgreSQL
├── frontend/          # React 19, TypeScript, Vite, TanStack Query
├── docker-compose.yml # PostgreSQL 16 for local development
├── .cursor/rules/     # Cursor AI project rules (reference spec/)
├── .specstory/        # SpecStory session history and CLI config
└── .env               # Local secrets (git-ignored) — create at repo root
```

**v1 scope:** no authentication; assignee and comment author are free text. See [`spec/requirements.md`](spec/requirements.md) §1.

## Prerequisites

| Tool | Version / notes |
|------|------------------|
| JDK | 21 |
| Gradle | 9.7.x (system install; no wrapper in repo) |
| Node.js | LTS (for `frontend/`) |
| Docker | Optional; required for PostgreSQL via Compose and backend integration tests (Testcontainers) |

## Quick start (full stack)

### 1. Environment

Create a repo-root `.env` (never commit it). Minimum for PostgreSQL:

```env
POSTGRES_DB=tickets
POSTGRES_USER=tickets
POSTGRES_PASSWORD=change-me-local-only
POSTGRES_PORT=5433

DB_URL=jdbc:postgresql://localhost:5433/tickets
DB_USERNAME=tickets
DB_PASSWORD=change-me-local-only

CORS_ALLOWED_ORIGINS=http://localhost:5173
SERVER_PORT=8080
```

`POSTGRES_PASSWORD` and `DB_PASSWORD` must match. `backend` `bootRun` loads this file from the repo root.

### 2. Database

From the repo root:

```bash
docker compose up -d db
docker compose ps   # wait until db is healthy (host port 5433 by default)
```

### 3. Backend

```bash
cd backend
gradle bootRun
```

- API: `http://localhost:8080/api`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`

**H2 without Docker:** `gradle bootRun --args='--spring.profiles.active=h2'`

**Dev seed (100 tickets, PostgreSQL only):** `gradle bootRun --args='--spring.profiles.active=dev'`

More detail: [`backend/README.md`](backend/README.md).

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Vite proxies `/api` to port 8080.

More detail: [`frontend/README.md`](frontend/README.md).

## Verify

```bash
cd backend
gradle check          # unit tests + integration tests (Docker for Testcontainers)

cd ../frontend
npm test
npm run build
```

Integration tests use real PostgreSQL in containers; they are skipped when Docker is unavailable (`disabledWithoutDocker`).

## AI and tooling

- **Cursor:** rules in [`.cursor/rules/`](.cursor/rules/) point at `spec/`.
- **SpecStory:** history and config under [`.specstory/`](.specstory/).

When prompting tools, prefer loading only the spec files needed for the task (see [`spec/README.md`](spec/README.md)).

## Troubleshooting

| Issue | What to try |
|-------|-------------|
| `password authentication failed` for Postgres | Ensure `.env` passwords match; if the volume was created with an old password: `docker compose down -v` then `docker compose up -d db` (wipes local DB data). |
| Port 5433 in use | Set `POSTGRES_PORT` in `.env` and update `DB_URL` accordingly. |
| Frontend cannot reach API | Run backend on 8080; use `npm run dev` so the Vite proxy is active. |

## License

Not specified in repository; add a `LICENSE` file if you intend to open-source or distribute.
