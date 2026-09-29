# Household Goods Sales SWP391 Master Plan

> **Archive note (2026-09-24):** This 37-UC proposal predates the 35-UC Backlog and RDS submitted to the lecturer. It is not approved scope. Read [PROJECT_STATUS.md](../PROJECT_STATUS.md) before using it.

Original draft status: **decision-ready planning baseline v2.0**

Audience: five-student SWP391 team, project mentor, and future implementation/document agents

## Executive decision

Build one focused **Household Goods Sales and Inventory Management System** for one
Vietnamese store and one stock location. The recommended commitment is:

- **37 baseline actor-goal use cases** across seven business epics;
- **2 optional features** that may enter only after the COD vertical slice is stable;
- **17 planned business tables** with no speculative module tables;
- **one GitLab repository and one deployable Spring Boot application** containing the
  production React build;
- PostgreSQL hosted on Supabase, with Flyway as the only DDL owner;
- Spring MVC REST controllers, service transactions, Spring Data JPA, Spring Security
  session authentication, and a React/Vite client;
- RDS frozen before feature implementation; SDS generated only from real code, migrations,
  API routes, tests, and screenshots.

The baseline is deliberately smaller than the previous 41-UC proposal because switching
from Thymeleaf to React adds API contracts, client state, CSRF integration, frontend tests,
and build integration. Scope was reduced without hollowing out the domain:

1. Promotion management is optional, not promised.
2. Operational dashboard is optional; baseline queues and reports already expose the work.
3. Receipt viewing is part of customer/staff order detail, not a separate UC.
4. The fulfillment queue is a saved actionable filter in staff order search, not a separate UC.
5. Product SKUs are manually entered sale units, not a generic size/color variant engine.
6. Failed delivery is terminal after the parcel returns; no redispatch loop exists.
7. Each completed order has at most one item-level return request.
8. Delivery fee is not refunded in the baseline.

## Product boundary

The system supports public catalogue browsing, customer accounts, browser-local cart,
delivery checkout, COD, one VND-capable payment sandbox, staff fulfillment, counter sales,
stock receipt/adjustment/audit, item-level return intake, manual refund recording, and
three management reports.

`DELIVERY` and `COUNTER` are sale channels; COD/ONLINE/CASH are separate allowed payment
methods. The baseline fixes delivery fee at 30,000 VND, ONLINE reservation at 15 minutes,
return eligibility at the instant before `completed_at + 7 days`, and idle session timeout
at 30 minutes. These values close test boundaries without introducing configuration screens.

It does not implement procurement, suppliers, purchase orders, multi-warehouse stock,
courier accounts or APIs, installation, warranty repair, exchanges, loyalty, reviews,
notifications, configurable policy engines, accounting, e-invoices, AI, chat, or mobile
apps. Supabase is used as managed PostgreSQL only; the React client never talks directly
to Supabase.

## Source-of-truth chain

Planning authority flows in one direction:

```text
Team-approved decisions
-> Scope Contract
-> Use Case Catalogue
-> Business Rules and state models
-> Technical foundation and traceability seed
-> RDS
-> GitLab backlog and implementation
-> verified code/migrations/tests/screens
-> SDS
```

When artifacts disagree, fix the earliest authoritative artifact first. A downstream file
must never silently redefine an upstream rule. Every approved change records its affected
UCs, BRs, screens, API routes, tables, queries, tests, backlog items, RDS sections, and SDS
evidence.

## Target architecture

```text
Browser
  React + Vite + React Router
  local cart and UI-only route guards
        |
        | same-origin HTTPS /api/v1
        v
Spring Boot
  Security filter chain and CSRF
  REST Controller -> Application Service -> Repository
  DTOs at the API boundary; transactions in services
        |
        | JDBC over SSL, small Hikari pool
        v
Supabase PostgreSQL
  app schema owned by Flyway migrations
  constraints, indexes, row locks, immutable ledgers
```

During development, Vite proxies `/api` to Spring Boot. For the submitted/deployed build,
the React bundle is served by Spring Boot, so authentication remains same-origin and the
team does not need cross-site cookies, a second deployment, or JWT revocation logic.

## Release plan

