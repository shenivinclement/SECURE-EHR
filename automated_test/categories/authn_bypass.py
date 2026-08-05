"""Category 1: AuthN bypass -- protected endpoints with no / malformed token.
A 2xx response is a finding (auth not enforced). DELETE endpoints are
included here (unauthenticated calls are expected to be rejected before any
mutation happens) but are never called with a valid token anywhere in this
harness.
"""
from common import req, rec
from endpoints import AUTH_REQUIRED, DOCTOR_ONLY

BODIES = {
    "/auth/me/two-factor": {"two_factor_enabled": False},
    "/ai/chat": {"message": "ping"},
    "/consent/grant": {"doctor_name": "Dr. Probe", "hospital_name": "Probe", "specialization": "Probe"},
    "/consent/revoke": {"consent_id": 1},
    "/hospitals": {"name": "Probe Hospital"},
    "/patients": {"name": "Probe Patient"},
    "/patients/me": {"name": "Probe"},
    "/records": {"patient_id": 1, "diagnosis": "probe"},
    "/visits": {"patient_id": 1, "hospital_id": 1},
}


def _fill(path, fixtures):
    a = fixtures.get("patientA", {})
    return (
        path.replace("{consent_id}", str(a.get("consent_id") or 1))
            .replace("{hospital_id}", str(fixtures.get("hospital_id") or 1))
            .replace("{patient_id}", str(a.get("patient_id") or 1))
            .replace("{record_id}", str(a.get("record_id") or 1))
            .replace("{visit_id}", str(a.get("visit_id") or 1))
    )


def _body_for(path):
    for suffix, body in BODIES.items():
        if path.endswith(suffix):
            return body
    return None


def run(tokens, fixtures):
    results = []
    all_endpoints = [(m, p) for m, p, _d in AUTH_REQUIRED] + [(m, p) for m, p in DOCTOR_ONLY]

    for method, path_tpl in all_endpoints:
        path = _fill(path_tpl, fixtures)
        body = _body_for(path_tpl)

        # (a) no Authorization header at all
        status, elapsed, _r = req(method, path, headers={}, json_body=body)
        finding = status is not None and 200 <= status < 300
        results.append(rec(
            path, method, "none", status, "401/403", finding,
            "critical" if finding else "info", elapsed, "authn_bypass",
            "No Authorization header sent" + (" -- endpoint returned 2xx with NO auth, auth not enforced!" if finding else " -- correctly rejected"),
        ))

        # (b) malformed / garbage bearer token
        status2, elapsed2, _r2 = req(method, path, headers={"Authorization": "Bearer not-a-real-jwt-token"}, json_body=body)
        finding2 = status2 is not None and 200 <= status2 < 300
        results.append(rec(
            path, method, "none", status2, "401", finding2,
            "critical" if finding2 else "info", elapsed2, "authn_bypass",
            "Malformed bearer token sent" + (" -- endpoint returned 2xx with a garbage token!" if finding2 else " -- correctly rejected"),
        ))

    return results
