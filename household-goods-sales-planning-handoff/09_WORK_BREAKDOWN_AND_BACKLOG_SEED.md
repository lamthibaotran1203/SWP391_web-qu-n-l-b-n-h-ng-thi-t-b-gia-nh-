# Work Breakdown and GitLab Backlog Seed

> **Archive note (2026-09-24):** This 37-UC proposal predates the 35-UC Backlog and RDS submitted to the lecturer. It is not approved scope. Read [PROJECT_STATUS.md](../PROJECT_STATUS.md) before using it.

Original draft status: **canonical 37-UC planning seed; team dates and names pending**

## 1. Purpose and authority

This file converts the accepted scope into work that five students can own, demonstrate,
review, and defend. It is not permission to start all features at once and it is not a fixed
calendar until the team replaces semester Week 1–10 with actual dates.

Use these rules:

- Feature count is exactly UC-01 through UC-37. OPT-01/OPT-02 stay outside the backlog
  commitment unless the admission gate in `01_SCOPE_CONTRACT_DRAFT.md` passes.
- Effort points compare risk only: Low = 1, Medium = 2, High = 3. They are not hours,
  grades, velocity promises, or a reason to pad use cases.
- One lead owns the vertical result: requirement, React UI, API, service, persistence/query,
  tests, demo data, RDS update, and defense explanation. Tasks may be paired, but a UC is
  never split into unrelated “frontend owner” and “backend owner”.
- The reviewer must understand downstream effects and must not approve from screenshots
  alone. C/D cross-review stock-money commands; B/E cross-review return/order projections.
- A feature reaches `Ready` only after the Definition of Ready in `MASTER_PLAN.md` passes.
  It reaches `Done` only after every applicable Definition of Done item passes.

The baseline contains **86 relative points**. Equal UC counts would therefore be misleading.

## 2. Five-person ownership baseline

Keep A–E until the Week-2 walking skeleton shows actual capability. Then replace labels with
names without changing UC IDs. Rebalance whole UCs and their evidence, never isolated layers.

| Lead | Vertical responsibility | Baseline UCs | Points | Primary integration reviewers |
| --- | --- | --- | ---: | --- |
| A | Identity, profile/address, Customer order list | UC-01–08, UC-14 | 16 | B for Customer UX; C for session/order boundaries |
| B | Catalogue/cart, product admin, Customer detail/return, sales report | UC-09–11, UC-15, UC-19–20, UC-30, UC-36 | 18 | A for ownership; D for SKU; E for returns |
| C | Checkout/payment, both cancellations, refunds, cash report | UC-12–13, UC-16, UC-28, UC-34–36 | 18 | D for stock locks; E for lifecycle actions |
| D | Inventory commands/views, counter sale/in-store return, inventory report | UC-21–24, UC-29, UC-31, UC-37 | 18 | C for money/idempotency; E for intake |
| E | Staff order enquiry, fulfillment, return decision/receipt | UC-17–18, UC-25–27, UC-32–33 | 16 | B for projections; C/D for money and stock |
| **Total** |  | **37 UCs** | **86** |  |

Member mapping is accepted only when every person has built or reviewed one React-to-Spring-
to-PostgreSQL path and can answer the questions in `07_OPEN_DECISIONS.md`.

## 3. Backlog hierarchy and workflow

```text
Epic       = one coherent business capability group
Feature    = one approved actor-goal UC
Task       = one testable implementation/document increment under a UC or cross-cutting proof
Defect     = observed mismatch against an accepted UC/BR/NFR/test oracle
Change     = proposed requirement change; never silently implemented as a defect
```

A `Manage` UC can contain list/create/edit/activate tasks without inventing extra UCs.
Payment callbacks, expiry jobs, history rows, stock movements, receipt projection, and report
SQL are supporting tasks under the actor goal that owns their outcome.

Recommended GitLab board states:

```text
Proposed -> Approved -> Ready -> In Progress -> In Review -> Verified -> Done
                         |                         |
                         +-> Blocked              +-> Rework
```

- `Approved` means scope accepted, not implementation-ready.
- `Verified` means merged to `main`, pipeline green, and acceptance/demo evidence captured.
- `Done` additionally means RDS/traceability are aligned. Code alone is not a terminal state.
- A blocked item records the blocking issue, owner, next check date, and safe work that can
  continue; it does not disappear into a chat message.

