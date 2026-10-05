# Household Goods Sales and Inventory Management System

SWP391 Group 03 classroom project for one Vietnamese household-goods store and one stock
location. The codebase is a modular monolith: React/Vite in the browser, Spring Boot REST and
Spring Security on the server, and PostgreSQL managed by Flyway.

Last recorded runtime state: **R1 walking skeleton locally verified**. The Supabase JDBC/Flyway
connection was verified on 2026-09-14; the first GitLab-hosted pipeline and full browser
login proof against Supabase were still pending. No business use case is claimed complete.

## Requirements documents

Read [PROJECT_STATUS.md](PROJECT_STATUS.md) before using local planning files. The team
submitted a 35-UC Google Sheet Backlog and RDS to the lecturer, then received substantial
feedback. The live RDS is being edited. The 37-UC
`household-goods-sales-planning-handoff/` folder is an older, unapproved proposal and is
useful only as reference material until the team records revised decisions. Current code
and migrations show implementation status, not the full accepted scope.

For the current Use Case Diagram, Screen Flow, ERD, database-schema, and lecturer-feedback
review, read
[docs/HomeMart_RDS_Consolidated_Review.docx](docs/HomeMart_RDS_Consolidated_Review.docx).
The document uses one UC Diagram specification per actor and records the proposed
`include`/`extend` relationships in prose and tables. The 35-UC Google Sheet remains the
canonical register. Apply UC name, actor, scope, and iteration corrections in the Sheet
before propagating them to the RDS, diagrams, authorization matrix, code, or tests.

## What is already wired

- React gets a real CSRF token and current-session projection from Spring Boot.
- Spring Security performs CSRF-protected form login, server-session authentication and
  logout.
- Flyway creates the lowercase `app` schema and identity table in PostgreSQL; Hibernate uses
  `ddl-auto=validate`.
- The first Manager is created idempotently from environment variables. No default password
  is committed.
- Development uses Vite's `/api` proxy. The production JAR contains the React build.
- Frontend tests, PostgreSQL integration tests, packaging and smoke checks are represented in
  GitLab CI.

No Supabase browser key is required. This application uses Supabase only as PostgreSQL, so
later configuration needs the backend JDBC URL, database username and database password.
React must never receive those values.

## Pinned toolchain

| Tool | Version |
| --- | --- |
| Java | 21 |
| Maven | 3.9.16 through Maven Wrapper |
| Node.js | 24.19.0 |
| npm | 11.x |
| Spring Boot | 3.5.16 |
| React | 19.3.0 |
| Vite | 8.2.2 |
| Local/CI/Supabase PostgreSQL | 17 (Supabase currently 17.6) |

## Repository map

```text
backend/
  src/main/java/com/swp391/householdgoods/
    common/          stable API errors/envelopes and request trace IDs
    config/          framework configuration and typed environment settings
    security/        session, CSRF, login/logout and current-user boundary
    identity/
      domain/        account entity and enums
      application/   transactions and application flow
      infrastructure/JPA repository
  src/main/resources/
    db/migration/    append-only Flyway migrations
frontend/
  src/
    api/             only shared HTTP/CSRF client
    app/             router and top-level providers
    auth/            server-backed session state
    features/        approved feature modules only
    pages/           route-level UI
    components/      small reusable UI pieces
docs/                architecture, later RDS/SDS/diagrams/evidence
scripts/             OS-neutral Node launch/build helpers
household-goods-sales-planning-handoff/
                      historical 37-UC proposal; see PROJECT_STATUS.md
```

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before changing the login flow. It contains
the exact request path to explain during defense.

## Fresh clone setup

### 1. Prerequisites on macOS/Linux

Install Git, a JDK 21 distribution, Node.js 24.19.x with npm 11, and Docker Desktop or Docker
Engine with Compose. Maven does not need to be installed globally.

Verify from the repository root:

```bash
java -version
node --version
npm --version
docker compose version
```

`java -version` must show 21. Node must show 24 and npm must show 11.

### 1. Prerequisites on Windows PowerShell

