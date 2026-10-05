# External integration boundaries

This file records the last verified runtime configuration, not an approved future RDS scope.
See [PROJECT_STATUS.md](../PROJECT_STATUS.md) for the submitted-document status. It does not
claim completion of the use cases that own email or online payment behavior.

## Secret boundary

- Real credentials belong in the ignored root `.env` for local development and in masked,
  protected GitLab CI/CD variables for shared environments.
- React receives no database, SMTP, payment-provider or Supabase credential.
- Logs, API responses, screenshots and committed configuration must not contain secrets.
- `.env.example` contains names only so a fresh clone documents every required variable.

## Supabase PostgreSQL

The developer MCP connection is OAuth-authenticated and scoped to project
`hqelyoqvqofqzvdcwvna`. A read-only dashboard query confirmed PostgreSQL 17.6. On
2026-09-14, Spring connected over SSL through Supavisor session mode, Flyway applied `V1` to
schema `app`, and Hibernate validated the mapping. These are dated checks, not a fresh audit
of the current remote database. MCP access does not configure the running application;
Spring still needs `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

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

SMTP supports registration, password reset, and staff setup if those flows remain in the
approved scope. Historical 37-UC IDs must not be copied into the submitted 35-UC RDS. The
owners still need token lifecycle, failure handling, and one real sandbox-message proof.
Configuration alone is not feature evidence.

## Online payment

Online payment is not yet implemented. No provider credentials, SDK, controller, callback,
or payment state transition is part of the last recorded runtime configuration. The provider
and flow need a current team decision; `DEC-04` in the historical planning pack is a draft.

## GitLab

The local `origin` remote points to `https://gitlab.com/031153437/SWP391.git`. Branch
protection, masked CI/CD variables, and hosted pipeline status were not checked during the
2026-09-24 documentation review; none is proven by the local remote configuration.