## 4. Epic and dependency map

| Epic | Feature UCs | Hard upstream dependency | Gate-level demo result |
| --- | --- | --- | --- |
| EP-01 Identity and access | UC-01–08 | Security/SMTP decisions and account migration | correct sessions, roles, ownership, token expiry/use |
| EP-02 Catalogue and cart | UC-09–11, UC-19–20 | Manager seed and Product/SKU model | public browse/cart plus controlled catalogue maintenance |
| EP-03 Customer ordering and payment | UC-12–16 | EP-01, EP-02, inventory balance | DELIVERY order with COD/ONLINE payment, reservation, owned views, cancellation |
| EP-04 Inventory control | UC-21–24 | Product/SKU model and movement ledger | balance, receipt, adjustment, and immutable movement reconcile |
| EP-05 Store operations | UC-17–18, UC-25–29 | orders, inventory, money ledger | search/detail with actionable filter, pack, dispatch, outcome, cancellation, counter sale |
| EP-06 Returns and refunds | UC-30–34 | completed sale, order items, inventory and money ledger | request, review, one intake, stock disposition, actual refund |
| EP-07 Management reports | UC-35–37 | stable ledgers and fixed timestamp meanings | three independently reconciled report cohorts |

Critical path:

```text
TECH-01/02/03 walking skeleton
-> Product/SKU + inventory read
-> browser cart + UC-12 COD reservation
-> Staff order detail + UC-25/26/27 COD completion
-> stock commands + counter sale
-> online payment/cancellation
-> returns/refund
-> reports
-> feature freeze, SDS extraction, defense
```

## 5. Feature backlog seed

`Target` is the first planned completion week, not permission to postpone documentation.
Full BR/screen/API/table/query/test links are in `11_TRACEABILITY_SEED.md`.

