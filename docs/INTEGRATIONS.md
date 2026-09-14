# External integration boundaries

This file records configuration readiness only. It does not claim completion of the use
cases that own email or online payment behavior.

## Secret boundary

- Real credentials belong in the ignored root `.env` for local development and in masked,
  protected GitLab CI/CD variables for shared environments.
- React receives no database, SMTP, payment-provider or Supabase credential.
- Logs, API responses, screenshots and committed configuration must not contain secrets.
- `.env.example` contains names only so a fresh clone documents every required variable.

## Supabase PostgreSQL

The developer MCP connection is OAuth-authenticated and scoped to project
`hqelyoqvqofqzvdcwvna`. A read-only dashboard query confirmed that the active database is
PostgreSQL 17.6 and that schema `app` has not been migrated yet. MCP access is a development
tool and does not configure the running application. Spring still needs an SSL JDBC URL,
database username and database password through `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

React must not use Supabase Auth, anon/service-role keys or the Data API. Flyway remains the
only owner of schema `app`. Prefer a direct IPv6 database connection when the deployment host
supports it; otherwise use Supavisor session mode on port 5432 so Hibernate/JDBC prepared
statements remain valid. For this project, session mode uses host
`aws-0-ap-southeast-1.pooler.supabase.com` and user
`postgres.hqelyoqvqofqzvdcwvna`; both connection modes use the project database password.

## SMTP

The selected local settings use authenticated SMTP with STARTTLS on port 587. Spring Mail
reads `SMTP_*`; application messages use `MAIL_FROM_EMAIL` and `MAIL_FROM_NAME`. The sender
display name is `Gia Dụng Việt`. The address must remain an account or alias accepted by the
SMTP provider.

SMTP is infrastructure for UC-01, UC-07 and UC-08. Their owner must still implement token
lifecycle, failure handling and one real sandbox-message proof. Configuration alone is not
feature evidence.

## Online payment

Online payment is explicitly deferred. No provider credentials, SDK, controller, callback or
payment state transition is part of the current runtime configuration. The team must approve
DEC-04 before provider-specific variables and implementation are added.

## GitLab

The local `origin` remote points to `https://gitlab.com/031153437/SWP391.git`. Authentication,
the first push, protected `main`, masked CI/CD variables and the first hosted pipeline remain
GitLab-side actions; none are implied by adding the remote.
