#!/usr/bin/env python3
"""
Full demo data reset and import for SecureEHR.

Steps:
  1  Clean slate via POST /admin/reset (retries until Render deploys the endpoint;
     wipes users, patients, records, visits, consents AND hospitals)
  2  Register each doctor as a new user with role="doctor" (names match CSV
     "Doctor" values exactly, no "Dr." prefix stored, so /doctor endpoints'
     name-based consent matching works)
  3  Register each patient as a new user
  4  Create patient profiles — capture patient_id from response
  5  Import ALL matching CSV rows as medical records
  6  Derive a `hospitals` row for every distinct hospital name referenced by those
     CSV rows (cure rate / condition rates computed from the full CSV, placeholder
     coordinates/rating/specialty assigned deterministically) and create them via
     POST /hospitals
  7  Create hospital visits from CSV rows, linked to the derived hospitals via
     hospital_id (no more free-text hospital_name)
  8  Create consents from unique doctors in CSV rows
  9  Top up with guaranteed primary/cross consents between each patient and
     the 5 seeded doctors (PRIMARY_DOCTOR / CROSS_CONSENTS below). Step 8's
     CSV-derived consents are realistic but incidental — they rarely name one
     of our 5 seeded doctors, so without this step most demo doctor logins
     would see zero patients. Grants here reuse real (doctor, hospital) pairs
     that already exist in that patient's own CSV rows, and POST /consent/grant
     is an idempotent upsert-to-active per (patient, doctor_name), so this is
     safe to run even where Step 8 already created — and possibly revoked —
     a same-named consent by coincidence.

This is the single source of truth for demo accounts — it always seeds both
patients AND doctors, so there is no separate script to remember to run.
"""

import csv
import hashlib
import os
import requests
import pandas as pd
import time
from datetime import datetime
from collections import Counter, defaultdict
from dotenv import load_dotenv

load_dotenv()

BASE_URL = "http://localhost:8000"
CSV_PATH = "healthcare_dataset.csv"
ADMIN_KEY = os.getenv("ADMIN_RESET_KEY")
if not ADMIN_KEY:
    raise SystemExit(
        "ADMIN_RESET_KEY is not set. Set the same value here and on the API "
        "(see .env) before running the demo reset."
    )

PATIENTS = [
    {"name": "James Brown",      "email": "james.brown@secureehr.test",    "password": "Demo@1234", "primary_condition": "Diabetes"},
    {"name": "Michael Williams", "email": "michael.williams@secureehr.test","password": "Demo@1234", "primary_condition": "Hypertension"},
    {"name": "David Johnson",    "email": "david.johnson@secureehr.test",  "password": "Demo@1234", "primary_condition": "Asthma"},
    {"name": "John Johnson",     "email": "john.johnson@secureehr.test",   "password": "Demo@1234", "primary_condition": "Arthritis"},
    {"name": "Robert Smith",     "email": "robert.smith@secureehr.test",   "password": "Demo@1234", "primary_condition": "Cancer"},
]

# Doctor names match the CSV "Doctor" column exactly (no "Dr." prefix stored) —
# /doctor routes match consents to a doctor by comparing current_user.name
# against Consent.doctor_name, so these strings must line up with the CSV data
# and with the doctor_name values import_consents() derives from CSV rows below.
DOCTORS = [
    {"name": "Heather Lin",   "email": "dr.heather.lin@secureehr.test",   "password": "Demo@1234"},
    {"name": "Jamie Hodges",  "email": "dr.jamie.hodges@secureehr.test",  "password": "Demo@1234"},
    {"name": "David Hubbard", "email": "dr.david.hubbard@secureehr.test", "password": "Demo@1234"},
    {"name": "Lisa Parker",   "email": "dr.lisa.parker@secureehr.test",   "password": "Demo@1234"},
    {"name": "David Lyons",   "email": "dr.david.lyons@secureehr.test",   "password": "Demo@1234"},
]

# Guaranteed primary doctor per patient: patient name -> (doctor_name, hospital_name).
# Each pairing is a real (Doctor, Hospital) combination taken from that patient's own
# CSV rows (verified against healthcare_dataset.csv), so the hospital_name always
# matches a hospital actually created for that patient in Step 6. Specialization is
# derived from the patient's own primary_condition via CONDITION_DEPT below.
PRIMARY_DOCTOR = {
    "James Brown":      ("Heather Lin",   "and Brown Lawson, Gardner"),
    "Michael Williams": ("Jamie Hodges",  "LLC Taylor"),
    "David Johnson":    ("David Hubbard", "Lee-Brennan"),
    "John Johnson":     ("Lisa Parker",   "and Russo Ruiz Barron,"),
    "Robert Smith":     ("David Lyons",   "Cohen-White"),
}

