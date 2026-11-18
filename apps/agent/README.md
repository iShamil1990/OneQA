# OneQA Vanessa worker

The worker needs Python 3.10+ and a locally configured Vanessa launcher. Register
an agent with the `vanessa` capability and supply its token through the
`ONEQA_AGENT_TOKEN` environment variable. Use a dedicated agent identity and a
persistent, private state directory for each worker.

```text
python apps/agent/worker.py --url https://oneqa.example --agent-id AGENT_UUID --project-id PROJECT_UUID --state-dir DIRECTORY -- LAUNCHER ARG1 ARG2
```

The launcher command is configured on the worker host. It must run the configured
Vanessa suite and write a UTF-8 JSON array of
[OneQA scenario objects](../../docs/architecture/vanessa-results.md) to
`ONEQA_REPORT_PATH`. Its output is captured in `launcher.log` in the run
directory. A nonzero exit code or failed scenario sets the run to `FAILED`.
Artifact references may be included in the report.

Run the command periodically with a scheduler that prevents overlapping
invocations. Each invocation claims at most one run. If a delivery fails after
the report is saved, the next invocation retries sending it. The backend retains
the first report for each external scenario ID. If the worker is interrupted
before saving the report, it leaves the assignment for operator recovery and
will avoid silently rerunning the same suite.

Run the worker test with `python -m unittest discover -s apps/agent`. It covers
claim, a local launcher, interrupted delivery, and final status against a test
HTTP endpoint. The actual 1C launcher configuration is environment-specific.
