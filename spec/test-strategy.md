# Test Strategy

## 1. Goals

1. Prove every acceptance criterion in `requirements.md §8`.
2. Prove the state machine exhaustively (all 25 from/to pairs).
3. Catch defects in AI-generated code before merge — tests are written from the **spec**, not from the generated implementation.

## 2. Test pyramid

| Level | Tool | Scope | Runs in |
|-------|------|-------|---------|
| Unit | JUnit 5, AssertJ | `TicketStatus`, `Ticket` domain, mappers, validators | `./mvnw test` |
| Web slice | `@WebMvcTest` + MockMvc | Controllers: validation, status codes, error JSON | `./mvnw test` |
| Repository | `@DataJpaTest` + Testcontainers PostgreSQL | Search, filter, paging, migrations | `./mvnw verify` |
| Integration | `@SpringBootTest` + Testcontainers + MockMvc/RestClient | Full HTTP → DB flows, **state machine** | `./mvnw verify` |
| Frontend component | Vitest + React Testing Library + MSW | Forms, error display, status buttons | `npm test` |
| E2E (optional) | Playwright | Create → transition → comment happy path | `npm run e2e` |
| Persistence | Script / manual | Data survives restart (AC-11) | Pre-release |

Integration tests use real PostgreSQL (Testcontainers), not H2, so SQL and constraints behave as in production.

## 3. Test design principles

- Test names describe behaviour: `shouldRejectTransition_whenClosedToOpen()`.
- Given / When / Then structure; one behaviour per test.
- Each test creates its own data; no ordering dependence. Clean tables between integration tests.
- Assert on **error `code`** and HTTP status, not only on message text.
- Every test class/method references the requirement it covers (`@DisplayName("AC-10 ...")` or a comment).

## 4. State-machine tests (AC-09, AC-10, AC-14) — mandatory

### 4.1 Unit — `TicketStatusTest`

Parameterised over all 25 pairs from `state-machine.md §4`:

```java
@ParameterizedTest(name = "{0} -> {1} allowed={2}")
@CsvSource({
  "OPEN,IN_PROGRESS,true",   "OPEN,CANCELLED,true",
  "IN_PROGRESS,RESOLVED,true","IN_PROGRESS,CANCELLED,true",
  "RESOLVED,CLOSED,true", "RESOLVED,IN_PROGRESS,true",
  "OPEN,OPEN,false", "OPEN,RESOLVED,false", "OPEN,CLOSED,false",
  "IN_PROGRESS,OPEN,false", "IN_PROGRESS,IN_PROGRESS,false", "IN_PROGRESS,CLOSED,false",
  "RESOLVED,OPEN,false", "RESOLVED,RESOLVED,false", "RESOLVED,CANCELLED,false",
  "CLOSED,OPEN,false", "CLOSED,IN_PROGRESS,false", "CLOSED,RESOLVED,false", "CLOSED,CLOSED,false", "CLOSED,CANCELLED,false",
  "CANCELLED,OPEN,false", "CANCELLED,IN_PROGRESS,false", "CANCELLED,RESOLVED,false", "CANCELLED,CLOSED,false", "CANCELLED,CANCELLED,false"
})
void transitionMatrix(TicketStatus from, TicketStatus to, boolean allowed) {
    assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
}
```

Plus: a guard test that fails if a new enum value is added without updating the matrix (`assertThat(TicketStatus.values()).hasSize(5)`).

### 4.2 Integration — `TicketStatusTransitionIT`

Against the real HTTP API and PostgreSQL:

| Test | Expectation |
|------|-------------|
| Every valid pair (6) — drive a ticket to `from` via valid path, POST transition to `to` (with `note` when target is `RESOLVED` or `CANCELLED`) | `200`, response status = `to`, DB status = `to`, history row written with note when required |
| Every invalid pair (19) | `409 INVALID_STATUS_TRANSITION`, `currentStatus`/`requestedStatus` correct, DB status **unchanged**, no history row |
| Full happy path `OPEN → IN_PROGRESS → RESOLVED → CLOSED` | all `200`, history has 4 rows (incl. creation) |
| Reopen `RESOLVED → IN_PROGRESS → RESOLVED → CLOSED` | `200`; notes on both `RESOLVED` transitions |
| Transition to `RESOLVED` / `CANCELLED` without `note` | `400 VALIDATION_FAILED`, field `note` |
| `PATCH` / `DELETE` comment on editable ticket | `200` / `204`; list reflects change |
| `PATCH` / `DELETE` comment on `CLOSED` ticket | `409 TICKET_NOT_EDITABLE` |
| Brief examples `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN` | explicit named tests, `409` |
| Unknown status `"DONE"` | `400 VALIDATION_FAILED` |
| Missing `targetStatus` | `400 VALIDATION_FAILED` |
| Non-existent ticket | `404 TICKET_NOT_FOUND` |
| `status` sent in `PATCH` | `400`, status unchanged |
| Stale `version` | `409 CONCURRENT_MODIFICATION` |
| Update / comment on `CLOSED` and `CANCELLED` | `409 TICKET_NOT_EDITABLE` |
| `allowedTransitions` in GET response for each status | matches matrix |