Install Git, a JDK 21 distribution, Node.js 24.19.x with npm 11, and Docker Desktop. Enable
Linux containers in Docker Desktop. Open a new PowerShell window and verify:

```powershell
java -version
node --version
npm --version
docker compose version
```

If Java reports another version, set `JAVA_HOME` to the JDK 21 directory and reopen the
terminal. Do not install Maven separately; `backend\mvnw.cmd` supplies the pinned version.

### 2. Clone and install frontend dependencies

Clone the team repository:

```text
git clone https://gitlab.com/031153437/SWP391.git
cd <CLONED_FOLDER>
npm --prefix frontend ci
```

The same three commands work in macOS/Linux terminals and Windows PowerShell.

### 3. Create local configuration

Copy `.env.example` to `.env`, then fill only these local bootstrap fields:

```dotenv
BOOTSTRAP_MANAGER_EMAIL=your.manager.email@example.com
BOOTSTRAP_MANAGER_PASSWORD=choose-a-private-password-with-letter-and-digit
BOOTSTRAP_MANAGER_FULL_NAME=Quản lý cửa hàng
```

Leave the other blank fields in place; the local launcher ignores blank values and uses the
safe Docker database defaults. `.env` is ignored by Git. Never paste a Supabase password,
SMTP secret or payment secret into a tracked file, chat screenshot or frontend variable.

macOS/Linux:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

### 4. Start PostgreSQL

Start Docker Desktop first, then run from the repository root:

```text
npm run db:up
```

The local database listens on `localhost:55432` to avoid collisions with a separately
installed PostgreSQL on the usual port 5432. The container creates both `household_goods`
and `household_goods_test`; Flyway alone creates the `app` schemas and tables.

### 5. Start backend and frontend

Terminal 1, from the repository root:

```text
npm run backend:dev
```

Terminal 2, from the repository root:

```text
npm run frontend:dev
```

