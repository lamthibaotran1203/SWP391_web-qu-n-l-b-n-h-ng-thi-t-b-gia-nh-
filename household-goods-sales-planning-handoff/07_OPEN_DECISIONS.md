# Decision Register Before RDS Freeze

> **Archive note (2026-09-24):** This 37-UC proposal predates the 35-UC Backlog and RDS submitted to the lecturer. It is not approved scope. Read [PROJECT_STATUS.md](../PROJECT_STATUS.md) before using it.

Original draft status: **recommended defaults; team signatures still required**

`PLAN BASELINE` means this pack consistently assumes the decision. `CONFIRM W1` means an
external service/account/host choice must be confirmed by the end of week 1. A changed
decision requires impact analysis before the RDS is updated.

| ID | Decision | Recommended baseline | Status | Consequence if changed |
| --- | --- | --- | --- | --- |
| DEC-01 | Product boundary | Ordinary household goods and limited small appliances; no installation | PLAN BASELINE | Large appliances add scheduling, delivery, installation, and warranty scope |
| DEC-02 | Checkout identity | Active verified Customer required for delivery order | PLAN BASELINE | Guest checkout changes ownership, address, history, cancellation, and return rules |
| DEC-03 | Account email | One SMTP-compatible sandbox for verification, Staff setup, and reset only | CONFIRM W1 | Removing email requires redesigned activation/setup/reset flows; never fake delivery |
| DEC-04 | Online payment | One VND-capable sandbox; recommend VNPAY | CONFIRM W1 | Another provider must still support signed result, exact amount, unique reference, and test credentials |
| DEC-05 | Payment methods | Delivery: COD or ONLINE; counter: CASH only | PLAN BASELINE | Split/tender/multiple providers multiply ledger and UI/test states |
| DEC-06 | Cart ownership | Browser-local cart; no cart table or server merge | PLAN BASELINE | Persisted cart adds ownership, merge, expiry, API, and table scope |
| DEC-07 | Promotion | Outside baseline as OPT-01 | PLAN BASELINE | Admission adds two tables and affects catalogue, checkout, counter sale, returns, reports, tests |
| DEC-08 | Delivery model | One local workflow updated by Staff; no Courier actor/API | PLAN BASELINE | Courier integration adds assignment, tracking, callback, and authorization rules |
| DEC-09 | Delivery fee | One fixed server-owned VND fee snapshotted on order | PLAN BASELINE | Zone/weight rules require configuration and address/rate tests |
| DEC-10 | Failed delivery | Record only after whole parcel returns; terminal `DELIVERY_FAILED`; no redispatch | PLAN BASELINE | Redispatch adds re-reservation and repeat dispatch lifecycle |
| DEC-11 | Counter customer | Account/contact optional; order/receipt code is return evidence | PLAN BASELINE | Required account harms walk-in flow; weaker evidence harms return authorization |
| DEC-12 | Return window | Seven calendar days from order completion | PLAN BASELINE | Different fixed window is small; configurable exceptions are major scope |
| DEC-13 | Return reasons | Defective, wrong item, shipping damage | PLAN BASELINE | Change-of-mind needs restocking/condition/fee rules |
| DEC-14 | Return granularity | Multi-line item and quantity level | PLAN BASELINE | Full-order-only is shallow; serial/batch return is too broad |
| DEC-15 | Return count | At most one Return Request per completed order | PLAN BASELINE | Multiple requests require cumulative allocation and race controls |
| DEC-16 | Return decision/intake | Approve all or reject; one physical intake; disposition quantities per line | PLAN BASELINE | Partial approval/multi-intake greatly expands status and quantity matrix |
| DEC-17 | Delivery-fee return refund | Not refundable through return workflow | PLAN BASELINE | Conditional fee refund adds full-order/reason/cumulative exception logic |
| DEC-18 | Refund execution | Manager records exact cash/manual-bank-transfer refund; no provider refund API | PLAN BASELINE | Provider refunds add external failure/idempotency/reconciliation states |
| DEC-19 | Stock sourcing | Receipt stores source name/reference; no Supplier/Purchase Order | PLAN BASELINE | Procurement is a separate module and table/state family |
| DEC-20 | Staff/Manager boundary | Manager inherits Staff; no Admin super-role | PLAN BASELINE | Another role changes diagrams, security matrix, seed data, and all privileged UCs |
| DEC-21 | Reporting | Separate cash, completed-sales, and inventory roll-forward reports | PLAN BASELINE | Combining loses timestamp/cohort meaning and testability |
| DEC-22 | Dashboard | Outside baseline as OPT-02 | PLAN BASELINE | Admission adds current-count queries/screen but no new tables |
| DEC-23 | Application stack | GitLab, Java 21, Spring Boot/MVC REST, React/Vite, JPA, Flyway, Supabase PostgreSQL | PLAN BASELINE | Framework/auth/deployment switch reopens architecture, CI, RDS/SDS, and workload |
| DEC-24 | Authentication/deployment | Same-origin Spring session + CSRF; React build served by Spring Boot | PLAN BASELINE | JWT or split origin adds token/cookie/CORS/refresh/revocation design |
| DEC-25 | Supabase use | Managed PostgreSQL only, custom `app` schema, backend JDBC only | PLAN BASELINE | Direct client access requires Supabase Auth/RLS and a second authorization model |
| DEC-26 | Product image | One validated HTTPS image URL per Product | PLAN BASELINE | Upload/storage service adds file validation, lifecycle, credentials, and deployment work |
| DEC-27 | Runtime DB connection | Direct IPv6 connection when available; otherwise Supavisor session mode 5432; SSL required | CONFIRM W1 | Transaction pooler 6543 conflicts with normal Hibernate prepared-statement behavior |
| DEC-28 | Local/CI database | PostgreSQL only: local container or dev project; GitLab CI PostgreSQL service | CONFIRM W1 | H2 can hide PostgreSQL constraints/locking/query differences |
| DEC-29 | Deployment host | One container/JVM host serving Spring + React; Supabase remains external DB | CONFIRM W1 | Separate frontend/serverless host reopens same-origin and connection decisions |
| DEC-30 | Team ownership | Use A–E mapping initially; replace after walking-skeleton evidence | CONFIRM W1 | Premature fixed assignment can overload high-risk modules |
| DEC-31 | Channel vocabulary | Order: DELIVERY/COUNTER; return request: CUSTOMER/IN_STORE; payment method is separate | PLAN BASELINE | Reusing ONLINE for channel and method makes reports, checks, and flows ambiguous |
| DEC-32 | Expiry windows | Pending ONLINE payment/reservation 15 min; verification/setup token 24 h; reset token 30 min | PLAN BASELINE | Different values require BR/config/test/demo updates but no new feature |
| DEC-33 | Catalogue activation | Active Product requires active Category, Brand, and active SKU; referenced active classifications cannot be deactivated | PLAN BASELINE | Cascade/hide alternatives change catalogue queries and administration errors |
| DEC-34 | Delivery fee | Fixed 30,000 VND for every DELIVERY order; zero for COUNTER | PLAN BASELINE | A changed fixed value affects config/tests/demo only; zone/weight rules are out of scope |
| DEC-35 | Session lifetime | 30-minute idle timeout, no remember-me; credential/status changes invalidate user sessions | PLAN BASELINE | Persistent/distributed sessions add storage and lifecycle work |
| DEC-36 | Command size | Sale/cart max 50 SKUs and 999/line; receipt max 100 SKUs and 999,999/line | PLAN BASELINE | Unbounded commands make validation, locks, payloads, and tests unsafe |
| DEC-37 | First Manager | Idempotent environment-driven bootstrap; no public registration or committed default password | PLAN BASELINE | Manual Table Editor setup breaks reproducibility and credential handling |
| DEC-38 | PostgreSQL major | Pin local/CI PostgreSQL to the Supabase project major at R1 | CONFIRM W1 | A mismatched test engine can hide DDL/query/locking behavior |

