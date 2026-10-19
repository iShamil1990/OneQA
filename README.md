# OneQA

> Enterprise Test Automation Platform. First edition: **OneQA for 1C**.

## Overview

OneQA is an open-source platform for managing automated test execution
with an initial focus on the 1C ecosystem.

### MVP Goals

-   Execute and monitor automated test runs
-   Collect and store test results
-   View Gherkin scenarios and steps
-   Store screenshots, logs and execution artifacts
-   Live execution monitoring
-   REST API for integrations
-   Jenkins and GitLab CI integration
-   Vanessa Automation adapter

## Technology Stack

### Backend

-   Java 21
-   Spring Boot 3.5
-   Spring Modulith
-   Spring Security
-   PostgreSQL
-   Flyway
-   MapStruct
-   OpenAPI

### Frontend

-   Vue 3
-   TypeScript
-   Vite
-   Pinia
-   Vue Router
-   Tailwind CSS
-   TanStack Query

## Repository Layout

``` text
apps/
  backend/
  frontend/
  agent/

packages/
  ui-core/
  ui-domain/
  design-tokens/
  sdk-java/
  sdk-ts/

adapters/
docker/
docs/
scripts/
```

## Development Roadmap

See [the maintained roadmap](docs/ROADMAP.md) for completed PRs, work in
progress, proposed delivery sequence and quality gates. The original PR-0004
through PR-0010 plan has been superseded by actual implementation.

## License

Apache License 2.0
