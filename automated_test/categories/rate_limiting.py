"""Category 7: Rate limiting -- bounded burst (~30 reqs) against
POST /auth/login with a wrong password, to confirm a throttle/lockout
exists. Safe: the password is always wrong, so no account is ever
authenticated and no lockout state (if any existed) would strand a real
user.
"""
import time
from common import req, rec

BURST_SIZE = 30
TARGET_EMAIL = "james.brown@secureehr.test"


def run(tokens, fixtures):
    results = []
    statuses = []
    t_start = time.time()

    for i in range(BURST_SIZE):
        status, elapsed, _r = req("POST", "/auth/login", json_body={"email": TARGET_EMAIL, "password": f"wrong-{i}"})
        statuses.append(status)
        results.append(rec(
            "/auth/login", "POST", "none", status, "401 (or 429 once throttled)", status == 429, "info", elapsed,
            "rate_limiting", f"Burst attempt {i + 1}/{BURST_SIZE} against the same account with a wrong password",
        ))

    t_total = (time.time() - t_start) * 1000
    throttled = any(s == 429 for s in statuses)
    all_401 = all(s == 401 for s in statuses if s is not None)

    finding = not throttled
    results.append(rec(
        "/auth/login", "POST", "none", statuses[-1] if statuses else None, "429 after repeated failures", finding,
        "medium" if finding else "info", t_total, "rate_limiting",
        f"Sent {BURST_SIZE} rapid failed-login attempts in {t_total:.0f}ms for the same account -- "
        + (f"no throttling observed (all {sum(1 for s in statuses if s==401)}/{BURST_SIZE} returned 401, no 429/lockout); no brute-force protection detected" if finding else "throttling detected (429 seen)"),
    ))

    return results