# Extra cross-consents so a few doctors share overlapping patients (second-opinion
# style access): (patient_name, doctor_name, hospital_name, specialization). The
# specialization here is the target doctor's own specialty, not the patient's.
CROSS_CONSENTS = [
    ("Michael Williams", "Heather Lin",  "and Brown Lawson, Gardner", "Endocrinology"),
    ("James Brown",      "Jamie Hodges", "LLC Taylor",                "Cardiology"),
    ("John Johnson",     "David Lyons",  "Cohen-White",               "Oncology"),
]

CONDITION_DEPT = {
    "Diabetes":     "Endocrinology",
    "Hypertension": "Cardiology",
    "Asthma":       "Pulmonology",
    "Arthritis":    "Rheumatology",
    "Cancer":       "Oncology",
}

CONDITION_TO_SPECIALTY = {
    "Arthritis":    "Orthopedics & Rheumatology",
    "Hypertension": "Cardiology",
    "Diabetes":     "Endocrinology & Diabetology",
    "Asthma":       "Pulmonology",
    "Cancer":       "Oncology",
    "Obesity":      "Bariatrics & General Medicine",
}

# Arbitrary placeholder service-area bounding box for map markers — not tied to
# any real city. Coordinates are assigned deterministically per hospital name so
# they stay stable across resets.
PLACEHOLDER_LAT_RANGE = (20.00, 20.40)
PLACEHOLDER_LON_RANGE = (78.00, 78.40)

# Fictional street/area/city components used to synthesize a plausible-looking
# but clearly non-real demo address — consistent with the placeholder coords
# above not being tied to any real city.
STREET_NAMES = ["MG Road", "Station Road", "Ring Road", "Civil Lines",
                "Park Street", "College Road", "Hospital Road", "Nehru Nagar",
                "Gandhi Chowk", "Sadar Bazaar"]
AREA_NAMES   = ["Sector 4", "Sector 9", "Sector 12", "North Zone",
                "East Extension", "Old Town", "New Colony", "Model Town"]
CITY_NAMES   = ["Chandanpur", "Ravigaon", "Sitapuram", "Kesarwadi",
                "Amritnagar", "Devgiri", "Bhavpur", "Lakshmipet"]

EXPIRY_CYCLE = ["2026-09-30", "2026-12-31", "2027-03-31", "2027-06-30"]


def log(msg):
    print(f"[{datetime.now():%H:%M:%S}] {msg}", flush=True)