| ID | Actor-goal outcome | Pri/Pts | Lead / reviewer | Target | Hard dependencies | Minimum acceptance evidence |
| --- | --- | --- | --- | --- | --- | --- |
| UC-01 | Verified Customer registration | P0/2 | A / B | W3 | TECH-02, SMTP proof | valid/expired/reused/replaced token; ACTIVE login only |
| UC-02 | Role-correct login session | P0/2 | A / C | W2 | TECH-02, Manager seed | session rotation, state rejection, CSRF refresh, role navigation |
| UC-03 | Session logout | P0/1 | A / C | W2 | UC-02 | POST+CSRF invalidates session; protected API becomes 401 |
| UC-04 | Own Customer profile maintenance | P1/2 | A / B | W3 | UC-02 | field validation and server-owned Customer identity isolation |
| UC-05 | Own delivery-address maintenance | P0/2 | A / C | W3 | UC-02 | create/edit/default/deactivate; wrong-owner request is rejected |
| UC-06 | Own password replacement | P1/2 | A / C | W7 | UC-02 | wrong current password rejected; all existing sessions invalidated |
| UC-07 | Forgotten-password recovery | P1/2 | A / B | W7 | SMTP proof | generic response; 30-minute, replacement, reuse, session tests |
| UC-08 | Staff account lifecycle | P1/2 | A / E | W7 | UC-02, SMTP proof | Staff-only target guard; setup/activate/deactivate/session behavior |
| UC-09 | Public catalogue/search | P0/2 | B / D | W3 | UC-19–20 seed | active filtering, bounded stable paging, search/sort, no N+1 evidence |
| UC-10 | Public Product/SKU detail | P0/1 | B / D | W3 | UC-20 | active visibility, availability, inactive/missing behavior |
| UC-11 | Browser-local cart | P0/2 | B / A | W3 | UC-09–10 | add/update/remove/clear; corrupt/stale storage; server values untrusted |
| UC-12 | Atomic Customer order placement | P0/3 | C / D | W4 | UC-05, UC-11, UC-20–21 | rollback, stale price, last-unit race, exact retry/conflicting key |
| UC-13 | Verified idempotent online payment | P1/3 | C / A | W7 | UC-12, provider proof | bad signature/ref/amount, duplicate, expiry and late-success race |
| UC-14 | Owned paged order history | P0/1 | A / B | W4 | UC-12 | mandatory owner predicate, status filter, deterministic paging |
| UC-15 | Owned order detail/timeline/receipt | P0/2 | B / A | W4 | UC-12–14 | no cross-owner leakage; payment/return/actions and paid-only receipt |
| UC-16 | Eligible Customer cancellation | P0/3 | C / D | W7 | UC-12–13 | allowed/forbidden states, release movement, paid/late refund-due state |
| UC-17 | Staff/Manager sales-order search | P0/2 | E / B | W5 | UC-12, UC-29 fixtures | role guard, channel/status/date filters, actionable preset, bounded stable paging |
| UC-18 | Operational order detail/actions | P0/2 | E / C | W5 | UC-17, ledgers | projection without duplication; Staff/Manager action differences |
| UC-19 | Category/Brand workspace | P1/2 | B / D | W3 | UC-02 Manager | normalized uniqueness; referenced-active deactivation conflict |
| UC-20 | Product/SKU workspace | P0/3 | B / D | W3 | UC-19 | activation invariants, immutable SKU code/history, stale version conflict |
| UC-21 | Current inventory overview | P0/2 | D / B | W3 | UC-20 | on-hand/reserved/available, low-stock, inactive-stock discoverability |
| UC-22 | Atomic stock receipt | P0/3 | D / C | W6 | UC-20–21 | multi-line balance/movement tie-out, rollback, retry/fingerprint conflict |
| UC-23 | Guarded stock adjustment | P0/2 | D / C | W6 | UC-21 | Manager/reason/version/nonnegative/reservation guards and one movement |
| UC-24 | Immutable movement history | P1/2 | D / C | W6 | all stock-changing UCs | filters, stable paging, business-reference trace, no edit/delete |
| UC-25 | Confirmed order packing | P0/1 | E / D | W5 | UC-17–18 | guarded single transition/history; zero stock delta |
| UC-26 | Packed order dispatch | P0/3 | E / D | W5 | UC-25 | order-then-SKU lock, exact physical/reserve deltas, duplicate guard |
| UC-27 | Delivery success/returned failure | P0/3 | E / C | W6 | UC-26 | exact COD collection or restored stock; terminal duplicate/race tests |
| UC-28 | Manager pre-shipping cancellation | P0/3 | C / E | W7 | UC-18, payment/stock ledgers | reason/state/role guards, reservation release, refund-due projection |
| UC-29 | Atomic cash counter sale | P0/3 | D / C | W6 | UC-20–21, ledgers | last-unit race, retry, full rollback, cash/stock/order reconciliation |
| UC-30 | Owned Customer return request | P1/3 | B / E | W8 | UC-15, completed DELIVERY order | owner/deadline/reason/line caps, one request, retry conflict |
| UC-31 | Receipt-based in-store request | P1/3 | D / E | W8 | UC-18/29, completed order | receipt evidence, either sale channel, line caps, duplicate guard |
| UC-32 | Whole-request Manager decision | P1/2 | E / B | W8 | UC-30–31 | Manager-only approve-all/reject, reason, stale/duplicate behavior |
| UC-33 | One physical return intake | P1/3 | E / D | W8 | UC-32 | positive receipt, disposition equation, sellable movement, no second intake |
| UC-34 | Actual capped refund completion | P1/3 | C / E | W8 | UC-16/27/33, money ledger | source eligibility, exact remaining cap, duplicate and atomic state tests |
| UC-35 | Cash movement report | P1/3 | C / B | W8 | complete money ledger | half-open transaction dates; collection-refund-net independent tie-out |
| UC-36 | Gross completed-sales report | P1/3 | B / C | W8 | completed order snapshots | completion cohort, channels/payment dimensions, item sum tie-out |
| UC-37 | Inventory roll-forward report | P1/3 | D / C | W8 | complete movement ledger | opening + categorized deltas = closing, boundary and seed tests |

## 6. Cross-cutting work items

These IDs are tasks/evidence, never actor-goal UCs.

