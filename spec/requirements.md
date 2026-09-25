# Requirements

## 1. Purpose

A web application that lets a support team create, track, update and discuss support tickets, with a backend-enforced ticket lifecycle.

The application is also a vehicle for demonstrating **spec-driven, AI-assisted development**: requirements → specs → AI-generated code → human validation → tests.

## 2. Scope

**In scope:** ticket CRUD (no delete), comments (add, edit, delete), keyword search, status filtering, status lifecycle (including reopen), mandatory transition notes, persistence, backend validation, meaningful UI errors.

**Out of scope (v1):** authentication/authorisation, user management, attachments, email notifications, SLAs, ticket deletion, real-time updates, multi-tenancy.

## 3. Actors

| Actor | Description |
|-------|-------------|
| Support agent | Any user of the UI. No login in v1; the agent types their name when commenting. |
| System | Spring Boot backend enforcing validation and state rules. |

## 4. Functional requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-01 | A user can create a ticket with title, description, priority and optional assignee. New tickets start in `OPEN`. | MUST |
| FR-02 | A user can list tickets, newest first, paginated. | MUST |
| FR-03 | A user can view a ticket's full details, including comments and current status. | MUST |
| FR-04 | A user can update title, description and priority of a ticket. | MUST |
| FR-05 | A user can set, change or clear the assignee of a ticket. | MUST |
| FR-06 | A user can add a comment (author + text) to a ticket. Comments are shown oldest first. | MUST |
| FR-07 | A user can search tickets by keyword; matches title **or** description, case-insensitive, partial match. | MUST |
| FR-08 | A user can filter the ticket list by status. Search and filter can be combined. | MUST |
| FR-09 | A user can change a ticket's status; only transitions defined in `state-machine.md` are allowed. | MUST |
| FR-10 | The UI only offers status actions that are valid from the current status. | SHOULD |
| FR-11 | Each successful status change is recorded in status history (from, to, timestamp, optional note). | MUST |
| FR-12 | A user can reopen a `RESOLVED` ticket to `IN_PROGRESS` (`RESOLVED → IN_PROGRESS`). | MUST |
| FR-13 | Transition to `RESOLVED` or `CANCELLED` requires a resolution note (stored on the history row). | MUST |
| FR-14 | A user can edit a comment's body on a non-terminal ticket. | MUST |
| FR-15 | A user can delete a comment on a non-terminal ticket. | MUST |

## 5. Business rules

| ID | Rule |
|----|------|
| BR-01 | Status lifecycle per `state-machine.md`: forward path, cancel paths, and `RESOLVED → IN_PROGRESS` (reopen). |
| BR-06 | `POST .../transitions` to `RESOLVED` or `CANCELLED` MUST include a non-empty `note` (after trim). Other targets MUST NOT require `note`; if sent, it is ignored and not stored. |
| BR-07 | Comment edit and delete are allowed only when the parent ticket is not terminal (`CLOSED`, `CANCELLED`). No author verification in v1 (A-01). |
| BR-02 | Any transition not listed in BR-01 is rejected by the backend, including same-status "transitions" (e.g. `OPEN → OPEN`). |
| BR-03 | Status can only be changed through the dedicated transition endpoint, never through the general update endpoint. |
| BR-04 | Tickets in a terminal status (`CLOSED`, `CANCELLED`) are read-only: field updates and new comments are rejected. (See A-03.) |
| BR-05 | The client cannot set `id`, `status`, `createdAt`, `updatedAt` on create or update; the server owns them. |

## 6. Validation rules (enforced by backend)

