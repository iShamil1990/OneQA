import argparse
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
from threading import Thread
import unittest
from urllib.error import HTTPError
from uuid import uuid4

from worker import run_once


class WorkerTest(unittest.TestCase):
    def test_replays_report_after_lost_response_without_rerunning_launcher(self):
        run_id, project_id, agent_id = uuid4(), uuid4(), uuid4()
        received = []
        scenarios = {}

        class Server(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def do_POST(self):
                length = int(self.headers.get("Content-Length", 0))
                payload = json.loads(self.rfile.read(length)) if length else None
                received.append((self.path, self.headers["X-OneQA-Agent-Token"], payload))
                body = b""
                status = 204
                if "/claim?" in self.path:
                    status = 200
                    body = json.dumps({"runId": str(run_id), "projectId": str(project_id)}).encode()
                elif self.path.endswith("/scenarios"):
                    first = not scenarios
                    scenarios.setdefault(payload["externalId"], payload)
                    status = 503 if first else 201
                    body = b"{}"
                self.send_response(status)
                self.end_headers()
                self.wfile.write(body)

        launcher = """
import json, os
from pathlib import Path
path = Path(os.environ["ONEQA_REPORT_PATH"])
with path.open("x", encoding="utf-8") as out:
    json.dump([{"externalId": "demo:1", "featureName": "Demo",
                "scenarioName": "Run", "status": "PASSED",
                "steps": [], "artifacts": []}], out)
"""
        with TemporaryDirectory() as directory, HTTPServer(("127.0.0.1", 0), Server) as server:
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            args = argparse.Namespace(url=f"http://127.0.0.1:{server.server_port}",
                                      agent_id=agent_id, project_id=project_id,
                                      state_dir=Path(directory), timeout=10,
                                      command=[sys.executable, "-c", launcher])
            try:
                with self.assertRaises(HTTPError):
                    run_once(args, "worker-test-token")
                run_once(args, "worker-test-token")
            finally:
                server.shutdown()
                thread.join()
        self.assertEqual({"status": "PASSED"}, received[-1][2])
        self.assertEqual(received[1], received[3])
        self.assertIn(f"projectId={project_id}", received[0][0])
        self.assertEqual(1, len(scenarios))


if __name__ == "__main__":
    unittest.main()
