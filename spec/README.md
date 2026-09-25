# Support Ticket Management System — Specifications

This folder is the **single source of truth** for the project. Code is generated and reviewed *against* these specs, not the other way round. When behaviour changes, update the spec first, then regenerate or modify code.

## Reading order

| # | File | Purpose | Answers |
|---|------|---------|---------|
| 1 | [requirements.md](requirements.md) | What we are building and why | Features, rules, acceptance criteria, assumptions |
| 2 | [state-machine.md](state-machine.md) | Ticket lifecycle rules | Which status changes are legal |
| 3 | [data-model.md](data-model.md) | Persistent entities | Tables, columns, constraints, migrations |
| 4 | [api-contract.md](api-contract.md) | REST interface | Endpoints, payloads, status codes, error format |
| 5 | [architecture.md](architecture.md) | How the system is built | Layers, packages, tech choices, config, secrets |
| 6 | [ui-flow.md](ui-flow.md) | Frontend behaviour | Screens, navigation, error display |
| 7 | [test-strategy.md](test-strategy.md) | How we prove it works | Test levels, mandatory tests, AI-code validation |

## Conventions

- **IDs**: requirements use `FR-xx` (functional), `NFR-xx` (non-functional), `BR-xx` (business rules), `AC-xx` (acceptance criteria). Code, tests and PRs reference these IDs.
- **MUST / SHOULD / MAY** follow RFC 2119 meaning.
- Any decision not stated in the original brief is recorded as an **assumption** (`A-xx`) in `requirements.md`.

## Using these specs with AI tools

1. Load only the spec files relevant to the current task into context (e.g. `state-machine.md` + `api-contract.md` when building the transition endpoint). Do not paste the whole folder for small tasks.
2. Prompt pattern: *"Implement `<FR-xx>` as specified in `@spec/<file>.md`. Do not add behaviour not described in the spec. List any ambiguity you find instead of guessing."*
3. Project-level rules live in `.cursor/rules/` and `.github/copilot-instructions.md`, and point back to this folder.
4. Every AI-generated change is reviewed with the checklist in [test-strategy.md §7](test-strategy.md#7-validating-ai-generated-code).

## Change log

| Date | Change | Author |
|------|--------|--------|
| 2026-09-25 | v1 scope: reopen, mandatory resolution notes, comment edit/delete (§10 requirements.md); state machine 6 valid transitions | — |
| 2026-09-25 | Locked implementation decisions (§11 in requirements.md); syncticket naming; status history on detail UI | — |
| YYYY-MM-DD | Initial specification | — |
