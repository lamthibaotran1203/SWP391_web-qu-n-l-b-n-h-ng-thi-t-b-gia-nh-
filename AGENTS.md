# Project Agent Instructions

## Mission and ownership

This repository is a five-member SWP391 classroom project for a Household Goods Sales and
Inventory Management System. Optimize for a coherent, explainable, pass-ready submission,
not production-scale complexity.

The current user owns the shared project setup and only the use cases explicitly assigned to
them. Do not infer their assignment from the historical 37-UC planning pack or silently
implement another member's feature.
Read other use cases only as needed to preserve shared contracts and integration. If the
user's assigned UC list is not recorded yet, ask for it before starting feature work; this
does not block repository setup, CI, security, database, or build-foundation tasks the user
explicitly requests.

One UC owner should implement and be able to explain its full vertical slice: requirement,
React UI, API, service transaction, persistence/query, tests, demo data, and document
evidence. Do not divide one UC into disconnected frontend-only and backend-only ownership.

## Planning authority

Read `PROJECT_STATUS.md` before using planning documents. The 35-UC Google Sheet Backlog is
the current canonical register for top-level UC IDs, names, modules, actors, and iteration
ownership. Make any UC rename, actor correction, scope clarification, or small audit fix in
that Sheet first, then propagate it to diagrams, screen artifacts, RDS, code, and tests. Keep
UC-01 through UC-35 stable; supporting child IDs may be added without changing that count.

The team also submitted an RDS to the lecturer, and its live Google Doc is being edited. Use
the submitted RDS revision as comparison evidence and the live copy as work in progress. Do
not treat either copy as authority over a newer decision recorded in the canonical Backlog.
Use `docs/HomeMart_RDS_Consolidated_Review.docx` as the local review guide for the UC Diagram,
Screen Flow, ERD, physical database schema, and the lecturer-feedback audit. It describes one
UC Diagram per actor, the proposed actor-to-UC associations, and the current working
`include`/`extend` registry with arrow directions. Items labelled as proposals, working
decisions, conflicts, or open decisions still require confirmation in the canonical Backlog
before they are propagated. The Word review guide does not replace the Backlog or the live RDS.

`household-goods-sales-planning-handoff/` is an earlier, unapproved 37-UC proposal. Its UC
IDs, supplier/procurement exclusion, role model, and other scope choices conflict with the
submitted documents. Use it for ideas or technical analysis only. Do not implement a feature,
renumber UCs, or rewrite the submitted RDS solely from this pack. Its `PLAN BASELINE` labels
record draft assumptions, not team approval. `10_PROMPT_FOR_NEXT_CODEX.md` is archived and
must not be reused as current instructions.

For requirement changes, use the canonical Backlog, provenance-labelled lecturer feedback,
the exact submitted RDS revision, and explicit later team decisions. The 35 IDs are stable
traceability anchors, not a guarantee that every current name or boundary is correct. Treat
the UC Diagram feedback note as the team's transcription of oral feedback unless an original
lecturer-authored comment is available. When sources conflict, record the discrepancy and
obtain a decision on scope or behavior before propagating it through UC, BR, state, screen,
API, table/query, test, RDS, and traceability entries. Current code and migrations show what
is implemented, not the complete future scope. If the exact submitted RDS revision is
unavailable, continue useful review against the live working copy and feedback, mark its
provenance, and seek the missing revision only for conclusions that depend on the submitted
wording.

## Context-loading policy

Do not read the entire planning pack on every task. Load only the smallest relevant set:

- Repository setup or architecture: `README.md`, current code, migrations, and relevant
  historical sections of `08` only when needed.
- Feature work: the canonical Backlog row, approved owner, and current RDS revision, then relevant
  historical BR/state/API/query sections as comparison material.
- Database or RDS work: submitted artifacts, lecturer feedback, current code/migrations,
  and only the relevant draft sections for comparison.
- Planning or allocation changes: canonical Backlog, submitted/live RDS, feedback, team decisions, and
  historical `01`, `02`, `07`, `09`, and `11` as comparison material.
- SDS work: inspect the frozen implementation, migrations, routes, tests, and screenshots;
  never invent classes, methods, queries, or evidence from the plan.

When `PROJECT_STATUS.md` exists, read it immediately after this file. Keep it short and update
it after meaningful implementation work with: current release gate, the user's assigned UCs,
completed evidence, current task, next task, blockers, and exact verification commands. The
first setup task may create it; it must not claim work done by other members.

## Current implemented technical baseline

- GitLab monorepo with `backend/`, `frontend/`, `docs/`, `scripts/`, and `.gitlab-ci.yml`.
- Java 21, Maven, Spring Boot, Spring MVC REST, Spring Security, Spring Data JPA, and Flyway.
- ReactJS with Vite and React Router. Use Bootstrap plus small project CSS unless changed by
  an explicit decision; avoid Redux and large UI frameworks without a demonstrated need.
- PostgreSQL hosted on Supabase. Spring accesses it through JDBC; React never connects
  directly to Supabase.
- Flyway alone owns the lowercase `app` schema. Hibernate uses `ddl-auto=validate`; never
  manually patch Supabase as a required setup step or edit an applied migration.
- Same-origin server session plus CSRF; no JWT, Supabase Auth, browser database key, or
  production credentialed cross-origin design.
- One production Spring Boot artifact serves the React build and `/api/v1`.
- Package by feature. Preserve Controller -> Application Service -> Repository boundaries and
  DTOs at every API boundary. Transactions and authoritative rules belong in services.
- Use PostgreSQL integration tests for database behavior; H2 is not production proof.
- Vietnamese UI, validation messages, and demo data; English code symbols, schema, endpoint
  names, IDs, RDS, and SDS unless the course template requires otherwise.

