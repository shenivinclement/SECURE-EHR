"""
Sets up 5 demo doctor accounts and matching patient accounts with consents.

Doctors (real names from healthcare_dataset.csv, no "Dr." prefix stored):
  Heather Lin   → dr.heather.lin@secureehr.test
  Jamie Hodges  → dr.jamie.hodges@secureehr.test
  David Hubbard → dr.david.hubbard@secureehr.test
  Lisa Parker   → dr.lisa.parker@secureehr.test
  David Lyons   → dr.david.lyons@secureehr.test

Patients (new demo accounts from CSV data):
  James Brown    → james.brown@secureehr.test   (Diabetes,     Heather Lin primary)
  Michael Williams → michael.williams@secureehr.test (Hypertension, Jamie Hodges primary)
  David Johnson  → david.johnson@secureehr.test (Asthma,       David Hubbard primary)
  John Johnson   → john.johnson@secureehr.test  (Arthritis,    Lisa Parker primary)
  Robert Smith   → robert.smith@secureehr.test  (Cancer,       David Lyons primary)

Cross-consents:
  Heather Lin  ← Michael Williams (active)
  Jamie Hodges ← James Brown      (active)
  David Lyons  ← John Johnson     (active)
"""

import csv
import requests
import time
from datetime import datetime
from pathlib import Path

BASE_URL = "http://localhost:8000"
CSV_PATH = Path(__file__).parent / "healthcare_dataset.csv"
PASSWORD = "Demo@1234"


DOCTORS = [
    {"name": "Heather Lin",   "email": "dr.heather.lin@secureehr.test"},
    {"name": "Jamie Hodges",  "email": "dr.jamie.hodges@secureehr.test"},
    {"name": "David Hubbard", "email": "dr.david.hubbard@secureehr.test"},
    {"name": "Lisa Parker",   "email": "dr.lisa.parker@secureehr.test"},
    {"name": "David Lyons",   "email": "dr.david.lyons@secureehr.test"},
]

# Patient data confirmed from healthcare_dataset.csv
PATIENTS = [
    {
        "name": "James Brown",
        "email": "james.brown@secureehr.test",
        "gender": "Male",
        "blood_group": "A+",
        "condition": "Diabetes",
        "primary_doctor": "Heather Lin",
        "hospital": "and Brown Lawson, Gardner",
        "specialization": "Endocrinology",
    },
    {
        "name": "Michael Williams",
        "email": "michael.williams@secureehr.test",
        "gender": "Male",
        "blood_group": "A-",
        "condition": "Hypertension",
        "primary_doctor": "Jamie Hodges",
        "hospital": "LLC Taylor",
        "specialization": "Cardiology",
    },
    {
        "name": "David Johnson",
        "email": "david.johnson@secureehr.test",
        "gender": "Male",
        "blood_group": "A+",
        "condition": "Asthma",
        "primary_doctor": "David Hubbard",
        "hospital": "Lee-Brennan",
        "specialization": "Pulmonology",
    },
    {
        "name": "John Johnson",
        "email": "john.johnson@secureehr.test",
        "gender": "Male",
        "blood_group": "B+",
        "condition": "Arthritis",
        "primary_doctor": "Lisa Parker",
        "hospital": "and Russo Ruiz Barron,",
        "specialization": "Rheumatology",
    },
    {
        "name": "Robert Smith",
        "email": "robert.smith@secureehr.test",
        "gender": "Male",
        "blood_group": "O+",
        "condition": "Cancer",
        "primary_doctor": "David Lyons",
        "hospital": "Cohen-White",
        "specialization": "Oncology",
    },
]

# Cross-consents: (patient_name, doctor_name, hospital, specialization)
CROSS_CONSENTS = [
    ("Michael Williams", "Heather Lin",  "and Brown Lawson, Gardner", "Endocrinology"),
    ("James Brown",      "Jamie Hodges", "LLC Taylor",                "Cardiology"),
    ("John Johnson",     "David Lyons",  "Cohen-White",               "Oncology"),
]


