"""Category 3: IDOR -- vary id params to reach another principal's object.
Read-only (GET) checks use each patient's real existing ids (no mutation).
The consent-revoke and object-creation checks operate on dedicated
DAST-PROBE objects created by this script (never on the user's real demo
data), and the consent probe is cleaned up (re-revoked) at the end
regardless of outcome. Everything here is GET or POST -- no PUT/DELETE.
"""
import time
from common import req, rec, auth_header


def run(tokens, fixtures):
    results = []
    a = fixtures.get("patientA", {})
    b = fixtures.get("patientB", {})
    tokA, tokB = tokens.get("patientA"), tokens.get("patientB")

    # --- GET /records/{record_id}: B reads A's record ---
    if a.get("record_id"):
        path = f"/records/{a['record_id']}"
        status, elapsed, _r = req("GET", path, headers=auth_header(tokB))
        finding = status == 200
        results.append(rec(
            path, "GET", "patientB->patientA", status, "403/404", finding,
            "critical" if finding else "info", elapsed, "idor",
            "Patient B fetched Patient A's medical record by id" + (" -- IDOR: full PHI record returned!" if finding else " -- correctly blocked"),
        ))
    else:
        results.append(rec("/records/{record_id}", "GET", "patientB->patientA", None, "403/404", False, "info", None, "idor", "Skipped -- Patient A has no existing record to probe with"))

    # --- GET /visits/{visit_id}: B reads A's visit ---
    if a.get("visit_id"):
        path = f"/visits/{a['visit_id']}"
        status, elapsed, _r = req("GET", path, headers=auth_header(tokB))
        finding = status == 200
        results.append(rec(
            path, "GET", "patientB->patientA", status, "403/404", finding,
            "critical" if finding else "info", elapsed, "idor",
            "Patient B fetched Patient A's hospital visit by id" + (" -- IDOR!" if finding else " -- correctly blocked"),
        ))
    else:
        results.append(rec("/visits/{visit_id}", "GET", "patientB->patientA", None, "403/404", False, "info", None, "idor", "Skipped -- Patient A has no existing visit to probe with"))

    # --- GET /patients/{patient_id}: B reads A's patient profile ---
    if a.get("patient_id"):
        path = f"/patients/{a['patient_id']}"
        status, elapsed, _r = req("GET", path, headers=auth_header(tokB))
        finding = status == 200
        results.append(rec(
            path, "GET", "patientB->patientA", status, "403/404", finding,
            "high" if finding else "info", elapsed, "idor",
            "Patient B fetched Patient A's full patient profile by id" + (" -- IDOR: PII returned!" if finding else " -- correctly blocked"),
        ))

    # --- GET /patients (list): does it leak any patient other than the caller's own? ---
    status, elapsed, r = req("GET", "/patients", headers=auth_header(tokB))
    leaks_others = False
    returned_ids = set()
    if status == 200 and r is not None:
        try:
            body = r.json()
            returned_ids = {p.get("id") for p in body if isinstance(p, dict)}
            # a self-scoped list must contain ONLY patient B's own id
            leaks_others = len(returned_ids - {b.get("patient_id")}) > 0
        except Exception:
            pass
    results.append(rec(
        "/patients", "GET", "patientB", status, "self-scoped list", leaks_others,
        "high" if leaks_others else "info", elapsed, "idor",
        f"Requested full patient list as Patient B -- {'returns patients other than the caller (broad PHI exposure)' if leaks_others else f'correctly self-scoped, returned only own record {returned_ids or {}}'}",
    ))

    # --- POST /consent/grant then /consent/revoke cross-account (self-contained probe object) ---
    probe_tag = f"DAST-Probe-{int(time.time())}"
    grant_body = {"doctor_name": probe_tag, "hospital_name": "DAST Probe Hospital", "specialization": "Probe"}
    gstatus, gelapsed, gr = req("POST", "/consent/grant", headers=auth_header(tokA), json_body=grant_body)
    probe_consent_id = None
    if gstatus == 200 and gr is not None:
        try:
            probe_consent_id = gr.json().get("id")
        except Exception:
            pass
    results.append(rec(
        "/consent/grant", "POST", "patientA", gstatus, "200", False, "info", gelapsed,
        "idor", f"Setup: created disposable probe consent '{probe_tag}' as Patient A for the revoke-IDOR check below",
    ))

    if probe_consent_id:
        path = "/consent/revoke"
        status, elapsed, _r = req("POST", path, headers=auth_header(tokB), json_body={"consent_id": probe_consent_id})
        finding = status == 200
        results.append(rec(
            path, "POST", "patientB->patientA", status, "403/404", finding,
            "critical" if finding else "info", elapsed, "idor",
            f"Patient B attempted to revoke Patient A's probe consent (id={probe_consent_id}) via consent_id in body" + (" -- IDOR: revoke succeeded cross-account!" if finding else " -- correctly blocked"),
        ))
        # cleanup: ensure the probe consent ends up revoked regardless of outcome
        req("POST", "/consent/revoke", headers=auth_header(tokA), json_body={"consent_id": probe_consent_id})
    else:
        results.append(rec("/consent/revoke", "POST", "patientB->patientA", None, "403/404", False, "info", None, "idor", "Skipped -- probe consent could not be created"))

    # --- POST /records: can Patient B create a record filed under Patient A's patient_id? ---
    # A finding requires the record to actually LAND on Patient A. A 201 whose
    # returned patient_id is Patient B's own means the server correctly ignored
    # the attacker-supplied patient_id and re-scoped it to the caller.
    if a.get("patient_id"):
        path = "/records"
        probe_body = {"patient_id": a["patient_id"], "diagnosis": f"{probe_tag} (harness test record, safe to delete)"}
        status, elapsed, r = req("POST", path, headers=auth_header(tokB), json_body=probe_body)
        landed_on = None
        if status == 201 and r is not None:
            try:
                landed_on = r.json().get("patient_id")
            except Exception:
                pass
        finding = status == 201 and landed_on == a["patient_id"]
        note = f"Patient B posted a medical record with patient_id={a['patient_id']} (Patient A) in the body"
        if finding:
            note += " -- IDOR: record was filed under Patient A by Patient B's session!"
        elif status == 201:
            note += f" -- correctly re-scoped to the caller's own patient_id ({landed_on}), attacker-supplied id ignored."
        else:
            note += " -- correctly rejected."
        results.append(rec(path, "POST", "patientB->patientA", status, "201 scoped to caller / 403", finding, "critical" if finding else "info", elapsed, "idor", note))

    return results