def _placeholder_coords(name: str):
    h = int(hashlib.md5(name.encode()).hexdigest(), 16)
    lat = PLACEHOLDER_LAT_RANGE[0] + (h % 10_000) / 10_000 * (PLACEHOLDER_LAT_RANGE[1] - PLACEHOLDER_LAT_RANGE[0])
    lon = PLACEHOLDER_LON_RANGE[0] + ((h // 10_000) % 10_000) / 10_000 * (PLACEHOLDER_LON_RANGE[1] - PLACEHOLDER_LON_RANGE[0])
    return round(lat, 4), round(lon, 4)


def _placeholder_contact(name: str):
    """Deterministic placeholder address + phone, stable across resets.

    Uses a different hash salt than _placeholder_coords so address/phone
    don't correlate with lat/lon.
    """
    h = int(hashlib.md5((name + "_contact").encode()).hexdigest(), 16)
    house_no = 1 + (h % 200)
    street   = STREET_NAMES[(h // 200) % len(STREET_NAMES)]
    area     = AREA_NAMES[(h // 200 // len(STREET_NAMES)) % len(AREA_NAMES)]
    city     = CITY_NAMES[(h // 200 // len(STREET_NAMES) // len(AREA_NAMES)) % len(CITY_NAMES)]
    pincode  = 100000 + (h % 900000)
    address  = f"{house_no}, {street}, {area}, {city} - {pincode}"

    # Indian mobile numbers start 6-9; derive remaining 9 digits from hash
    first_digit = 6 + (h % 4)
    rest        = str(h)[-9:].rjust(9, "0")
    phone       = f"+91-{first_digit}{rest}"

    return address, phone


# ── Step 1 ──────────────────────────────────────────────────────────────────

def admin_reset():
    log("=== STEP 1: Clean Slate ===")
    MAX_WAIT = 900  # 15 minutes
    interval = 30
    elapsed = 0
    while elapsed <= MAX_WAIT:
        try:
            r = requests.post(
                f"{BASE_URL}/admin/reset",
                headers={"x-admin-key": ADMIN_KEY},
                timeout=60,
            )
            if r.status_code == 200:
                log(f"  Reset OK: {r.json()}")
                return True
            if r.status_code in (404, 405, 422):
                if elapsed == 0:
                    log("  Admin endpoint not yet live — waiting for Render to deploy...")
                else:
                    log(f"  Still waiting... ({elapsed}s elapsed)")
                time.sleep(interval)
                elapsed += interval
                continue
            log(f"  Reset FAILED [{r.status_code}]: {r.text[:200]}")
            return False
        except requests.RequestException as e:
            log(f"  Request error: {e}. Retrying in {interval}s...")
            time.sleep(interval)
            elapsed += interval
    log("  Timed out waiting for admin reset endpoint.")
    return False


# ── Step 2 ───────────────────────────────────────────────────────────────────

def register_doctor(d):
    r = requests.post(f"{BASE_URL}/auth/register", json={
        "name": d["name"], "email": d["email"],
        "password": d["password"], "role": "doctor",
    }, timeout=60)
    if r.status_code == 201:
        log(f"  Registered doctor '{d['name']}' (id={r.json()['id']})")
        return True
    if r.status_code == 400:
        log(f"  Doctor '{d['name']}' already registered")
        return True
    log(f"  Doctor register FAILED [{r.status_code}]: {r.text[:120]}")
    return False


def register_doctors():
    log("=== Registering doctor accounts ===")
    ok = 0
    for d in DOCTORS:
        if register_doctor(d):
            ok += 1
        time.sleep(0.3)
    log(f"  Doctors: {ok}/{len(DOCTORS)} registered")
    return ok


# ── Steps 3 + 4 ─────────────────────────────────────────────────────────────

def register_and_create_patient(p, rows):
    # Register
    r = requests.post(f"{BASE_URL}/auth/register", json={
        "name": p["name"], "email": p["email"],
        "password": p["password"], "role": "patient",
    }, timeout=60)
    if r.status_code == 201:
        log(f"  Registered user_id={r.json()['id']}")
    elif r.status_code == 400:
        log("  User already exists, logging in")
    else:
        log(f"  Register FAILED [{r.status_code}]: {r.text[:80]}")
        return None, None, None

    # Login
    r = requests.post(f"{BASE_URL}/auth/login", json={
        "email": p["email"], "password": p["password"],
    }, timeout=60)
    if r.status_code != 200:
        log(f"  Login FAILED [{r.status_code}]: {r.text[:80]}")
        return None, None, None
    token = r.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Resolve user_id
    me = requests.get(f"{BASE_URL}/auth/me", headers=headers, timeout=60)
    uid = me.json()["id"] if me.status_code == 200 else None
    log(f"  Logged in user_id={uid}")

    # Check if patient profile already exists
    existing = requests.get(f"{BASE_URL}/patients/me", headers=headers, timeout=60)
    if existing.status_code == 200:
        pid = existing.json()["id"]
        log(f"  Patient profile already exists patient_id={pid}")
        return token, uid, pid

    # Derive profile from CSV rows
    genders = [row["Gender"].strip().title() for row in rows if row.get("Gender")]
    bloods  = [row["Blood Type"].strip()       for row in rows if row.get("Blood Type")]
    ages    = [int(row["Age"])                 for row in rows
               if row.get("Age") and str(row["Age"]).strip().isdigit()]

    gender      = Counter(genders).most_common(1)[0][0] if genders else "Male"
    blood_group = Counter(bloods).most_common(1)[0][0]  if bloods  else "O+"
    avg_age     = int(sum(ages) / len(ages))             if ages    else 35
    dob         = f"{2026 - avg_age}-06-15"

    # Create patient profile
    r = requests.post(f"{BASE_URL}/patients", json={
        "name": p["name"], "user_id": uid,
        "date_of_birth": dob, "gender": gender, "blood_group": blood_group,
    }, headers=headers, timeout=60)
    if r.status_code in (200, 201):
        pid = r.json()["id"]
        log(f"  Created patient_id={pid}  gender={gender}  blood={blood_group}  dob={dob}")
        return token, uid, pid
    log(f"  Patient create FAILED [{r.status_code}]: {r.text[:80]}")
    return token, uid, None


# ── Step 5 ───────────────────────────────────────────────────────────────────

def import_records(token, pid, rows, hospital_ids):
    log(f"  Importing {len(rows)} record(s)  patient_id={pid}")
    headers = {"Authorization": f"Bearer {token}"}
    ok = 0
    for row in rows:
        billing = max(0.0, float(row.get("Billing Amount") or 0))
        hosp_name = row["Hospital"].strip()
        body = {
            "patient_id": pid,
            "diagnosis":  row["Medical Condition"].strip(),
            "symptoms":   f"{row['Admission Type'].strip()} admission - Test result: {row['Test Results'].strip()}",
            "treatment":  row["Medication"].strip(),
            "medications": row["Medication"].strip(),
            "notes": (
                f"Doctor: {row['Doctor'].strip().title()} | "
                f"Hospital: {hosp_name} | "
                f"Discharged: {str(row['Discharge Date'])[:10]} | "
                f"Billing: ${billing:,.2f} | "
                f"Room: {int(float(row['Room Number']))} | "
                f"Insurance: {row['Insurance Provider'].strip()}"
            ),
            "record_date": str(row["Date of Admission"])[:10],
            "doctor_id":   None,
            "hospital_id": hospital_ids.get(hosp_name),
        }
        r = requests.post(f"{BASE_URL}/records", json=body, headers=headers, timeout=60)
        if r.status_code in (200, 201):
            ok += 1
        else:
            log(f"    Record FAILED [{r.status_code}]: {r.text[:80]}")
        time.sleep(0.15)
    log(f"  Records: {ok}/{len(rows)} imported")
    return ok


# ── Step 6 ───────────────────────────────────────────────────────────────────

def compute_hospital_stats(needed_names):
    """Stream the full CSV and compute cure-rate stats only for hospitals we need."""
    stats = defaultdict(lambda: {
        "total": 0, "normal": 0,
        "conditions": defaultdict(lambda: {"total": 0, "normal": 0}),
    })
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            hosp = row["Hospital"].strip()
            if hosp not in needed_names:
                continue
            cond   = row["Medical Condition"].strip()
            result = row["Test Results"].strip()
            stats[hosp]["total"] += 1
            if result == "Normal":
                stats[hosp]["normal"] += 1
            stats[hosp]["conditions"][cond]["total"] += 1
            if result == "Normal":
                stats[hosp]["conditions"][cond]["normal"] += 1
    return stats


def build_hospital_payloads(stats):
    records = []
    for name, data in stats.items():
        lat, lon = _placeholder_coords(name)
        address, phone = _placeholder_contact(name)
        overall_cure = round(data["normal"] / data["total"] * 100, 1) if data["total"] else 0.0

        condition_rates = {
            cond: round(v["normal"] / v["total"] * 100, 1)
            for cond, v in data["conditions"].items()
        }

        top_cond = max(data["conditions"].items(), key=lambda x: x[1]["total"])[0]
        specialty = CONDITION_TO_SPECIALTY.get(top_cond, "General Medicine")

        # Scale rating 3.8-4.6 based on overall cure rate
        rating = round(3.8 + max(0.0, min(1.0, (overall_cure - 18) / (53 - 18))) * 0.8, 1)

        records.append({
            "name":            name,
            "address":         address,
            "latitude":        lat,
            "longitude":       lon,
            "specialty":       specialty,
            "rating":          rating,
            "cure_rate":       overall_cure,
            "beds":            max(20, data["total"] * 8),
            "phone":           phone,
            "condition_rates": condition_rates,
        })
    return records


def create_hospitals(token, payloads):
    headers = {"Authorization": f"Bearer {token}"}
    name_to_id = {}
    ok = 0
    for h in payloads:
        r = requests.post(f"{BASE_URL}/hospitals", json=h, headers=headers, timeout=60)
        if r.status_code in (200, 201):
            name_to_id[h["name"]] = r.json()["id"]
            ok += 1
        else:
            log(f"    Hospital create FAILED [{r.status_code}] {h['name']}: {r.text[:100]}")
        time.sleep(0.1)
    log(f"  Hospitals: {ok}/{len(payloads)} created")
    return name_to_id


# ── Step 7 ───────────────────────────────────────────────────────────────────

def import_visits(token, pid, rows, hospital_ids):
    log(f"  Creating {len(rows)} visit(s)  patient_id={pid}")
    headers = {"Authorization": f"Bearer {token}"}
    ok = 0
    for row in rows:
        condition = row["Medical Condition"].strip()
        dept      = CONDITION_DEPT.get(condition, "General Medicine")
        billing   = max(0.0, float(row.get("Billing Amount") or 0))
        hosp_name = row["Hospital"].strip()
        hosp_id   = hospital_ids.get(hosp_name)
        if hosp_id is None:
            log(f"    Skipping visit — no hospital_id for '{hosp_name}'")
            continue
        body = {
            "patient_id":   pid,
            "hospital_id":  hosp_id,
            "visit_date":   str(row["Date of Admission"])[:10],
            "reason":       condition,
            "doctor_name":  row["Doctor"].strip().title(),
            "department":   dept,
            "status":       "completed",
            "notes": (
                f"Discharge: {str(row['Discharge Date'])[:10]} | "
                f"Medication: {row['Medication'].strip()} | "
                f"Room: {int(float(row['Room Number']))} | "
                f"Insurance: {row['Insurance Provider'].strip()} | "
                f"Billing: ${billing:,.2f}"
            ),
        }
        r = requests.post(f"{BASE_URL}/visits", json=body, headers=headers, timeout=60)
        if r.status_code in (200, 201):
            ok += 1
        else:
            log(f"    Visit FAILED [{r.status_code}]: {r.text[:80]}")
        time.sleep(0.15)
    log(f"  Visits: {ok}/{len(rows)} created")
    return ok


# ── Step 8 ───────────────────────────────────────────────────────────────────

def import_consents(token, pid, rows):
    # Deduplicate (doctor, hospital) pairs, keeping first condition encountered
    seen = {}
    for row in rows:
        key = (row["Doctor"].strip().title(), row["Hospital"].strip())
        if key not in seen:
            seen[key] = row["Medical Condition"].strip()

    doctors = [(doc, hosp, cond) for (doc, hosp), cond in seen.items()]
    log(f"  Creating {len(doctors)} consent(s)  patient_id={pid}")
    headers = {"Authorization": f"Bearer {token}"}

    granted_ids = []
    for i, (doc, hosp, cond) in enumerate(doctors):
        spec   = CONDITION_DEPT.get(cond, "General Medicine")
        expiry = EXPIRY_CYCLE[i % 4]
        r = requests.post(f"{BASE_URL}/consent/grant", json={
            "patient_id":    pid,
            "doctor_name":   doc,
            "hospital_name": hosp,
            "specialization": spec,
            "expiry_date":   expiry,
            "purpose":       f"Access to {spec} records",
        }, headers=headers, timeout=60)
        if r.status_code in (200, 201):
            granted_ids.append(r.json()["id"])
        else:
            log(f"    Consent FAILED [{r.status_code}]: {r.text[:80]}")
        time.sleep(0.2)

    # Revoke roughly the first third for mixed statuses
    revoke_count = max(1, len(granted_ids) // 3)
    for cid in granted_ids[:revoke_count]:
        r = requests.post(f"{BASE_URL}/consent/revoke", json={"consent_id": cid},
                          headers=headers, timeout=60)
        if r.status_code != 200:
            log(f"    Revoke FAILED id={cid} [{r.status_code}]")
        time.sleep(0.1)

    log(f"  Consents: {len(granted_ids)} granted, {revoke_count} revoked")
    return len(granted_ids)


# ── Step 9 ───────────────────────────────────────────────────────────────────

def grant_named_consent(token, pid, doctor_name, hospital_name, specialization, expiry="2027-12-31"):
    headers = {"Authorization": f"Bearer {token}"}
    r = requests.post(f"{BASE_URL}/consent/grant", json={
        "patient_id":     pid,
        "doctor_name":    doctor_name,
        "hospital_name":  hospital_name,
        "specialization": specialization,
        "expiry_date":    expiry,
        "purpose":        f"Access to {specialization} records",
    }, headers=headers, timeout=60)
    if r.status_code in (200, 201):
        log(f"    Seeded consent -> {doctor_name} @ {hospital_name} (active)")
        return True
    log(f"    Seeded consent FAILED [{r.status_code}] -> {doctor_name}: {r.text[:80]}")
    return False


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    # Load CSV
    df = pd.read_csv(CSV_PATH)
    df["Date of Admission"] = pd.to_datetime(df["Date of Admission"]).dt.strftime("%Y-%m-%d")
    df["Discharge Date"]    = pd.to_datetime(df["Discharge Date"]).dt.strftime("%Y-%m-%d")
    log(f"CSV loaded: {len(df):,} rows")

    # Step 1
    if not admin_reset():
        log("WARNING: Clean slate failed — proceeding anyway (may create duplicates).")

    # Step 2
    log("")
    doctors_ok = register_doctors()
    if doctors_ok < len(DOCTORS):
        log("WARNING: not all doctor accounts registered — doctor logins may be incomplete.")

    totals = {"records": 0, "visits": 0, "consents": 0, "hospitals": 0}

    # ── Pass A: register patients + collect their CSV rows ──────────────────
    patient_ctx = {}
    for p in PATIENTS:
        log(f"\n=== {p['name']} ({p['primary_condition']}) ===")

        mask = df["Name"].str.strip().str.lower() == p["name"].lower()
        rows = df[mask].to_dict("records")
        log(f"  Found {len(rows)} CSV row(s)")
        if not rows:
            log("  WARNING: no CSV rows — skipping")
            continue

        token, uid, pid = register_and_create_patient(p, rows)
        if not pid:
            log("  Skipping — could not create patient profile")
            continue

        patient_ctx[p["name"]] = {"token": token, "pid": pid, "rows": rows}

    if not patient_ctx:
        log("No patients set up — aborting.")
        return

    # ── Derive + create hospitals from the union of all imported CSV rows ───
    log("\n=== Deriving placeholder hospitals from imported CSV rows ===")
    all_rows = [row for ctx in patient_ctx.values() for row in ctx["rows"]]
    needed_names = {row["Hospital"].strip() for row in all_rows}
    log(f"  {len(needed_names)} distinct hospital name(s) referenced")

    stats = compute_hospital_stats(needed_names)
    payloads = build_hospital_payloads(stats)

    bootstrap_token = next(iter(patient_ctx.values()))["token"]
    hospital_ids = create_hospitals(bootstrap_token, payloads)
    totals["hospitals"] = len(hospital_ids)

    # ── Pass B: records, visits, consents ────────────────────────────────────
    seeded_consents = 0
    for p in PATIENTS:
        name = p["name"]
        ctx = patient_ctx.get(name)
        if not ctx:
            continue
        log(f"\n--- {name}: records / visits / consents ---")
        totals["records"]  += import_records(ctx["token"], ctx["pid"], ctx["rows"], hospital_ids)
        totals["visits"]   += import_visits(ctx["token"], ctx["pid"], ctx["rows"], hospital_ids)
        totals["consents"] += import_consents(ctx["token"], ctx["pid"], ctx["rows"])

        # Step 9: top up with guaranteed primary/cross consents so every
        # seeded doctor has real, active patient(s) regardless of what the
        # CSV-derived consents above happened to grant/revoke.
        grants = []
        if name in PRIMARY_DOCTOR:
            doc, hosp = PRIMARY_DOCTOR[name]
            spec = CONDITION_DEPT.get(p["primary_condition"], "General Medicine")
            grants.append((doc, hosp, spec))
        grants += [(doc, hosp, spec) for (pname, doc, hosp, spec) in CROSS_CONSENTS if pname == name]

        for doc, hosp, spec in grants:
            if grant_named_consent(ctx["token"], ctx["pid"], doc, hosp, spec):
                seeded_consents += 1
    totals["consents"] += seeded_consents

    log(
        f"\n=== DONE: {doctors_ok} doctors, {len(patient_ctx)} patients, "
        f"{totals['hospitals']} hospitals, {totals['records']} records, "
        f"{seeded_consents} seeded doctor-patient consents, "
        f"{totals['visits']} visits, {totals['consents']} consents ==="
    )


if __name__ == "__main__":
    main()
