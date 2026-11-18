# Vanessa Automation result ingest (PR-0016)

This is a **results ingestion API**, not an agent executor or task dispatcher.

Agent sends `POST /api/agents/{agentId}/runs/{runId}/vanessa/scenarios` with `X-OneQA-Agent-Token` and JSON:

```json
{
  "externalId": "feature-file:scenario-line",
  "featureName": "Client order",
  "scenarioName": "Create order",
  "status": "PASSED",
  "errorMessage": null,
  "steps": [
    {"index": 0, "text": "Given a client exists", "status": "PASSED", "durationMs": 125, "errorMessage": null}
  ],
  "artifacts": [{"kind": "LOG", "reference": "logs/run-123/scenario.txt"}]
}
```

The backend accepts only active agent credentials from the run's organization while the run is `RUNNING`. Returns `201 {"id":"UUID"}` on success. Invalid credentials, cross-organization submissions or non-running runs return `403`. Scenario identifiers are unique per run to prevent accidental duplicate submissions. The operation is transactional, so a failed step/artifact insert rolls back the scenario.

User JWT access to scenario summaries:
`GET /api/organizations/{organizationId}/workspaces/{workspaceId}/projects/{projectId}/runs/{runId}/vanessa/scenarios`.

Notes:
- Artifacts are **references**, not uploaded files. Do not put secrets in references; actual binary upload and access controls are future work.
- Step status: `PASSED`, `FAILED`, `SKIPPED`. Artifact kinds: `LOG`, `SCREENSHOT`, `ERROR`, `OTHER`.
- The agent adapter is responsible for translating Vanessa Automation outputs into this JSON contract.
- This version does not implement retries/idempotent upsert, agent-to-run assignment, event streaming or automatic run completion.