def log(msg):
    print(f"[{datetime.now():%H:%M:%S}] {msg}")


def register_user(name, email, role):
    r = requests.post(
        f"{BASE_URL}/auth/register",
        json={"name": name, "email": email, "password": PASSWORD, "role": role},
        timeout=60,
    )
    if r.status_code == 201:
        uid = r.json()["id"]
        log(f"  Registered {role} '{name}' (id={uid})")
        return uid
    if r.status_code == 400:
        log(f"  '{name}' already registered")
        return "EXISTS"
    log(f"  Register FAILED [{r.status_code}]: {r.text[:120]}")
    return None


def login(email):
    r = requests.post(
        f"{BASE_URL}/auth/login",
        json={"email": email, "password": PASSWORD},
        timeout=60,
    )
    if r.status_code != 200:
        log(f"  Login FAILED [{r.status_code}]: {r.text[:80]}")
        return None, None
    tok = r.json()["access_token"]
    me = requests.get(f"{BASE_URL}/auth/me", headers={"Authorization": f"Bearer {tok}"}, timeout=60)
    uid = me.json()["id"] if me.status_code == 200 else None
    return tok, uid


def get_or_create_patient_profile(tok, uid, p):
    r = requests.get(
        f"{BASE_URL}/patients/me",
        headers={"Authorization": f"Bearer {tok}"},
        timeout=60,
    )
    if r.status_code == 200:
        pid = r.json()["id"]
        log(f"  Patient profile exists (patient_id={pid})")
        return pid

    body = {
        "name": p["name"],
        "user_id": uid,
        "gender": p["gender"],
        "blood_group": p["blood_group"],
    }
    r = requests.post(
        f"{BASE_URL}/patients",
        json=body,
        headers={"Authorization": f"Bearer {tok}"},
        timeout=60,
    )
    if r.status_code in (200, 201):
        pid = r.json()["id"]
        log(f"  Created patient profile (patient_id={pid})")
        return pid
    log(f"  Patient create FAILED [{r.status_code}]: {r.text[:120]}")
    return None


def import_records_from_csv(tok, pid, condition, n=10):
    rows = []
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row["Medical Condition"] == condition:
                rows.append(row)
            if len(rows) >= n:
                break

    imported = 0
    for row in rows:
        body = {
            "patient_id": pid,
            "diagnosis": row["Medical Condition"],
            "symptoms": f"{row['Admission Type']} admission - Test result: {row['Test Results']}",
            "treatment": row["Medication"],
            "medications": row["Medication"],
            "notes": (
                f"Doctor: {row['Doctor'].title()} | Hospital: {row['Hospital']} | "
                f"Discharged: {row['Discharge Date'][:10]} | "
                f"Billing: ${float(row['Billing Amount']):,.2f} | "
                f"Room: {int(float(row['Room Number']))} | Insurance: {row['Insurance Provider']}"
            ),
            "record_date": row["Date of Admission"][:10],
        }
        r = requests.post(
            f"{BASE_URL}/records",
            json=body,
            headers={"Authorization": f"Bearer {tok}"},
            timeout=60,
        )
        if r.status_code in (200, 201):
            imported += 1
        time.sleep(0.15)
    log(f"  Imported {imported}/{len(rows)} medical records")


def list_consents(tok):
    r = requests.get(
        f"{BASE_URL}/consent",
        headers={"Authorization": f"Bearer {tok}"},
        timeout=60,
    )
    return r.json() if r.status_code == 200 else []


def grant_consent(tok, doctor_name, hospital, specialization, expiry="2027-12-31"):
    r = requests.post(
        f"{BASE_URL}/consent/grant",
        json={
            "doctor_name": doctor_name,
            "hospital_name": hospital,
            "specialization": specialization,
            "expiry_date": expiry,
            "purpose": f"Access to {specialization} records",
        },
        headers={"Authorization": f"Bearer {tok}"},
        timeout=60,
    )
    if r.status_code in (200, 201):
        cid = r.json()["id"]
        log(f"  Granted consent id={cid} -> {doctor_name} @ {hospital}")
        return cid
    log(f"  Grant FAILED [{r.status_code}]: {r.text[:120]}")
    return None