| ID | Target | Lead model | Required result / exit evidence |
| --- | --- | --- | --- |
| TECH-01 | W1 | A + all review | GitLab monorepo, Java/Node versions, ignored env sample, protected `main`, MR templates |
| TECH-02 | W2 | A + C | Spring Security session/CSRF walking skeleton; React login/mutation/logout/fresh-token smoke |
| TECH-03 | W2 | D + C | Supabase-aligned CI PostgreSQL; Flyway `app`; Hibernate validate; safe first-Manager bootstrap |
| TECH-04 | W2 | B | Vite development proxy and production React build served from Spring artifact |
| TECH-05 | W2 | A | Stable API problem/error, paging, validation, correlation-ID, and DTO conventions |
| TECH-06 | W4 | C + D | PostgreSQL concurrency harness and global aggregate/SKU lock-order proof |
| TECH-07 | W7 | C | Provider adapter, signed callback/IPN, fixed Clock, 15-minute expiry scheduler proof |
| DATA-01 | W3 onward | B coordinates | Credible Vietnamese seed catalogue; all leads add their owned lifecycle fixtures |
| QA-01 | every MR | feature lead | Unit/controller/PostgreSQL/React tests plus browser smoke where applicable |
| DOC-01 | W1–8 | feature lead | RDS and traceability change in same MR as behavior change |
| DOC-02 | W9 | rotating auditor | ID/name/actor/state/API/schema/test drift report is empty at feature freeze |
| SDS-01 | W9–10 | all, one editor | Actual symbol/query/migration/test inventory from tagged commit; no planned fake classes |
| REL-01 | W9 | all | Fresh clone -> migrate -> seed -> test -> package -> run on clean environment |
| DEMO-01 | W9–10 | all | Four core paths plus selected conflict/race/external-failure paths are repeatable |

## 7. Ten-week release allocation

| Week | Implementation WIP limit and outcome | Document/test/evidence outcome | Stop/go check |
| --- | --- | --- | --- |
| 1 | Spikes only; no feature branch swarm | decisions, 37 UCs, 64 BRs, states, 17-table ERD, RDS skeleton | R0 requirements freeze |
| 2 | TECH-01–05 walking skeleton | CI/PostgreSQL/session/CSRF/build proof and auth matrix | R1 or stop all feature starts |
| 3 | Identity core, catalogue/Product/SKU, cart, inventory read | first complete vertical trace rows and reviewed RDS sections | data/route contracts stable |
| 4 | COD checkout/reservation and Customer views | concurrency/idempotency evidence; demo seed | R2 core sale gate |
| 5 | Staff search/detail with actionable filter, pack, dispatch | order sequences and movement tie-out | no invalid state shortcut |
| 6 | outcome/COD, stock commands/history, counter sale | store-operation reconciliation and core rehearsal | R3 store operations |
| 7 | payment sandbox, expiry, late result, cancellations, remaining identity P1 | provider/security/config evidence | no fake payment success |
| 8 | returns/refund and three table-first reports | authorization/report reconciliation and RDS completion | R4 complete baseline |
| 9 | defects only; WIP biased to integration | feature freeze, trace audit, final RDS, SDS extraction, clean install | R5 submission candidate |
| 10 | no scope work | SDS, render QA, user/install guide, demo and defense rehearsal | R6 defense package |

WIP rule: before R2, each member has at most one feature in `In Progress`; the team may have
only two simultaneous high-risk mutation UCs. Finish/review/integrate before starting another.

## 8. GitLab setup

### Milestones

Create `R0 Requirements`, `R1 Skeleton`, `R2 Core Sale`, `R3 Store Operations`,
`R4 Baseline`, `R5 Submission Candidate`, and `R6 Defense`. Map actual dates only after the
course calendar is known.

### Labels

Use a small controlled label set:

```text
type::feature  type::task  type::defect  type::change  type::document
epic::identity  epic::catalogue  epic::sales  epic::inventory
epic::operations  epic::returns  epic::reports  epic::platform
priority::P0  priority::P1  scope::optional
risk::security  risk::stock  risk::money  risk::external  risk::document
```

Do not encode owner, due date, or status redundantly in labels; GitLab already has assignee,
milestone, and board state.

### Feature issue body

