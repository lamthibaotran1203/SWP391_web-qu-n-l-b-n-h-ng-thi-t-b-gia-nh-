# Team and Project Context

> **Archive note (2026-09-24):** This 37-UC proposal predates the 35-UC Backlog and RDS submitted to the lecturer. It is not approved scope. Read [PROJECT_STATUS.md](../PROJECT_STATUS.md) before using it.

Original draft status: **project constraint and planning authority**

## Objective

The objective is to pass SWP391 with a coherent, demonstrable, and defensible student
system. Real-world plausibility prevents absurd flows; it does not authorize production
scope. Every included capability must be implementable, testable, documented, and
explainable by five students in ten weeks.

## Team constraints

- Five students are assigned and their relative strengths are not yet proven.
- The term is ten normal weeks, including integration, documents, screenshots, demo data,
  rehearsal, and defect repair.
- Work is divided by vertical business flow, not by equal UC count or permanent
  frontend/backend silos.
- Every member must be able to explain their React screen, API, service transaction,
  database effect/query, tests, and RDS/SDS traceability.
- The first integrated COD flow is the evidence used to rebalance ownership.

## User-selected technology constraints

- Source control and teamwork: GitLab.
- Database: PostgreSQL hosted on Supabase.
- Backend: Java/Spring Boot using MVC layering.
- Frontend: ReactJS.

The planning baseline resolves these into one GitLab monorepo, Spring MVC REST APIs,
React/Vite, Spring Security session authentication with CSRF, JPA, Flyway, and one
same-origin production deployment. Exact library patch versions are pinned only when the
repository is scaffolded.

## Domain baseline

The product is a **Household Goods Sales and Inventory Management System** for one small
Vietnamese store with one physical stock location. It sells ordinary household products
and limited small appliances. Large installed appliances, installation jobs, warranty
claims, and repair workflows are not supported.

The system supports public browsing, registered-customer delivery orders, COD, one online
payment sandbox, counter sales, fulfillment, inventory control, one item-level return
request per completed order, actual refund recording, and management reports.

## Actors

| Actor | Responsibility | Boundary |
| --- | --- | --- |
| Guest | Browse, keep a browser-local cart, register, login, request password reset | Cannot order or read customer history before authentication |
| Authenticated User | Abstract actor generalized by Customer, Staff, and Manager for logout/password change | Not a persisted role and never grants authorization by itself |
| Customer | Maintain own profile/addresses, order/pay, inspect own orders/receipt, cancel eligible orders, request one eligible return | Cannot read another customer or internal operations |
| Staff | Receive stock, operate fulfillment, search orders, sell at counter, create in-store returns, receive approved returns | Cannot manage catalogue/staff, adjust stock, cancel by store, decide returns, refund, or report |
| Manager | Inherits Staff operations and manages staff/catalogue, adjustments, store cancellation, return decisions/refunds, reports | Cannot bypass lifecycle, stock, payment, refund, ownership, or audit guards |
| Payment Gateway | Secondary external system for one VND-capable sandbox | Supports UC-13; callback/IPN is not a standalone actor-goal UC |
| Email Service | Secondary external system for verification/setup/reset mail only | Delivery is a supporting flow of UC-01, UC-07, and UC-08 |

There is no Admin super-role, Supplier, Courier, Warehouse Manager, Accountant, Scheduler,
or System actor in the baseline. Jobs and callbacks are internal/supporting triggers.

## Document expectations

- Requirements and RDS are approved before feature implementation.
- SDS is produced from inspected code, migrations, routes, tests, and screenshots after
  feature freeze.
- No diagram, table, query, package, or method may be invented only to make SDS look full.
- The prior SWP391 package is a structural reference only. Its domain, feature count,
  technologies, and rules are not requirements.
- The useful course structure is preserved: overview and UCs, screen flows and
  authorization, non-UI functions, database/package overview, detailed UC specifications,
  screen design/database access, then appendix rules/statuses; SDS adds actual packages,
  tables, class/sequence designs, and queries.

## Scope classification rules

- A UC represents an external actor's goal and observable business result.
- Calculations, validation, locks, status-history insertion, stock movements, callbacks,
  expiration jobs, receipt rendering, and report SQL are steps, not separate UCs.
- Group small CRUD actions only when they share actor, authorization boundary, workspace,
  aggregate, and business outcome.
- Split flows when actor, trigger, time, authorization, transaction, evidence, or observable
  outcome differs materially.
- Equal UC counts do not imply equal effort.

## Feature admission questions

Before adding a feature, answer all of these:

1. Which external actor has the goal?
2. What observable result occurs?
3. Why is it not a step or alternative flow of an existing UC?
4. Which rules, states, screens, APIs, tables, queries, tests, demo data, RDS, SDS, and
   backlog rows change?
5. Who owns and reviews the vertical slice?
6. Which planned work loses capacity?
7. What is the safe cut if the feature fails?

If the answers are incomplete, the feature remains Future Work.