| Release gate | Target | Required proof |
| --- | --- | --- |
| R0 Requirements freeze | End week 1 | Decisions confirmed; 37 UCs, 64 BRs, states, exclusions, and cut policy accepted |
| R1 Executable skeleton | End week 2 | GitLab pipeline green; React calls authenticated Spring API; Flyway runs against PostgreSQL |
| R2 Core sale | End week 4 | Catalogue -> cart -> COD order -> reservation works with rollback and concurrency tests |
| R3 Store operations | End week 6 | Pack -> dispatch -> delivery outcome, stock receipt/adjustment, and counter sale reconcile |
| R4 Complete baseline | End week 8 | Online payment, returns/refunds, and three reports pass integration tests |
| R5 Submission candidate | End week 9 | Feature freeze; RDS/code/backlog traceability and repeatable demo stable |
| R6 Defense package | End week 10 | SDS from real implementation, installation guide, screenshots, rehearsal, and final QA |

No optional feature may start before R2 passes. No new business feature may start after R4.

## Critical implementation path

1. Freeze actors, statuses, money/stock formulas, provider choice, and exclusions.
2. Establish GitLab rules, monorepo layout, CI, environment profiles, Flyway baseline, and
   the React-to-Spring same-origin security proof.
3. Implement catalogue/product/SKU and current inventory because all sales depend on them.
4. Implement browser cart and transactional COD checkout with stable SKU lock ordering.
5. Implement staff order search/detail with an actionable filter and fulfillment through COD collection/completion.
6. Add counter sale and stock receipt/adjustment; reconcile balances to movement rows.
7. Integrate one online payment sandbox and prove signature, exact amount, duplicate result,
   cancellation, expiry, and late-success behavior.
8. Implement the single-request item-level return and manual refund flow.
9. Build reports from payment/order/movement timestamps, not copied dashboard totals.
10. Freeze behavior, repair traceability, generate SDS, and rehearse normal plus failure paths.

## Ten-week operating plan

| Week | Product outcome | Document/evidence outcome |
| --- | --- | --- |
| 1 | No feature coding beyond spikes | Decisions, scope, UCs, BRs, states, screens, and RDS skeleton frozen |
| 2 | Auth/API/React/PostgreSQL walking skeleton | CI proof, environment guide, baseline ERD, endpoint and authorization matrix |
| 3 | Account, catalogue, product/SKU, inventory reads | First reviewed RDS sections and trace rows for implemented slices |
| 4 | COD checkout and reservation vertical slice | Transaction/lock tests, screenshots, demo data, RDS change audit |
| 5 | Staff order search/detail with actionable filter, packing, dispatch | Order-state sequence evidence and movement reconciliation |
| 6 | Delivery outcome, COD collection, counter sale, stock commands | Core demo rehearsal and R2/R3 gate review |
| 7 | Online payment, cancellation, expiry, late callback | Provider evidence, idempotency tests, secret/config guide |
| 8 | Returns/refunds and three reports | Report reconciliation, authorization matrix completion, R4 gate |
| 9 | Integration defects and feature freeze only | Final RDS alignment, traceability audit, SDS extraction starts |
| 10 | No scope work | SDS, release/install guide, render QA, demo script, defense rehearsal |

## Five-person ownership model

Ownership is by vertical slice. A lead owns requirement, React screen, API, service,
database/query, tests, demo data, and defense explanation for each assigned UC. Cross-module
reviewers verify effects on stock, money, authorization, and status.

| Lead | Primary responsibility | Baseline UCs | Relative load |
| --- | --- | --- | ---: |
| A | Identity, profile/address, customer order list | UC-01–08, UC-14 | 16 |
| B | Catalogue, cart, product admin, customer detail/return, sales report | UC-09–11, UC-15, UC-19–20, UC-30, UC-36 | 18 |
| C | Checkout, payment, cancellations, refunds, cash report | UC-12–13, UC-16, UC-28, UC-34–36 | 18 |
| D | Inventory, stock receipt/adjustment, counter sale/intake, inventory report | UC-21–24, UC-29, UC-31, UC-37 | 18 |
| E | Staff order operations, fulfillment, return review/receipt | UC-17–18, UC-25–27, UC-32–33 | 16 |

Replace A–E with names only after the team demonstrates capability in the walking skeleton.
Pair C/D on stock-money races and B/E on return/order projections.

## Submission working set

The prior course package influenced only this deliverable shape. Final filenames follow the
current lecturer/template, but the team maintains one source per fact.

