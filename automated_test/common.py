"""Shared helpers for the SecureEHR DAST test harness. All requests target
BASE_URL only (read below) and are GET/HEAD/safe-POST unless explicitly
marked otherwise -- no destructive writes are issued by this harness."""
import time
import requests

BASE_URL = "http://localhost:8000"
TIMEOUT = 10

# Demo accounts (seeded by secureehr-backend-clean/reset_and_import_demo.py).
# Two patients + two doctors so cross-account IDOR/RBAC checks are possible.
DEMO_USERS = {
    "patientA": ("james.brown@secureehr.test", "Demo@1234", "patient"),
    "patientB": ("michael.williams@secureehr.test", "Demo@1234", "patient"),
    "doctorA": ("dr.heather.lin@secureehr.test", "Demo@1234", "doctor"),
    "doctorB": ("dr.jamie.hodges@secureehr.test", "Demo@1234", "doctor"),
}


def login(email, password):
    try:
        r = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": password}, timeout=TIMEOUT)
        if r.status_code == 200:
            return r.json().get("access_token")
    except requests.RequestException:
        pass
    return None


def get_tokens():
    tokens = {}
    for key, (email, pw, _role) in DEMO_USERS.items():
        tokens[key] = login(email, pw)
    return tokens


def auth_header(token):
    return {"Authorization": f"Bearer {token}"} if token else {}


def req(method, path, headers=None, json_body=None, params=None):
    t0 = time.time()
    try:
        r = requests.request(method, f"{BASE_URL}{path}", headers=headers or {}, json=json_body, params=params, timeout=TIMEOUT)
        elapsed = (time.time() - t0) * 1000
        return r.status_code, elapsed, r
    except requests.RequestException as e:
        elapsed = (time.time() - t0) * 1000
        return None, elapsed, str(e)


def rec(endpoint, method, role, status, expected_status, finding, severity, elapsed_ms, category, note):
    return {
        "endpoint": endpoint,
        "method": method,
        "role": role,
        "status": status,
        "expected_status": expected_status,
        "finding": bool(finding),
        "severity": severity,
        "response_time_ms": round(elapsed_ms, 1) if elapsed_ms is not None else None,
        "test_category": category,
        "note": note,
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
    }


def status_matches(status, expected_list):
    return status in expected_list
