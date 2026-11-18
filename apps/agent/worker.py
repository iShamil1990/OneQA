"""Poll OneQA once and execute a locally configured Vanessa launcher."""

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
from urllib.error import HTTPError
from urllib.parse import urlencode, urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener
from uuid import UUID


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def post(opener, url, token, payload=None):
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    request = Request(url, method="POST", data=body, headers={
        "X-OneQA-Agent-Token": token,
        "Content-Type": "application/json",
    })
    with opener.open(request, timeout=30) as response:
        return None if response.status == 204 else json.load(response)


def execute(run_dir, command, timeout):
    report = run_dir / "report.json"
    env = os.environ.copy()
    env.pop("ONEQA_AGENT_TOKEN", None)
    env["ONEQA_REPORT_PATH"] = str(report)
    try:
        with (run_dir / "launcher.log").open("wb") as log:
            process = subprocess.run(command, env=env, stdout=log, stderr=subprocess.STDOUT,
                                     timeout=timeout, check=False)
    except (OSError, subprocess.TimeoutExpired):
        return {"status": "FAILED", "scenarios": []}

    try:
        with report.open(encoding="utf-8") as source:
            scenarios = json.load(source)
        if not isinstance(scenarios, list) or not scenarios:
            raise ValueError("Expected a nonempty scenario array")
        external_ids = set()
        for scenario in scenarios:
            external_id = scenario["externalId"]
            if not isinstance(external_id, str) or not external_id or external_id in external_ids:
                raise ValueError("Expected unique scenario identifiers")
            external_ids.add(external_id)
            if scenario["status"] not in {"PASSED", "FAILED", "SKIPPED"}:
                raise ValueError("Invalid scenario status")
    except (OSError, ValueError, TypeError, KeyError):
        return {"status": "FAILED", "scenarios": []}
    failed = process.returncode != 0 or any(item["status"] == "FAILED" for item in scenarios)
    return {"status": "FAILED" if failed else "PASSED", "scenarios": scenarios}


def run_once(args, token):
    opener = build_opener(NoRedirect())
    base = f"{args.url.rstrip('/')}/api/agents/{args.agent_id}"
    assignment = post(opener, f"{base}/runs/claim?{urlencode({'projectId': args.project_id})}", token)
    if assignment is None:
        return
    if UUID(assignment["projectId"]) != args.project_id:
        raise ValueError("OneQA returned an assignment for another project")
    run_id = UUID(assignment["runId"])
    run_dir = args.state_dir.resolve() / str(run_id)
    manifest = run_dir / "delivery.json"
    if not manifest.exists():
        # Create the directory before invoking 1C. A restarted worker must never
        # silently execute the same assigned suite again after an interruption.
        run_dir.mkdir()
        delivery = execute(run_dir, args.command, args.timeout)
        pending = manifest.with_suffix(".tmp")
        pending.write_text(json.dumps(delivery), encoding="utf-8")
        pending.replace(manifest)
    else:
        delivery = json.loads(manifest.read_text(encoding="utf-8"))

    run_url = f"{base}/runs/{run_id}"
    for scenario in delivery["scenarios"]:
        post(opener, f"{run_url}/vanessa/scenarios", token, scenario)
    post(opener, f"{run_url}/finish", token, {"status": delivery["status"]})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", required=True)
    parser.add_argument("--agent-id", required=True, type=UUID)
    parser.add_argument("--project-id", required=True, type=UUID)
    parser.add_argument("--state-dir", required=True, type=Path)
    parser.add_argument("--timeout", type=int, default=1800)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if args.command[:1] == ["--"]:
        args.command.pop(0)
    endpoint = urlsplit(args.url)
    if (not endpoint.hostname or endpoint.username or endpoint.query or endpoint.fragment
            or (endpoint.scheme != "https" and
                (endpoint.scheme != "http" or endpoint.hostname not in {"localhost", "127.0.0.1", "::1"}))):
        parser.error("--url must be HTTPS or loopback HTTP")
    token = os.environ.get("ONEQA_AGENT_TOKEN")
    if not token or not args.command or args.timeout <= 0:
        parser.error("Set ONEQA_AGENT_TOKEN and supply a launcher command and positive timeout")
    try:
        args.state_dir.mkdir(parents=True, exist_ok=True)
        run_once(args, token)
    except HTTPError as error:
        print(f"OneQA request failed with HTTP {error.code}", file=sys.stderr)
        return 1
    except (OSError, ValueError, KeyError) as error:
        print(f"Worker failed: {type(error).__name__}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