# ── patient_id lookup cache (name → id)
patient_id_cache: dict[str, int] = {}


def resolve_patient_id(name):
    return patient_id_cache.get(name)


# ── Step 1: Register doctors ──────────────────────────────────────────────────
log("=== Registering doctor accounts ===")
for doc in DOCTORS:
    register_user(doc["name"], doc["email"], "doctor")
    time.sleep(0.3)

# ── Step 2: Register patients, create profiles, import records ────────────────
log("\n=== Setting up patient accounts ===")
for p in PATIENTS:
    log(f"--- {p['name']} ---")
    register_user(p["name"], p["email"], "patient")
    time.sleep(0.3)

    tok, uid = login(p["email"])
    if not tok:
        log("  Login failed, skipping")
        continue

    pid = get_or_create_patient_profile(tok, uid, p)
    if not pid:
        continue
    patient_id_cache[p["name"]] = pid

    import_records_from_csv(tok, pid, p["condition"])
    time.sleep(0.3)

# ── Step 3: Grant primary consents ───────────────────────────────────────────
log("\n=== Granting primary consents ===")
for p in PATIENTS:
    log(f"--- {p['name']} -> {p['primary_doctor']} ---")
    tok, _ = login(p["email"])
    if not tok:
        continue

    pid = resolve_patient_id(p["name"])
    if not pid:
        log("  Patient id unknown, skipping")
        continue

    # Skip if active consent already exists for this doctor
    existing = list_consents(tok)
    already = any(
        c["doctor_name"] == p["primary_doctor"] and c["status"] == "active"
        for c in existing
    )
    if already:
        log(f"  Active consent for {p['primary_doctor']} already exists")
        continue

    grant_consent(tok, p["primary_doctor"], p["hospital"], p["specialization"])
    time.sleep(0.3)

# ── Step 4: Grant cross-consents ─────────────────────────────────────────────
log("\n=== Granting cross-consents ===")

# Build a lookup from patient name → (email, hospital, specialization)
patient_meta = {p["name"]: p for p in PATIENTS}

for patient_name, doctor_name, hospital, specialization in CROSS_CONSENTS:
    log(f"--- {patient_name} -> {doctor_name} (cross) ---")
    meta = patient_meta[patient_name]
    tok, _ = login(meta["email"])
    if not tok:
        continue

    pid = resolve_patient_id(patient_name)
    if not pid:
        log("  Patient id unknown, skipping")
        continue

    existing = list_consents(tok)
    already = any(
        c["doctor_name"] == doctor_name and c["status"] == "active"
        for c in existing
    )
    if already:
        log(f"  Cross-consent for {doctor_name} already exists")
        continue

    grant_consent(tok, doctor_name, hospital, specialization)
    time.sleep(0.3)

log("\n=== DONE ===")
log("Doctor accounts:")
for d in DOCTORS:
    log(f"  {d['email']}  /  {PASSWORD}  (name='{d['name']}')")
log("Patient accounts:")
for p in PATIENTS:
    log(f"  {p['email']}  /  {PASSWORD}")

# ── Step 5: Verify — log in as each doctor and call GET /doctor/patients ──────
log("\n=== Verifying doctor patient access ===")
for doc in DOCTORS:
    tok, _ = login(doc["email"])
    if not tok:
        log(f"  {doc['name']}: login FAILED")
        continue
    r = requests.get(
        f"{BASE_URL}/doctor/patients",
        headers={"Authorization": f"Bearer {tok}"},
        timeout=60,
    )
    if r.status_code != 200:
        log(f"  {doc['name']}: /doctor/patients FAILED [{r.status_code}]: {r.text[:80]}")
        continue
    patients_seen = r.json()
    names = [p["name"] for p in patients_seen]
    log(f"  {doc['name']} sees {len(names)} patient(s): {names}")
