# RDS First Research and Delivery Plan

> **Archive note (2026-09-24):** This 37-UC proposal predates the 35-UC Backlog and RDS submitted to the lecturer. It is not approved scope. Read [PROJECT_STATUS.md](../PROJECT_STATUS.md) before using it.

Original draft status: **course-aligned document contract and work sequence**

## Principle

RDS states approved required behavior and planned high-level design. Implementation must
conform to it or record an approved requirement change. SDS records the verified solution
that actually exists. The team must not fabricate SDS packages, methods, queries, diagrams,
or test results before code stabilizes.

The prior SWP391 documents are used only to recover the expected structural shape. Their
domain content, feature count, and implementation choices are not imported.

## Phase 0: planning freeze

Complete before writing 37 detailed UC descriptions:

1. Team signs the baseline rows in `07_OPEN_DECISIONS.md`.
2. Freeze UC-01 through UC-37 names/actors/priorities and the optional-feature policy.
3. Freeze BR-01 through BR-64 and the exact order/payment/return/stock states.
4. Select the payment/SMTP sandboxes and deployment path or assign a dated spike.
5. Confirm the 17-table logical model and the screen/API authorization matrix.
6. Replace A–E with member names and reviewers after the walking-skeleton exercise.

Exit evidence: signed decision log, no unresolved semantic conflict, and one traceability row
for every baseline UC.

## Phase 1: canonical planning artifacts

The handoff folder is the seed. The real project should keep a canonical `docs/planning/`
directory with these responsibilities; names may follow the team repository convention:

```text
scope-contract.md
use-case-catalog.md
business-rules.md
state-models.md
screen-and-api-catalog.md
logical-data-model.md
traceability-matrix.md
test-and-demo-plan.md
decision-log.md
backlog-export.md
```

Do not maintain duplicate editable copies. The RDS is generated/updated from these accepted
facts, while GitLab issues link back to UC/BR IDs.

Exit criteria:

- Every UC has actor, trigger, result, priority, owner, and reviewer.
- Every BR has an observable effect and test oracle.
- Every route and screen has an owning UC or is explicitly marked supporting/non-UI.
- Every table and index has a traced command/query reason.
- Every status transition has actor/guard/stock/money/history effects.
- Every acceptance test maps to one UC plus cited BRs.
- No technical task is mislabeled as a use case.

## Phase 2: RDS production

### Front matter

- Title: Requirement and Design Specification.
- Project/system name, group, course, term, version, date, authors/reviewers.
- Record of Changes with version/date/action/owner/description.
- Table of Contents and list of figures/tables when the template supports them.

### I. Overview

#### 1. User Requirements

1. Product purpose and one-store boundary.
2. Actor table, including abstract Authenticated User and secondary external systems.
3. Use-case diagrams using exactly UC-01 through UC-37.
4. UC catalogue grouped by the seven epics.
5. Optional OPT-01/OPT-02 shown only in Future Work unless formally admitted.

#### 2. Overall Functionalities

1. Guest, Customer, Staff, and Manager screen-flow diagrams.
2. Screen catalogue and purpose.
3. Screen authorization matrix.
4. API authorization matrix.
5. Non-UI/supporting functions: CSRF refresh, verification/setup/reset token delivery,
   order-code/request-key handling, price/fee calculation, stock reservation/movement,
   payment callback/IPN verification, expiry job, receipt projection, and report queries.

#### 3. System High-Level Design

1. Context/container diagram: React browser, same-origin Spring Boot, payment/email systems,
   Supabase PostgreSQL.
2. Planned MVC/package responsibilities; no speculative class names required yet.
3. Logical ERD for exactly the 17 baseline tables.
4. Database schema/table descriptions, key constraints, indexes, and delete/retention policy.
5. State diagrams and transaction/lock-order table.
6. Deployment/configuration boundary and secret ownership.

### II. Requirement Specifications

Create one detailed section for each UC in canonical order. Use this required template:

| Field | Required content |
| --- | --- |
| UC ID and Name | Exact catalogue ID/name |
| Created By / Date | Responsible member and initial date |
| Primary Actor | Exact actor/generalization |
| Secondary Actors | Payment/Email only where applicable |
| Trigger | External event that begins the goal |
| Description | Short observable business purpose |
| Preconditions | Authentication, ownership, existing state/data |
| Postconditions on success | State, stock, money, history, output |
| Minimal guarantee on failure | What remains unchanged/recorded |
| Normal Flow | Numbered actor/system interaction; no implementation class names |
| Alternative Flows | Valid alternative business paths |
| Exceptions | Integration/database/unavailable failure behavior |
| Business Rules | Exact BR IDs, not copied prose |
| Priority / Frequency | P0/P1 and defensible usage frequency |
| Acceptance Evidence | Test IDs and observable result |

Rules for authoring flows:

