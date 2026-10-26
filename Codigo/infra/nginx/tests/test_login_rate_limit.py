"""Exercise the shipped Nginx configuration with real HTTP and an isolated upstream.

Run: python -m unittest discover -s Codigo/infra/nginx/tests -v
Requires Docker; containers and network are removed even after assertion failures.
"""

import http.client
import json
from pathlib import Path
import subprocess
import time
import unittest
import uuid


NGINX_IMAGE = "nginx:1.30.4-alpine3.24"
ROOT = Path(__file__).resolve().parents[1]


def docker(*args):
    return subprocess.run(
        ["docker", *args], check=True, capture_output=True, text=True
    ).stdout.strip()


class LoginRateLimitTest(unittest.TestCase):
    def test_limits_login_before_upstream_and_preserves_other_routes(self):
        suffix = uuid.uuid4().hex[:12]
        network = f"glazr-rate-test-{suffix}"
        backend = f"glazr-rate-backend-{suffix}"
        proxy = f"glazr-rate-proxy-{suffix}"
        try:
            docker("network", "create", network)
            docker(
                "run", "-d", "--name", backend, "--network", network,
                "--network-alias", "backend", "-v",
                f"{(ROOT / 'tests/backend.conf').as_posix()}:/etc/nginx/nginx.conf:ro",
                NGINX_IMAGE,
            )
            docker(
                "run", "-d", "--name", proxy, "--network", network,
                "-p", "127.0.0.1::8080", "-v",
                f"{(ROOT / 'nginx.conf').as_posix()}:/etc/nginx/nginx.conf:ro",
                NGINX_IMAGE,
            )
            port = int(docker("port", proxy, "8080/tcp").rsplit(":", 1)[1])

            def request(path, method="GET", headers=None):
                connection = http.client.HTTPConnection("127.0.0.1", port, timeout=5)
                try:
                    connection.request(method, path, headers=headers or {})
                    response = connection.getresponse()
                    return response.status, dict(response.getheaders()), response.read()
                finally:
                    connection.close()

            deadline = time.monotonic() + 20
            while True:
                try:
                    self.assertEqual(200, request("/health")[0])
                    break
                except (OSError, http.client.HTTPException):
                    if time.monotonic() >= deadline:
                        self.fail("Nginx did not start: " + docker("logs", proxy))
                    time.sleep(0.2)

            # A small burst reaches the upstream normally, including invalid logins.
            for _ in range(5):
                status, headers, _ = request("/api/v1/auth/login", "POST")
                self.assertEqual(401, status)
                self.assertEqual("true", headers.get("X-Upstream-Reached"))

            # Query strings and forged forwarding headers must not bypass the quota.
            for suffix in ("", "?attempt=other"):
                status, headers, body = request(
                    "/api/v1/auth/login" + suffix, "POST",
                    {"X-Forwarded-For": "203.0.113.17", "X-Real-IP": "203.0.113.18"},
                )
                self.assertEqual(429, status)
                self.assertEqual("LOGIN_RATE_LIMITED", json.loads(body)["code"])
                self.assertGreater(int(headers["Retry-After"]), 0)
                self.assertNotIn("X-Upstream-Reached", headers)
                self.assertIn("application/problem+json", headers["Content-Type"])
                self.assertEqual("no-store", headers["Cache-Control"])

            for path in ("/api/v1/auth/csrf", "/api/v1/auth/me", "/api/v1/users"):
                self.assertEqual(200, request(path)[0])

            status, headers, _ = request("/api/v1/auth/login", "OPTIONS")
            self.assertEqual(401, status)  # The stub answers; preflight is not throttled.
            self.assertEqual("true", headers.get("X-Upstream-Reached"))

            # One slot refills; rejected attempts do not extend the wait indefinitely.
            time.sleep(12.1)
            self.assertEqual(401, request("/api/v1/auth/login", "POST")[0])
        finally:
            subprocess.run(["docker", "rm", "-f", proxy, backend], capture_output=True)
            subprocess.run(["docker", "network", "rm", network], capture_output=True)


if __name__ == "__main__":
    unittest.main()
