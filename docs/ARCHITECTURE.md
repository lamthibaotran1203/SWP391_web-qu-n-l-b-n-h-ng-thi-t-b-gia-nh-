# Architecture and defense map

This document describes the code that exists in the walking skeleton. It is not an SDS and
does not predict classes for unimplemented use cases.

## Runtime boundary

```text
Development
Browser :5173 -> React -> /api proxy -> Spring Boot :8080 -> JDBC -> PostgreSQL

Production/demo
Browser -> Spring Boot static React files + /api/v1 -> JDBC/SSL -> Supabase PostgreSQL
```

React never receives a database URL, database password, Supabase anon key or service-role
key. Spring Security owns the authenticated session; PostgreSQL is reachable only by the
backend.

## Login request flow

```text
LoginPage
  -> api/http.js obtains GET /api/v1/auth/csrf
  -> POST /api/v1/auth/login through the Vite proxy
  -> SecurityFilterChain checks CSRF
  -> Spring UsernamePasswordAuthenticationFilter
  -> DatabaseUserDetailsService
  -> AccountQueryService (read-only transaction)
  -> UserAccountRepository
  -> app.user_account
  -> BCrypt password comparison
  -> server HttpOnly session + safe CurrentUserResponse
  -> AuthProvider updates the visible session state
```

`AuthController` does not implement password comparison. Login is handled by Spring
Security's filter. The controller exposes only the safe current-user and CSRF reads.

## Bootstrap and database startup flow

```text
Spring startup
  -> datasource from environment/local profile
  -> Flyway creates and migrates schema app
  -> Hibernate validates entity-to-table mapping (never updates DDL)
  -> BootstrapManagerRunner reads optional environment configuration
  -> BootstrapManagerService validates and hashes the password
  -> UserAccountRepository inserts only when the email does not exist
```

There is no committed default Manager password. A second startup is idempotent and does not
overwrite the existing password.

## Package responsibilities

| Path | Responsibility |
| --- | --- |
| `backend/.../common` | API envelope/problem details and per-request trace ID |
| `backend/.../config` | framework beans, typed environment config and SPA forwarding |
| `backend/.../security` | session, CSRF, login/logout and current actor boundary |
| `backend/.../identity/domain` | account entity and domain vocabulary |
| `backend/.../identity/application` | use-case/application orchestration and transactions |
| `backend/.../identity/infrastructure` | JPA persistence adapter |
| `frontend/src/api` | the only shared HTTP/CSRF client |
| `frontend/src/auth` | current authenticated-user state from the server |
| `frontend/src/pages` | route-level UI compositions; no database/business authority |
| `frontend/src/components` | small reusable visual components |

When a business feature is assigned, add one vertical package such as `catalog`, `order` or
`inventory`. Its controller accepts DTOs, its application service owns rules/transactions,
and its repository handles persistence/query only. Do not create all future packages before
their contracts and owners are confirmed.

## Security invariants already implemented

- Mutating session requests require the CSRF value issued by Spring.
- Login success rotates the session; logout invalidates it and clears cookies.
- `GET /api/v1/auth/me` returns an allow-listed projection, never the entity/password hash.
- Manager and Staff URL namespaces have backend role checks.
- API failures use stable problem fields and an opaque `X-Trace-Id`.
- Production cookies default to Secure, HttpOnly and SameSite=Lax.