## Required week-1 proof for external choices

| Choice | Proof before acceptance |
| --- | --- |
| SMTP sandbox | One real message can be captured; credentials are not committed; failure is observable |
| Payment sandbox | Test merchant credentials exist; success/failure data and signature verification are reproducible; public callback/IPN can reach the app when needed |
| Supabase connection | Fresh Spring profile connects over SSL, Flyway creates `app`, and Hibernate validates |
| Deployment host | Can run one Spring Boot artifact, inject masked variables, expose HTTPS, and reach Supabase |
| Member mapping | Each member completes/reviews part of the walking skeleton before high-risk ownership is frozen |

## Questions every member must answer aloud

1. Why is Product different from SKU?
2. Why does cart state not reserve stock, and why is every cart value revalidated?
3. When do on-hand and reserved quantities change in each order transition?
4. What prevents two simultaneous checkouts or duplicate submit from overselling/duplicating?
5. What happens when a valid payment result is repeated or arrives after cancellation?
6. Why does delivery failure become terminal only after the parcel returns?
7. Why does a completed order remain completed during a return?
8. Why are approval, physical intake, and refund separate?
9. Why are cash, sales, and inventory reports based on different timestamps?
10. Why does React never connect directly to Supabase?
11. Why is session + CSRF appropriate even though the frontend is React?
12. Which two features are optional, and what evidence permits their admission?
13. How does a UC trace to screen, API, service, table/query, test, GitLab issue, RDS, and SDS?

## Decision change protocol

For any changed row, record proposer/date/reason and list affected UCs, BRs, statuses,
screens, endpoints, tables/indexes, tests, backlog/owner load, RDS sections, and demo paths.
The change is rejected if it has no capacity offset or leaves an artifact inconsistent.
