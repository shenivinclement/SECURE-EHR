"""Category 4: RBAC matrix -- every role token x every role-restricted
endpoint, actual vs expected. GET and safe-POST only.
"""
from common import req, rec, auth_header
from endpoints import PUBLIC, AUTH_REQUIRED, DOCTOR_ONLY

ROLES = ["none", "patientA", "patientB", "doctorA", "doctorB"]

SAFE_BODIES = {
    "/ai/chat": {"message": "rbac probe"},
    "/consent": None,
}

# Endpoints whose "correct" outcome for an authenticated role legitimately
# depends on data state, not just role -- e.g. a doctor with no active
# consent from that specific patient is SUPPOSED to get 403, and a doctor
# has no /patients/me profile by design. Both status codes count as
# "expected" here; only something outside the set is a finding.
CONTEXTUAL_OK = {
    ("GET", "/patients/me"): {"doctor": {403, 404}},
    ("GET", "/doctor/patients/{patient_id}/records"): {"doctor": {200, 403}},
    ("GET", "/doctor/patients/{patient_id}/visits"): {"doctor": {200, 403}},
}

# Object-scoped endpoints: the harness fills the path with patientA's own
# object ids, so ONLY patientA is the legitimate owner. After the ownership
# fixes, every non-owner (including doctors) must get 404 -- that is the
# correct, secure outcome, not a failure. A non-owner receiving 200 here
# would be a real IDOR regression.
OWNER_SCOPED = {
    ("GET", "/patients/{patient_id}"),
    ("GET", "/records/{record_id}"),
    ("GET", "/visits/{visit_id}"),
}
OWNER_ROLE = "patientA"


def _fill(path, fixtures):
    a = fixtures.get("patientA", {})
    return (
        path.replace("{patient_id}", str(a.get("patient_id") or 1))
            .replace("{record_id}", str(a.get("record_id") or 1))
            .replace("{visit_id}", str(a.get("visit_id") or 1))
            .replace("{hospital_id}", str(fixtures.get("hospital_id") or 1))
            .replace("{consent_id}", str(a.get("consent_id") or 1))
    )


def _role_kind(role):
    return "none" if role == "none" else ("doctor" if role.startswith("doctor") else "patient")


def _expected_range(bucket, role):
    if bucket == "public":
        return (200, 399)
    if bucket == "auth_any":
        return (200, 299) if role != "none" else (400, 403)
    if bucket == "doctor_only":
        return (200, 299) if role.startswith("doctor") else (400, 403)
    raise ValueError(bucket)


def _check(method, path_tpl, path, role, status, bucket):
    if (method, path_tpl) in OWNER_SCOPED and role != "none":
        if role == OWNER_ROLE:
            return (status == 200), "200 (owner)"
        return (status == 404), "404 (non-owner, ownership-scoped)"

    contextual = CONTEXTUAL_OK.get((method, path_tpl))
    if contextual and _role_kind(role) in contextual:
        allowed = contextual[_role_kind(role)]
        ok = status in allowed
        expected_label = "/".join(str(s) for s in sorted(allowed)) + " (context-dependent)"
        return ok, expected_label
    lo, hi = _expected_range(bucket, role)
    ok = status is not None and lo <= status <= hi
    return ok, f"{lo}-{hi}"


def run(tokens, fixtures):
    results = []

    for method, path in PUBLIC:
        for role in ROLES:
            tok = None if role == "none" else tokens.get(role)
            status, elapsed, _r = req(method, path, headers=auth_header(tok))
            ok, expected_label = _check(method, path, path, role, status, "public")
            results.append(rec(path, method, role, status, expected_label, not ok, "medium" if not ok else "info", elapsed, "rbac_matrix", "Public endpoint access check"))

    for method, path_tpl, destructive in AUTH_REQUIRED:
        if destructive or method in ("PUT", "PATCH", "DELETE"):
            continue  # only GET/POST are exercised with a valid token in this harness
        path = _fill(path_tpl, fixtures)
        body = SAFE_BODIES.get(path_tpl)
        if method == "POST" and path_tpl not in SAFE_BODIES:
            continue  # skip POSTs whose body-shape is already covered by idor.py, to avoid duplicate writes
        for role in ROLES:
            tok = None if role == "none" else tokens.get(role)
            status, elapsed, _r = req(method, path, headers=auth_header(tok), json_body=body)
            ok, expected_label = _check(method, path_tpl, path, role, status, "auth_any")
            results.append(rec(path, method, role, status, expected_label, not ok, "medium" if not ok else "info", elapsed, "rbac_matrix", "Authenticated (any role) endpoint access check"))

    for method, path_tpl in DOCTOR_ONLY:
        path = _fill(path_tpl, fixtures)
        params = {"q": "James"} if path_tpl == "/doctor/search" else None
        for role in ROLES:
            tok = None if role == "none" else tokens.get(role)
            status, elapsed, _r = req(method, path, headers=auth_header(tok), params=params)
            ok, expected_label = _check(method, path_tpl, path, role, status, "doctor_only")
            results.append(rec(path, method, role, status, expected_label, not ok, "high" if not ok else "info", elapsed, "rbac_matrix", "Doctor-only endpoint access check"))

    return results