Open [http://localhost:5173](http://localhost:5173). Do not open port 8080 for normal local
frontend development; using 5173 proves the Vite `/api` proxy works.

## One identical browser verification flow

Use this flow on macOS, Linux and Windows:

1. Open `http://localhost:5173` and confirm the green “Hệ thống đã sẵn sàng”
   state. This appears only after React receives real CSRF and session responses from Spring.
2. Open “Đăng nhập hệ thống” and use the email/password stored only in your local
   `.env`.
3. Confirm the page shows the Manager full name, email and `MANAGER` role.
4. Refresh the browser; the session remains authenticated and no token appears in local or
   session storage.
5. Select “Đăng xuất”; confirm the next session read is anonymous.
6. Stop and restart both applications. Log in again with the same account to prove PostgreSQL
   persistence and idempotent bootstrap behavior.

This is TECH-01–05 evidence only. It must not be presented as completion of UC-01 through
UC-08.

## Checks before pushing a branch

Start the test database with `npm run db:up`, then run:

```text
npm run frontend:check
npm run backend:test
npm run build
```

`npm run build` runs `npm ci`, builds React, then packages one executable Spring Boot JAR at
`backend/target/household-goods-backend-0.1.0-SNAPSHOT.jar`. The JAR contains
`static/index.html` and serves both the SPA and `/api/v1` from one origin.

## Connecting the application to Supabase

The selected project reference is `hqelyoqvqofqzvdcwvna`. It is online and runs PostgreSQL
17.6, matching local development and CI major 17. Its direct connection uses IPv6; the
database password is still required separately. Set environment variables outside Git:

```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://db.hqelyoqvqofqzvdcwvna.supabase.co:5432/postgres?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=<database-password>
SESSION_COOKIE_SECURE=true
BOOTSTRAP_MANAGER_EMAIL=<private-manager-email>
BOOTSTRAP_MANAGER_PASSWORD=<private-manager-password>
BOOTSTRAP_MANAGER_FULL_NAME=<manager-name>
```

Prefer a direct IPv6 connection when the deployment host supports it; otherwise use
Supavisor **session mode on port 5432** with:

```dotenv
DB_URL=jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.hqelyoqvqofqzvdcwvna
```

Both modes use the same project database password. Do not use transaction mode 6543 as the
normal Hibernate connection. `APP_BASE_URL` remains blank until a deployment host is chosen;
the current walking skeleton does not read it yet. Keep local Docker as the reproducible
fallback and test database even if the team chooses Supabase for shared development.

The Supabase MCP connection is optional developer tooling and does not replace these JDBC
variables. A teammate can build and run the application without installing Codex or MCP.

## How to explain the code in defense

For login, narrate this exact path:

```text
LoginPage
-> frontend/src/api/http.js obtains CSRF and posts credentials
-> Vite proxies /api to Spring Boot
-> SecurityConfig validates CSRF and invokes Spring's authentication filter
-> DatabaseUserDetailsService
-> AccountQueryService (@Transactional readOnly)
-> UserAccountRepository
-> app.user_account
-> BCrypt comparison
-> HttpOnly server session
-> safe CurrentUserResponse
-> AuthProvider refreshes the visible UI state
```

The rule for future features is equally simple:

```text
React page -> shared API client -> REST Controller DTO
-> Application Service transaction/rules -> Repository query/entity
-> PostgreSQL constraints -> response DTO -> React reloads server truth
```

Do not put calculations in controllers, permission checks only in React, business rules in
repositories, or JPA entities directly in JSON responses.

## Common problems

- **Java version rejected:** install/select JDK 21. The build deliberately stops on Java 17.
- **Docker connection error:** start Docker Desktop, wait until it reports “Engine running”,
  then repeat `npm run db:up`.
- **Port 55432 already used:** stop the conflicting local service or set `DB_URL` to a known
  disposable PostgreSQL instance. Keep the team-documented port consistent.
- **Manager login fails after changing `.env`:** bootstrap is intentionally idempotent and
  does not overwrite an existing password. Use the original password or recreate only your
  disposable local database.
- **Reset local Docker data:** `docker compose down --volumes` permanently deletes this
  project's local database volume; use it only when local data can be discarded. Never use
  it against Supabase.

## Team workflow

- Protect `main`; changes enter through a GitLab Merge Request with a green pipeline and one
  reviewer.
- One owner implements and explains one complete vertical UC slice.
- Do not begin a business UC until ownership and its planning/traceability rows are confirmed.
- Never edit an applied Flyway migration. Add a new migration.
- Never commit `.env`, passwords, JDBC URLs with credentials or provider secrets.

External service readiness and the boundary between configuration and owned UCs are recorded
in [docs/INTEGRATIONS.md](docs/INTEGRATIONS.md).

## Shared schema migration

Supabase was upgraded through Flyway V4 on 2026-10-05. It contains 24 business tables plus
Flyway history. Existing V1–V3 and product data were preserved; incomplete catalogue items
are INACTIVE. See [deployment evidence and RDS addendum](docs/database/HomeMart_Schema_Deployment_2026-10-05.md).

On macOS/Linux or Windows PowerShell, from repository root with JDK 21 and the ignored
local DB environment configured:

```text
npm run build
npm run db:info
npm run db:migrate
npm run db:info
```

Expected: Flyway current version 4, pending migrations 0. The schema command does not start
the web server or bootstrap accounts. Never manually apply the historical diagram SQL to
shared Supabase. Run integration tests against isolated PostgreSQL, not the shared database.

## Shared catalogue foundation

Read [the catalogue contract](docs/contracts/Catalog_Shared_Contract.md) before implementing
product, cart, order or stock code. Supabase is now at V5. Live prices belong to `hm_sku` and
live quantities to `hm_inventory`; old product fields are immutable `legacy_*` archives.
The public read API is `/api/v1/catalog/skus`; management UC-13 remains to be implemented.

```text
npm run frontend:check
npm run backend:verify:postgres
npm run db:seed:catalog
```

The verification command creates/removes an isolated PostgreSQL Docker database and builds
with Java 21; it never uses Supabase credentials. Demo seed is explicit/idempotent and targets
the configured database. The shared frontend catalogue client is in `frontend/src/features/catalog/api.js`.
