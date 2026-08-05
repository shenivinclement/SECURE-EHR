"""Read-only discovery of concrete object ids (patient/record/visit/consent)
belonging to each demo account, used to build IDOR probes. No writes."""
from common import req, auth_header


def _get_json(path, token):
    status, elapsed, r = req("GET", path, headers=auth_header(token))
    if status == 200 and r is not None:
        try:
            return r.json()
        except Exception:
            return None
    return None


def build_fixtures(tokens):
    fx = {}
    for who in ("patientA", "patientB"):
        tok = tokens.get(who)
        me = _get_json("/patients/me", tok)
        records = _get_json("/records", tok) or []
        visits = _get_json("/visits", tok) or []
        consents = _get_json("/consent", tok) or []
        fx[who] = {
            "patient_id": me.get("id") if isinstance(me, dict) else None,
            "record_id": records[0]["id"] if records else None,
            "visit_id": visits[0]["id"] if visits else None,
            "consent_id": consents[0]["id"] if consents else None,
        }

    all_patients = _get_json("/patients", tokens.get("patientA")) or []
    fx["all_patient_ids"] = [p["id"] for p in all_patients if isinstance(p, dict) and "id" in p]

    hospitals = _get_json("/hospitals", None) or []
    fx["hospital_id"] = hospitals[0]["id"] if hospitals else None

    return fx