These are implementation constraints, not proof of a finished RDS/SDS. The 17 table entries
are logical contracts; exact columns, nullability, lengths, constraints, indexes, and mappings
must be finalized through RDS and executed Flyway migrations before they are treated as the
physical database design.

## Walking-skeleton acceptance

Repository setup is not complete merely because both projects compile. The R1 skeleton must
provide one visible, non-fabricated browser-to-database proof without pretending that a
business UC is complete:

1. React starts and calls Spring through the Vite `/api` proxy in development.
2. React obtains the real CSRF state from `GET /api/v1/auth/csrf` and renders a clear
   Vietnamese connected/error state from the server response.
3. Flyway creates the `app` schema in PostgreSQL and Hibernate validation succeeds.
4. An environment-provided bootstrap Manager can log in through the React form; Spring reads
   the account from PostgreSQL, creates the session, and `GET /api/v1/auth/me` returns the
   authenticated safe projection.
5. React performs CSRF-protected logout, refreshes CSRF state, and the next protected/current
   session check shows the logged-out result.
6. Restarting the applications preserves the database account while no password, database
   URL, token, or secret appears in Git, browser storage, or rendered output.

Capture exact run commands and the expected visible result in the root README. Keep any
temporary diagnostics local/test-only; do not add a permanent business-looking status API or
claim UC-01 through UC-08 complete from this thin identity proof.

## Mac and Windows compatibility

The primary developer uses macOS and other members use Windows. Shared setup and required
commands must work on both:

- Commit Maven Wrapper for Unix and Windows: `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/`.
- Use OS-neutral `npm` scripts for frontend commands. Do not make Bash, Make, Homebrew,
  SDKMAN, Unix paths, executable bits, symlinks, or inline POSIX environment assignment a
  required workflow.
- If orchestration is needed, prefer a small Node script; otherwise document equivalent
  macOS/Linux and PowerShell commands. Optional convenience `.sh` scripts must have a `.ps1`
  or OS-neutral alternative.
- Pin Java, Maven-wrapper, Node, npm, and PostgreSQL major versions in repository-visible
  files and use the same major versions in GitLab CI.
- Add `.gitattributes` and `.editorconfig` so source files use consistent UTF-8/LF while
  Windows command files retain appropriate line endings.
- Use `docker compose` syntax rather than the legacy `docker-compose`. If local PostgreSQL via
  Docker is approved for DEC-28, keep the Compose file, health check, database name, port, and
  safe development credentials reproducible on Docker Desktop for both operating systems.
- Never hard-code `/Users/...`, drive letters, path separators, or a developer's Supabase
  credentials. All documented commands start from the repository root.
- The root README must contain separate macOS/Linux and Windows PowerShell prerequisite and
  startup snippets, followed by one identical browser verification flow.

Product SKUs are manually entered sale units such as a model, capacity, or package label.
Do not build a generic clothing-style size/color variant engine. Supplier/procurement scope
is unresolved because the submitted 35-UC Backlog includes it while the historical 37-UC
pack excludes it. Do not add unapproved business modules or silently remove submitted ones.

## Implementation workflow

For feature work, handle one assigned UC vertical slice at a time:

1. Confirm actor, trigger, outcome, preconditions, flows, BRs, authorization, states,
   screen/API, tables/queries, acceptance tests, dependencies, and ownership.
2. State the intended files and invariants before broad edits.
3. Implement the smallest complete slice. Reuse established conventions and avoid speculative
   abstractions, generic engines, or tables without a traceable requirement.
4. Verify normal flow and the most important failure, permission, duplicate, stale-state, or
   concurrency path appropriate to the UC.
5. Update traceability/RDS evidence in the same change when behavior or contracts changed.
6. Update `PROJECT_STATUS.md` and report changed files, commands run, results, and remaining
   risks. Do not call a UC done from screenshots or happy-path code alone.

Do not begin an unassigned UC merely because it is the next item in the global backlog. It is
acceptable to add a minimal shared interface, fixture, migration prerequisite, or integration
stub required by the user's assigned slice, but call out that boundary and do not complete the
other member's business flow.

For stock, money, payment, fulfillment, return, and report logic, preserve the documented
state guards, idempotency/version rules, lock order, immutable ledgers, time boundaries, and
reconciliation equations. Never weaken an invariant just to make a test pass.

## Agent use

The primary agent owns decisions, edits, integration, and the final verification. Use at most
two subagents at once, and only when independent exploration or review will materially save
time. Good subagent tasks are read-only contract extraction, test-gap review, migration
review, or frontend/backend review of an already bounded UC.

Do not let parallel agents edit the same files or independently change shared migrations,
security configuration, API error contracts, state machines, or public DTOs. Do not delegate
one coupled feature to multiple writers. The primary agent must reconcile every finding
against the current documented decisions before applying it.

## Repository hygiene and completion

- Inspect existing files and Git state before editing; preserve unrelated user/team changes.
- Never commit secrets. Add safe names and placeholders to `.env.example` or documentation.
- Do not add dependencies, tables, endpoints, or broad refactors without a concrete need.
- Do not commit, push, merge, rewrite history, or modify GitLab settings unless requested.
- Keep `master` buildable. Run the narrowest relevant checks during iteration and the affected
  frontend, backend, PostgreSQL, build, and smoke checks before declaring shared setup done.
- A fresh clone must eventually be able to migrate an empty PostgreSQL database, seed demo
  data, test, package, start, and execute the documented core paths without manual database
  editing.
- Be explicit about incomplete integration owned by teammates. Never fabricate their code,
  contribution history, reviews, pipeline links, screenshots, RDS evidence, or SDS symbols.
