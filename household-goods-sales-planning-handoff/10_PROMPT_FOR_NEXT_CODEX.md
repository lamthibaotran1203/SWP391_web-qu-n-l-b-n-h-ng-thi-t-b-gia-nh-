# Prompt for the Next Codex Project

Copy this folder into the implementation repository, then use the prompt below. Replace the
bracketed phase only if the team has genuinely passed its preceding release gate.

```text
You are continuing a five-student, ten-week SWP391 classroom project: Household Goods Sales
and Inventory Management for one Vietnamese store and one stock location.

Before proposing or changing anything, read the complete planning handoff in this order:
README.md, MASTER_PLAN.md, STACK_PROFILE.txt, then 00 through 09 and 11 in numeric order
(10 is this launch prompt). Do not rely on summaries alone. Treat any old SWP391
sample/reference as evidence of document structure only; never copy its domain, feature
count, code, assumptions, or defects into this project.

Planning authority flows one way:
approved decisions -> scope -> 37-UC catalogue -> BR/state model -> technical blueprint and
traceability -> RDS -> GitLab backlog/code/tests -> SDS from the final tagged implementation.
When artifacts disagree, fix the earliest authoritative fact first and propagate the change.

Frozen baseline unless a decision issue is approved:
- exactly UC-01 through UC-37 (86 relative points), with OPT-01 Promotion and OPT-02 Dashboard
  outside commitment;
- BR-01 through BR-64, 17 business tables, SCR-01 through SCR-25, QRY-01 through QRY-12,
  and NFR-01 through NFR-16;
- GitLab monorepo; Java 21 Spring Boot/MVC REST/Security/JPA/Flyway backend; React/Vite client;
  PostgreSQL hosted on Supabase;
- production is one same-origin Spring Boot deployable serving React and /api/v1; session plus
  CSRF, no JWT; React never calls Supabase directly;
- Flyway owns a lowercase app schema, Hibernate validates, PostgreSQL is used in integration
  tests, and stock/money/state effects are protected by the documented transaction/lock order;
- DELIVERY/COUNTER are order channels; COD/ONLINE/CASH are payment choices in their allowed
  contexts; failed delivery is terminal after physical return; one return request and one
  intake per order; refund is manually recorded only after actual payment to the customer.

Current working phase: [R0 requirements freeze / R1 walking skeleton / R2 core sale / R3 store
operations / R4 full baseline / R5 submission candidate / R6 defense package]. First verify
the preceding gate with concrete repository evidence. If it has not passed, repair that gate
before starting lower-priority work.

For any requested feature, begin from its row in 11_TRACEABILITY_SEED.md and its detailed UC
and BR facts. Confirm actor, trigger, observable result, normal/alternative/error flows,
authorization/ownership, screen/API contract, state and stock/money effects, transaction and
idempotency/version policy, tables/query/indexes, acceptance tests, demo fixture, owner and
reviewer. Implement a vertical slice and update RDS/traceability in the same coherent MR.

Do not expand scope because a real retailer could have more modules. Do not add Supplier/PO,
multi-warehouse, courier integration, warranty/repair, exchanges, loyalty, notifications,
accounting/e-invoice, uploads, Supabase Auth/Data API/RLS, split deployment, microservices, or
generic dashboards. Do not weaken ledgers, locks, idempotency, authorization, or adverse tests
to save time. Apply the documented capacity cut order instead.

Do not write SDS as prediction. After feature freeze, inspect the tagged real code, Flyway
migrations, repository queries, tests, pipeline, screenshots, and configuration names; use
only existing symbols and record approved deviations.

For this run, perform: [insert one concrete requested outcome]. Report the release gate checked,
files changed, tests/evidence run, traceability cells updated, remaining risks, and any decision
that genuinely requires team approval.
```