- Each step has an actor/system subject and one action/result.
- Alternative flow states the branch point and where it rejoins/ends.
- A failure never claims a stock/money/state effect that the transaction rolls back.
- Client-side validation may improve UX but server validation remains in the flow.
- Do not expose database tables, secret keys, hash values, or internal stack traces to actors.
- Callback/job behavior stays under its owning UC and is also listed under Non-UI Functions.

### III. Design Specifications

Group by epic, then document each routed screen from `08`:

1. Screen name and route.
2. Authorized actors and forbidden actors.
3. Wireframe or implemented screenshot clearly marked as planned/actual.
4. Field/control table: label, control type, source, required/validation, action/result.
5. API access table: HTTP method/path, request DTO, success response, expected errors.
6. Database access table: backend-owned tables, CRUD letters, and transaction/query purpose.
7. Related UC/BR/test IDs.

React never accesses a table directly. The Database Access table describes the backend
effect behind the API, preserving the course template without implying browser credentials.

### IV. Appendix

- Assumptions and external dependencies.
- Limitations and explicit exclusions.
- BR-01 through BR-64.
- Non-functional requirements NFR-01 onward from `08`.
- Order/payment/return/account status definitions.
- Money, quantity, and time definitions.
- Traceability summary.
- Glossary: Product, SKU, on-hand, reserved, available, collection, refund due, receipt.

## RDS quality gate

RDS is frozen only when:

- all IDs/names/actors match catalogue and diagrams exactly;
- all 37 use cases have complete normal/alternative/error flows;
- screens and APIs enforce the same authorization and ownership;
- stock/money effects match the state/transaction tables;
- every table and query has a traced purpose;
- three report cohorts use their correct timestamp;
- optional/future work is visibly separate from committed scope;
- no old domain names or implementation claims remain;
- the DOCX is rendered page by page and every table/diagram/TOC/page break is readable.

## Phase 3: implementation and living requirements

Build in vertical slices:

1. React/Spring/Security/Flyway/PostgreSQL walking skeleton.
2. Identity, catalogue, Product/SKU, and Inventory Overview.
3. Browser cart and COD checkout/reservation.
4. Staff order detail and fulfillment through COD completion.
5. Stock receipt/adjustment/history and counter sale.
6. Online payment/cancellation/expiry/late success.
7. Returns/receipt/disposition/refund.
8. Three reports.
9. Optional items only if admitted and core remains green.

Every feature MR includes:

- UC/BR links in the description;
- changed React route/component and API contract;
- changed service transaction and repository/migration/query;
- unit/controller/PostgreSQL/React tests as applicable;
- demo-data or repeatable setup change;
- RDS/traceability change when behavior changed;
- one reviewer outside the primary slice for high-risk commands.

If implementation reveals a requirements defect, open a decision issue. Update the source
decision/scope/BR first, then RDS, code/tests, and traceability in that order.

## Phase 4: SDS evidence freeze

Start after week-9 feature freeze. Build an evidence inventory from the repository:

- package tree and real responsibilities;
- React routes/pages/components/hooks/API clients;
- Spring controllers, DTOs, services, transaction annotations, repositories, and security;
- Flyway versions, actual tables/columns/constraints/indexes;
- SQL/JPQL/derived queries used by each UC;
- executed test classes/results and CI pipeline;
- deployed configuration keys without secret values;
- final screenshots and actual browser/API flow.

Do not use the planned class/package names from RDS as proof. Inspect the final commit.

## Phase 5: SDS production

### I. Overview

1. Actual architecture and deployment diagram.
2. Actual frontend/backend package/module descriptions.
3. Actual database schema, ERD, table descriptions, constraints, indexes, and Flyway history.
4. Actual security/session/CSRF/provider/email behavior.

### II. Code Designs

For each implemented UC-01 through UC-37, record:

1. Class/component diagram using real symbols only.
2. Class/component specifications: method or prop/hook signature and responsibility.
3. Sequence diagram from React interaction through controller/service/repository/provider.
4. Database queries: real derived method, JPQL, or SQL and its purpose.
5. Transaction/lock/idempotency behavior where applicable.
6. Implemented test/evidence references.

If several UCs share one class, document the class once in the overview and cite the exact
methods used by each UC; do not clone misleading diagrams merely to add pages.

### III. Verification and deployment

- GitLab commit/tag and pipeline ID.
- Installation/configuration guide.
- Migration and seed procedure for an empty database.
- Test summary and known limitations.
- RDS-to-SDS traceability and approved deviations.

## Document version policy

- Planning IDs never silently renumber after RDS freeze.
- Approved behavior change increments RDS minor version and updates Record of Changes.
- Feature freeze records one Git commit/tag used by SDS.
- SDS version references that exact commit/tag and Flyway migration set.
- Screenshots/diagrams include UC or screen IDs in captions, not filenames alone.
- Final exports are visually rendered and inspected; source DOCX and generated PDF are both
  retained according to the course submission rules.
