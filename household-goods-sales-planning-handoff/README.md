# Household Goods Sales Planning Handoff

Status: **decision-ready SWP391 baseline v2.0**

Current commitment: **37 baseline actor-goal use cases and 2 optional features**

## Start here

This folder is the planning source for a five-student, ten-week SWP391 classroom project.
It describes one small Vietnamese household-goods store, not a production retailer or an
open-ended e-commerce platform.

Read `MASTER_PLAN.md` first, then the numbered files:

1. `00_TEAM_AND_PROJECT_CONTEXT.md`
2. `01_SCOPE_CONTRACT_DRAFT.md`
3. `02_USE_CASE_CATALOG_DRAFT.md`
4. `03_BUSINESS_RULES_DRAFT.md`
5. `04_STATE_AND_TRANSACTION_MODEL_DRAFT.md`
6. `05_SCOPE_AUDIT.md`
7. `06_RDS_FIRST_RESEARCH_PLAN.md`
8. `07_OPEN_DECISIONS.md`
9. `08_TECHNICAL_FOUNDATION_BLUEPRINT.md`
10. `09_WORK_BREAKDOWN_AND_BACKLOG_SEED.md`
11. `11_TRACEABILITY_SEED.md`

`10_PROMPT_FOR_NEXT_CODEX.md` is only a handoff prompt. It does not add requirements.

## Authority

| Artifact | Authority |
| --- | --- |
| Team-approved rows in `07` | Accepted project decisions |
| `01` | Product boundary, committed scope, and exclusions |
| `02` | Canonical UC IDs, names, actors, priority, and count |
| `03` and `04` | Domain rules, states, formulas, stock/money effects, and transaction guards |
| `08` | Architecture, screen/API/data contracts, security, CI, and test foundation |
| `11` | Cross-artifact traceability seed; it must follow the sources above |
| `09` | Work allocation and backlog seed, not a source of new behavior |

If two files conflict, do not choose the more convenient sentence. Correct the earliest
authoritative source, then propagate the approved change downstream.

## Technical direction

- GitLab monorepo with protected `main`, merge requests, issue board, and CI.
- Java 21, Maven, and one Spring Boot deployable; exact compatible versions are pinned at R1.
- Spring MVC REST controllers, Spring Security, Spring Data JPA, and Flyway.
- React with Vite and React Router; production assets are served by Spring Boot.
- Session-cookie authentication and CSRF; no JWT/local-storage access token baseline.
- PostgreSQL hosted on Supabase in a project-owned `app` schema.
- React never connects directly to Supabase; no Supabase Auth, Data API, Realtime, or
  Storage dependency in the baseline.
- PostgreSQL integration tests; no H2 claim as proof of production behavior.
- Vietnamese UI/errors/demo data; English code, schema, endpoints, IDs, RDS, and SDS.

The plan intentionally chooses one deployment origin. Vite proxies `/api` during local
development; the submitted build packages the React `dist` output with Spring Boot. This
keeps the React requirement while avoiding a second deployed frontend, cross-site cookies,
and unnecessary JWT revocation logic.

## Course-document shape used as reference

The prior SWP391 package is used only to understand deliverable structure. The target RDS
keeps the useful course shape: actors/use cases and diagrams, screen flow/descriptions/
authorization, non-UI functions, high-level database/package design, detailed UC
specifications, screen designs and database access, then assumptions/rules/statuses. The
later SDS records real packages/database and, per implemented UC, real class specifications,
sequence flow, and queries. No old football-domain behavior is imported.

## Scope control

Promotion and Operational Dashboard are optional. Supplier/procurement, multi-location
stock, courier integration, warranty/repair, exchange, loyalty, review, notification,
policy engines, AI, mobile, microservices, and direct browser-to-Supabase access are excluded.

Before adding anything, identify its actor, trigger, observable result, rules, states,
screen/API, tables/queries, tests, owner, lost schedule capacity, and exact traceability
changes. If any answer is missing, keep it out.

## Completion rule

A UC is complete only when these agree:

```text
UC and BRs
-> RDS flow and screen authorization
-> React route and API contract
-> service transaction and lock order
-> Flyway tables/constraints/indexes and repository query
-> automated tests and demo data
-> GitLab issue/MR evidence
-> later SDS symbols and diagrams
```