Helper: `givenTicketInStatus(TicketStatus s)` reaches `s` **only through the API** (never by writing the DB directly), so the helper itself exercises valid paths.

## 5. Other functional tests

| AC | Test | Level |
|----|------|-------|
| AC-01 | Create returns 201, `Location`, status OPEN; UI form submits and navigates | IT + component |
| AC-02 | List paginated, newest first, excludes description | IT |
| AC-03 | Get by id includes comments count, allowedTransitions; 404 path | IT |
| AC-04 | PATCH title/description/priority; only sent fields change; `updatedAt` advances | IT |
| AC-05 | Set, change, clear assignee (`null` and `""`) | IT |
| AC-06 | Add comment 201; list ordered oldest first; validation errors | IT |
| AC-16 – AC-18 | Reopen, transition notes, comment edit/delete | IT + component |
| AC-07 | Search: title match, description match, case-insensitive, partial, no match → empty page, `%`/`_` literal | Repository + IT |
| AC-08 | Filter by each status; combined with `q`; invalid status → 400 | Repository + IT |
| AC-12 | Each rule in `requirements.md §6`: boundary values (2/3 and 200/201 chars for title, etc.), blanks, whitespace-only, bad enums, malformed JSON → 400 with correct `errors[].field` | WebMvc + IT |
| AC-13 | Field errors rendered under inputs; banner for 409/500/network; input preserved | Component (MSW) |

### 5.1 Restart persistence (AC-11)

Scripted check, documented in README:

```bash
docker compose up -d db
./mvnw spring-boot:run &            # start app
curl -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' \
  -d '{"title":"Persist me","description":"restart test","priority":"LOW"}'
# stop the app (Ctrl+C / kill), start it again
curl 'localhost:8080/api/tickets?q=Persist'   # ticket still present
```

Optional automated variant: Testcontainers PostgreSQL with a fixed container reused across two `SpringApplication` starts in one test.

### 5.2 Secrets (AC-15)

- CI step: `gitleaks detect --no-banner` fails the build on findings.
- Review check: `git grep -iE "password|secret|token" -- ':!*.md' ':!.env.example'` returns only placeholders.

## 6. Coverage & quality gates (CI)

| Gate | Threshold |
|------|-----------|
| All backend tests (`./mvnw verify`) | pass |
| All frontend tests (`npm test`) | pass |
| State-machine IT | pass — **blocking** |
| JaCoCo line coverage, `domain` + `service` packages | ≥ 80% |
| Frontend lint + type check (`npm run lint && tsc --noEmit`) | pass |
| gitleaks | no findings |

Coverage is a floor, not a goal; the state-machine matrix is the real quality bar.

## 7. Validating AI-generated code

Every AI-generated change is reviewed against this checklist before commit:

**Spec conformance**
- [ ] Implements exactly the referenced `FR`/`BR`; no invented endpoints, fields or behaviour.
- [ ] Status codes, error codes and payload shapes match `api-contract.md`.
- [ ] State transitions only via `Ticket.transitionTo`; the transition table exists in one place.

**Correctness & safety**
- [ ] Validation annotations present on all request DTOs and `@Valid` on controller params.
- [ ] No SQL string concatenation; LIKE wildcards escaped.
- [ ] `@Transactional` on service write methods; `@Version` honoured.
- [ ] No N+1 (list endpoint does not touch lazy collections); `open-in-view: false` still works.
- [ ] No secrets, hard-coded URLs or credentials; no stack traces in error responses.
- [ ] No deprecated APIs (e.g. `javax.*` instead of `jakarta.*`, old Spring Security config) — a common AI hallucination.
- [ ] Dependencies added by the AI actually exist and are needed.

**Tests**
- [ ] Tests were written or reviewed **from the spec**, not just generated to match the code.
- [ ] At least one negative test per new behaviour.
- [ ] Mutation sanity check: temporarily break the rule (e.g. allow `CLOSED → OPEN`) and confirm a test fails. If none fails, the tests are insufficient.

**Process**
- [ ] Prompt, key AI suggestions accepted/rejected, and reasons noted in the PR description.
- [ ] Spec updated in the same PR if behaviour intentionally changed.

## 8. Debugging approach

1. Reproduce with a failing test first (API IT or MockMvc), then fix.
2. Use the Problem Details `code` + server logs (rejected transitions log at WARN with ticket id/from/to).
3. For DB issues: enable `logging.level.org.hibernate.SQL=DEBUG` locally only; inspect with `docker compose exec db psql`.
4. For frontend: check the browser Network tab for the Problem Details body before debugging UI code.
5. When asking an AI to debug, give it the failing test, the error output and the relevant spec section — not the whole codebase.
