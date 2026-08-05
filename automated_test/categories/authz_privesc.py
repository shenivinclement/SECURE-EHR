"""Category 2: AuthZ / privilege escalation -- calling a higher-privilege
(doctor-only) endpoint with a lower-privilege (patient) role token.
A 2xx response is a finding.
"""
from common import req, rec, auth_header
from endpoints import DOCTOR_ONLY


def _fill(path, fixtures, who):
    p = fixtures.get(who, {})
    return (
        path.replace("{patient_id}", str(p.get("patient_id") or 1))
    )


def run(tokens, fixtures):
    results = []

    for who in ("patientA", "patientB"):
        patient_token = tokens.get(who)
        for method, path_tpl in DOCTOR_ONLY:
            path = _fill(path_tpl, fixtures, "patientA")
            status, elapsed, _r = req(method, path, headers=auth_header(patient_token))
            finding = status is not None and 200 <= status < 300
            results.append(rec(
                path, method, who, status, "403", finding,
                "high" if finding else "info", elapsed, "authz_privesc",
                f"Patient token ({who}) used against a doctor-only endpoint" + (" -- ESCALATION: patient received a 2xx response!" if finding else " -- correctly forbidden"),
            ))

    return results