| Artifact | Minimum content | Freeze/evidence rule |
| --- | --- | --- |
| Planning pack | scope, UC/BR/state/data/API/NFR decisions, backlog, traceability | accepted at R0; changes use decision protocol |
| RDS source + rendered PDF | course front matter; diagrams; 37 detailed UCs; 25 routed screens; API/data access; assumptions/rules/statuses | behavior authority before implementation; page-by-page render QA at R5 |
| Diagram sources | use-case, actor screen flows, architecture, ERD, states, key sequences | editable source plus readable export; IDs match RDS |
| GitLab backlog/export | issues, owners/reviewers, milestones, dependencies, evidence links | GitLab is live truth; dated XLSX only when course requires |
| Tagged source release | frontend/backend, Flyway, tests, seed, CI, safe config sample | clean R5 tag; no secret or manual database prerequisite |
| SDS source + rendered PDF | actual packages/classes/methods/sequences/queries/schema/tests from release tag | starts after feature freeze; no predicted symbol |
| Installation guide | prerequisites, environment names, Supabase connection, migrate/seed/build/run/troubleshoot | reproduced from a clean environment by a second member |
| User manual | Guest/Customer/Staff/Manager paths, screenshots, validation/conflict recovery | screenshots come from release candidate and contain no secrets |
| Verification/demo evidence | pipeline result, test summary, four core scripts, selected adverse/race cases, known exclusions | links to immutable tag/pipeline and repeatable demo data |

## Quality gates

### Definition of Ready

A feature may enter implementation only when its actor, trigger, result, preconditions,
normal/alternative flows, BR references, authorized screen, endpoint, request/response,
transaction boundary, table/query impact, acceptance tests, demo data, and owner are known.

### Definition of Done

A feature is done only when:

1. RDS behavior and implemented behavior agree.
2. Backend authorization and ownership reject a tampered request.
3. Validation and conflict responses are deterministic and shown safely in Vietnamese.
4. Mutations are transactionally guarded and duplicate submissions do not duplicate effects.
5. Flyway constraints/indexes and JPA mappings agree; Hibernate runs in validate mode.
6. Unit, controller, PostgreSQL integration, and relevant React tests pass.
7. Demo data proves at least one normal and one important alternative path.
8. GitLab MR is reviewed by a member who can explain the downstream effect.
9. Traceability and RDS are updated in the same MR.
10. SDS evidence records real symbols only after code stabilizes.

## Top risks and controls

| Risk | Early proof | Control |
| --- | --- | --- |
| React/Spring authentication mismatch | Week-2 login/logout/CSRF smoke | Same-origin production, session cookie, explicit fresh CSRF handling |
| Overselling or stock drift | Two concurrent COD checkouts | Lock SKU rows by ascending ID and write balance plus movement atomically |
| Duplicate order/counter sale | Retry same key and reuse it with changed content | Persist actor-scoped key plus server fingerprint; return original or 409 |
| Forged or duplicate payment result | Invalid signature and duplicate provider reference tests | Verify signature/reference/amount; unique provider transaction reference |
| Payment success after cancellation/expiry | Controlled race test | Record collection once, never reopen order, expose full refund due |
| Fulfillment and stock disagree | Dispatch/failure tests | Fixed state transitions and exact movement deltas in one transaction |
| Return quantity/refund mismatch | Multi-item/partial-intake tests | One return per order; received cap; refund from sold-price snapshots |
| Report totals conflict | Fixed-clock reconciliation fixtures | Distinct transaction, completion, and movement cohorts |
| Supabase connection exhaustion | CI/deploy smoke and pool metrics | Direct/session-pooler JDBC, SSL, Hikari max pool kept small |
| Documentation diverges | MR traceability check | No merge when UC/BR/API/table/test references are stale |

## Capacity policy

The first safe response to schedule pressure is to omit optional Promotion and Dashboard;
they are not in the 37-UC commitment. If further reduction is unavoidable, use this order:

1. Keep online payment but postpone password reset to a documented post-baseline increment.
2. Reduce report presentation to tables/export-free views; keep all three query meanings.
3. Remove in-store initiation for counter returns while retaining customer online returns.

Do not cut stock movements, transactional checkout, staff order enquiry, fulfillment state
guards, item-level return quantities, actual refund recording, or authorization tests. Those
are the coherence of the system, not polish.

## First team session checklist

1. Confirm the 37-UC baseline and explicit exclusions.
2. Select one online provider; VNPAY sandbox is the recommended VND default.
3. Select one SMTP sandbox and one deployment host; record credentials only in GitLab
   protected/masked variables and local ignored environment files.
4. Map A–E to members and assign one reviewer per high-risk slice.
5. Create the GitLab repository, protect `main`, prohibit direct pushes, and require a green
   pipeline plus one peer review for merge.
6. Create the RDS skeleton using the section contract in `06_RDS_FIRST_RESEARCH_PLAN.md`.
7. Build the week-2 security/database walking skeleton before opening feature branches.

## Completion statement

The project is submission-ready only when a fresh clone can build, migrate an empty
PostgreSQL database, seed demo data, run tests, start the application, execute the four core
demo paths, and reproduce RDS/SDS claims without manual database editing.