| Field | Rule |
|-------|------|
| `title` | required, trimmed, 3–200 characters |
| `description` | required, trimmed, 1–5000 characters |
| `priority` | required, one of `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `assignee` | optional; if present, trimmed, 1–100 characters; empty string is treated as "unassigned" |
| comment `author` | required, trimmed, 1–100 characters |
| comment `body` | required, trimmed, 1–2000 characters |
| transition `note` | required when `targetStatus` is `RESOLVED` or `CANCELLED`; trimmed, 1–2000 characters; otherwise absent or ignored |
| search `q` | optional, max 100 characters |
| `status` filter | optional, must be a valid status value |
| paging | `page` ≥ 0, `size` 1–100 (default 20) |

## 7. Non-functional requirements

| ID | Requirement |
|----|-------------|
| NFR-01 | Java 21, Spring Boot 3.x, REST/JSON API. |
| NFR-02 | PostgreSQL for the default runtime profile; H2 allowed for quick local dev. Data MUST survive an application restart when running on PostgreSQL. |
| NFR-03 | Schema managed by versioned migrations (Flyway); `ddl-auto` MUST NOT create or update the schema. |
| NFR-04 | Frontend in React (Vite + TypeScript) or Next.js. |
| NFR-05 | All error responses use one consistent JSON format (see `api-contract.md §3`). |
| NFR-06 | No secrets committed: credentials come from environment variables; `.env` is git-ignored; `.env.example` is committed with placeholders. |
| NFR-07 | Concurrent edits are detected with optimistic locking and reported as a conflict, not silently overwritten. |
| NFR-08 | List/search responds in < 500 ms for 10,000 tickets on a developer machine. |
| NFR-09 | The project builds and all tests pass with a single command per side (`./mvnw verify`, `npm test`). |

## 8. Acceptance criteria

| ID | Criterion | Covered by | Verified by |
|----|-----------|-----------|-------------|
| AC-01 | Ticket can be created from UI | FR-01 | UI test + manual |
| AC-02 | Tickets can be listed | FR-02 | API IT + UI test |
| AC-03 | Ticket details can be viewed | FR-03 | API IT + UI test |
| AC-04 | Ticket fields can be updated | FR-04 | API IT |
| AC-05 | Assignee can be changed | FR-05 | API IT |
| AC-06 | Comments can be added | FR-06 | API IT |
| AC-07 | Search works | FR-07 | Repository test + API IT |
| AC-08 | Status filter works | FR-08 | Repository test + API IT |
| AC-09 | Valid status transitions work | FR-09, BR-01 | State-machine IT |
| AC-10 | Invalid status transitions are rejected by backend | BR-02 | State-machine IT |
| AC-11 | Data survives application restart | NFR-02 | Restart test (test-strategy §5) |
| AC-12 | Backend validation works | §6 | Controller tests + API IT |
| AC-13 | UI shows meaningful errors | NFR-05, ui-flow §5 | UI tests + manual |
| AC-14 | State-machine integration tests pass | BR-01, BR-02 | CI |
| AC-15 | No secrets are committed | NFR-06 | secret scan in CI + review |
| AC-16 | Reopen `RESOLVED → IN_PROGRESS` works | FR-12 | State-machine IT |
| AC-17 | Resolution note required for `RESOLVED` / `CANCELLED` transitions | FR-13, BR-06 | API IT |
| AC-18 | Comments can be edited and deleted on editable tickets | FR-14, FR-15 | API IT + UI test |

## 9. Assumptions

| ID | Assumption | Reason |
|----|-----------|--------|
| A-01 | No authentication in v1; assignee and comment author are free-text names. | Not requested in the brief. |
| A-02 | Tickets cannot be deleted; `CANCELLED` is the "soft delete". | Preserves audit trail. |
| A-03 | Terminal tickets (`CLOSED`, `CANCELLED`) are read-only, including comments. `RESOLVED` tickets remain editable and commentable. | **Confirmed** (product, 2026-09-25). |
| A-04 | Reopening is allowed: `RESOLVED → IN_PROGRESS` only (not to `OPEN`). | Product decision (2026-09-25). |
| A-06 | Resolution notes live on `ticket_status_history`, not on the ticket row. | Keeps audit per transition. |
| A-05 | Search is a simple `ILIKE` substring match, not full-text search. | Sufficient for v1 volume. |

## 10. Resolved product questions (2026-09-25)

| Question | Answer |
|----------|--------|
| Allow `RESOLVED → IN_PROGRESS` (reopen)? | **Yes** in v1 (FR-12). |
| Mandatory resolution note on `RESOLVED` / `CANCELLED`? | **Yes** (FR-13, BR-06). |
| Comments editable or deletable? | **Yes** on non-terminal tickets (FR-14, FR-15). |

## 11. Implementation decisions (locked)

| Topic | Decision |
|-------|----------|
| Source of truth | `spec/` overrides legacy `.cursorrules` where they conflict (Java 21, Problem Details, `/api`, constructor injection). |
| Artifact / package | Repo `syncticket`; Java base package `com.syncticket`. |
| Frontend | React 18 + Vite + TypeScript (architecture default). |
| Status history | FR-11 in v1: persist on create + transitions; detail UI shows history (`GET /history`). |
| Build order | Backend (API + tests) first, then frontend. |
| Reopen + notes + comment CRUD | In v1 per §10 (not deferred). |
