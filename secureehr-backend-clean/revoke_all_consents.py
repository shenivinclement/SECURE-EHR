"""
Revokes all active consents across the 5 active demo patient accounts
(the reset_and_import_demo.py persona set).
"""

import requests
import time
from datetime import datetime

BASE_URL = "http://localhost:8000"
PASSWORD = "Demo@1234"

DEMO_PATIENTS = [
    "james.brown@secureehr.test",
    "michael.williams@secureehr.test",
    "david.johnson@secureehr.test",
    "john.johnson@secureehr.test",
    "robert.smith@secureehr.test",
]


def log(m):
    print(f"[{datetime.now():%H:%M:%S}] {m}")


def login(email):
    r = requests.post(
        f"{BASE_URL}/auth/login",
        json={"email": email, "password": PASSWORD},
        timeout=60,
    )
    if r.status_code != 200:
        log(f"  Login FAILED [{r.status_code}]: {r.text[:80]}")
        return None
    return r.json()["access_token"]


def get_consents(tok):
    r = requests.get(
        f"{BASE_URL}/consent",
        headers={"Authorization": f"Bearer {tok}"},
        timeout=60,
    )
    return r.json() if r.status_code == 200 else []


def revoke_consent(tok, consent_id):
    r = requests.post(
        f"{BASE_URL}/consent/revoke",
        json={"consent_id": consent_id},
        headers={"Authorization": f"Bearer {tok}"},
        timeout=60,
    )
    return r.status_code == 200


total_revoked = 0

for email in DEMO_PATIENTS:
    log(f"--- {email} ---")
    tok = login(email)
    if not tok:
        log("  Skipping (login failed)")
        continue

    consents = get_consents(tok)
    if not consents:
        log("  No consents found")
        continue

    for c in consents:
        cid = c["id"]
        if revoke_consent(tok, cid):
            log(f"  Revoked consent id={cid}")
            total_revoked += 1
        else:
            log(f"  FAILED to revoke consent id={cid}")
        time.sleep(0.2)

log(f"\n=== DONE: {total_revoked} consents revoked across {len(DEMO_PATIENTS)} patients ===")