```text
UC ID/name, actor, trigger, observable result
In-scope normal/alternative/error flows and cited BR/NFR IDs
SCR route and authorization; API request/response/error contract
Service transaction, lock/idempotency policy; tables/query/migration
Acceptance test IDs and demo fixture
Dependencies, lead, reviewer, milestone, relative points
RDS sections/figures/tables changed
Explicit non-goals

Checklist:
[ ] Definition of Ready passed
[ ] React/API/service/database vertical behavior implemented
[ ] role/ownership/CSRF and important conflict paths tested
[ ] PostgreSQL constraints/index/query behavior tested
[ ] traceability and RDS updated
[ ] clean review and green pipeline
[ ] demo evidence captured; reviewer can explain downstream effects
```

### Branch and merge request policy

- Branches contain the work ID, such as `feature/UC-12-checkout` or
  `fix/UC-26-dispatch-lock`; one branch should not hide unrelated UCs.
- Every change enters protected `main` through a linked Merge Request, green pipeline, and
  at least one peer review. High-risk stock/money/security changes require the named
  integration reviewer.
- An MR changing behavior updates the upstream decision/BR first and then code, test, RDS,
  and traceability in that same coherent change.
- Do not store secrets, Supabase connection strings, provider hashes, real personal data,
  or screenshots containing credentials in issues, commits, logs, or documents.

### Backlog export columns

If the course requires an XLSX similar to prior submissions, export these columns without
making the spreadsheet a second source of truth:

| Column | Value rule |
| --- | --- |
| UC ID / Module / Actor / Use Case | exact values from `02` |
| Lead / Reviewer | GitLab assignees mapped from A–E |
| Priority / Difficulty / Points | exact P0/P1 and 1/2/3 from this file |
| Status | exact board state, not free prose |
| Target week / Deadline | milestone and actual calendar date |
| Description | actor trigger and observable result, not code task list |
| Dependencies | UC/TECH IDs only |
| Evidence link | GitLab issue/MR/pipeline plus RDS/trace row |
| Notes | blocker or approved deviation only |

GitLab remains operational truth; export immediately before a review/submission and mark the
export date/version.

If a Contribution sheet is required, derive it from evidence rather than commit count alone:

| Contribution column | Evidence rule |
| --- | --- |
| Member / team role | actual name and current vertical responsibility |
| Assigned UCs / planned points | current whole-UC ownership from this file |
| Delivered work | merged issue/MR/test/migration/screen links from the trace ledger |
| Review/integration work | reviewed MRs and cross-module defects resolved |
| Document/demo work | authored/reviewed RDS/SDS/manual sections and rehearsed paths |
| Contribution % | team-agreed final assessment summing to 100%, with evidence and sign-off |

Do not infer contribution from lines changed, and do not award implementation credit for an
unmerged branch or document claim that another member cannot reproduce.

## 9. Release gates and capacity response

| Gate miss | Mandatory response | Forbidden response |
| --- | --- | --- |
| R1 session/CSRF/PostgreSQL not green | stop feature starts and pair on skeleton | bypass CSRF, switch to browser Supabase, use H2 as proof |
| R2 COD checkout race/retry fails | keep OPT-01/02 permanently out; pair C/D | remove locks/ledger or demo only single-user happy path |
| R3 stock/money does not reconcile | postpone UC-07 and UI polish | manually repair Supabase rows |
| R4 reports late | table-only reports with exact totals | merge three meanings into one vague dashboard |
| W9 trace/install unstable | absolute feature freeze | add “easy CRUD” to improve feature count |

Pre-approved reductions remain: optional features are already excluded; then postpone UC-07,
reduce chart/export polish, then remove UC-31 while preserving UC-30. Any other cut requires
the full impact protocol from `07_OPEN_DECISIONS.md` and must preserve the four core demo paths.

## 10. Weekly control routine

1. Monday: review gate, blockers, decision changes, dependencies, and WIP before assignment.
2. Daily: each member reports observable result, next acceptance test, and blocker—not a
   percentage with no evidence.
3. Before MR: lead self-checks UC/BR/state/traceability and runs the relevant local pipeline.
4. Review: reviewer tests one normal path, one tampered/alternative path, and downstream
   stock/money/authorization effects.
5. Friday: merge only green coherent slices, reconcile demo data, update RDS, and rehearse
   the newest integrated path from `main`.
6. At each release gate: record pass/fail evidence and execute the capacity response before
   starting lower-priority work.

The backlog is healthy only when every `Done` feature can be demonstrated from a clean build,
traced to its RDS facts, and explained end to end by both its lead and reviewer.
