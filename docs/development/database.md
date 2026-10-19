# Local PostgreSQL development

This is a **development-only** Docker Compose configuration. It is not a production deployment.

1. Copy `.env.example` to `.env` in the repository root.
2. Replace all placeholder passwords with unique secrets.
3. Run `docker compose config` and `docker compose up -d postgres` from the repository root.
4. Export `DB_URL=jdbc:postgresql://localhost:5432/oneqa`, `DB_USERNAME` and `DB_PASSWORD` matching the PostgreSQL variables in `.env`.
5. From `apps/backend`, run `mvn verify` then `mvn spring-boot:run`.
6. Check `http://localhost:8080/api/system/ping`.

Flyway applies versioned migrations at application startup. The initial migration creates the `schema_version` table; Flyway maintains its own `flyway_schema_history` table.

The integration test requires a running PostgreSQL database with the environment variables above. GitHub Actions provides its own isolated PostgreSQL service.

**Existing volumes:** Changing `POSTGRES_PASSWORD` in `.env` does not reset a password already initialized in the `postgres-data` volume. Change the database role password explicitly, or reset the disposable development volume after backing up any data.
