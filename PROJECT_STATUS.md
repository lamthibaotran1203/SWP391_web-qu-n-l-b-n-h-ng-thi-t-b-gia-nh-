# Project status

Updated: 2026-09-10

## Current release gate

R1 executable walking skeleton is locally complete. React, Spring Security, Flyway,
PostgreSQL, packaging and browser proof exist. The shared Supabase PostgreSQL database is now
connected through Spring, migrated by Flyway and validated by Hibernate; Supabase login proof
awaits a locally configured bootstrap Manager. CI-host proof is still pending.

## User-owned use cases

Not recorded yet. This blocks business feature implementation, but did not block the shared
TECH-01–05 setup requested here.

## Completed evidence

- Monorepo layout, pinned Java/Node/Maven versions, wrappers, ignored environment sample,
  editor/line-ending rules and GitLab templates.
- Vite `/api` proxy and production React output copied into the Spring Boot JAR.
- Real CSRF endpoint, CSRF-protected login/logout, server session and safe current-user DTO.
- Environment-driven, BCrypt-hashed, idempotent first-Manager bootstrap with no committed
  default credentials.
- Flyway `V1` creates `app.user_account`; Hibernate validates the PostgreSQL mapping.
- Stable problem response and per-request trace ID foundation.
- Frontend lint/test/build passed on 2026-09-10.
- PostgreSQL 17 integration tests passed under Java 21/Maven 3.9.16: 2 tests, 0 failures,
  0 errors.
- Authentication tests use an explicit `.invalid` fixture and remove it after the test class;
  production UI contains no test credentials or walking-skeleton/debug copy.
- Development browser smoke passed through the Vite `/api` proxy: connected state, login,
  refresh-preserved session, CSRF logout, anonymous state, empty local/session storage and no
  console errors.
- Packaged Spring Boot JAR browser smoke passed with the embedded React build and no console
  errors.
- GitLab CI YAML parsed successfully; the executable JAR was packaged again under the pinned
  Java 21/Maven 3.9.16 toolchain and contains the React entry point/assets.
- Supabase developer MCP is scoped and OAuth-authenticated to the selected project; GitLab
  `origin`, local SMTP variables, Spring Mail configuration and secret-file guards are
  prepared without exposing credentials to React or tracked configuration.
- A read-only Supabase query confirmed PostgreSQL 17.6, database/user `postgres`, and no
  existing `app` schema. Direct IPv6 and Supavisor session-mode endpoints are reachable.
- On 2026-09-14, Spring Boot connected to Supavisor session mode over SSL, Flyway created
  schema `app` and applied `V1`, Hibernate validated the mapping, and `/actuator/health` plus
  `/api/v1/auth/csrf` responded successfully. No Manager was created because bootstrap values
  were intentionally absent from the local environment.
- Online payment is explicitly deferred; no provider-specific runtime configuration or
  implementation is retained. The local `.env` is ignored and restricted to the current OS
  user.

## Current task

Clarify local and Supabase runtime configuration without starting unassigned business UCs.

## Next task

Configure a local bootstrap Manager, then run the browser login/logout proof against the
shared Supabase database. Obtain GitLab authentication and A–E member/UC ownership afterward.

## Blockers and unresolved decisions

- Supabase MCP, network reachability and Spring/Flyway proof are verified against PostgreSQL
  17.6. Session pooler is the tested connection choice. Browser authentication proof requires
  a locally configured bootstrap Manager (DEC-27, DEC-38).
- SMTP credentials are available locally; a real-message proof and credential rotation remain
  pending (DEC-03).
- Online payment is deferred. The provider decision remains open (DEC-04), with no active
  provider configuration in the codebase.
- GitLab `origin` is configured, but repository authentication, protected `main`, masked CI/CD
  variables and the first hosted pipeline are pending.
- Deployment host (DEC-29) and team ownership mapping (DEC-30) remain unresolved.

## Exact verification commands

From the repository root with JDK 21, Node 24/npm 11 and Docker running:

```text
npm run db:up
npm run frontend:check
npm run backend:test
npm run build
```

Visible result: `http://localhost:5173` shows “Hệ thống đã sẵn sàng”; a configured bootstrap
Manager can log in, survive refresh, log out through CSRF, and becomes anonymous on the next
`/api/v1/auth/me` request.
