import requests

BASE = "https://secureehr-backend.onrender.com"

# Login as Robert Smith
tok = requests.post(f"{BASE}/auth/login",
    json={"email":"robert.smith@secureehr.test","password":"Demo@1234"}).json()["access_token"]
h = {"Authorization": f"Bearer {tok}"}

# Check current consents
consents = requests.get(f"{BASE}/consent", headers=h).json()
print("=== Robert Smith consents ===")
for c in consents:
    print(f"  {c.get('doctor_name','?'):25s} status={c.get('status','?'):10s} id={c.get('id','?')}")

# Try granting consent for David Lyons
print()
print("=== Granting consent to David Lyons ===")
r = requests.post(f"{BASE}/consent/grant",
    json={"doctor_name":"David Lyons","hospital_name":"Cohen-White","specialization":"Oncology","expiry_date":"2027-12-31"},
    headers=h)
print(f"Status: {r.status_code}")
print(f"Response: {r.json()}")

# Check consents again
print()
consents2 = requests.get(f"{BASE}/consent", headers=h).json()
print("=== After grant ===")
for c in consents2:
    print(f"  {c.get('doctor_name','?'):25s} status={c.get('status','?'):10s} id={c.get('id','?')}")

# Now check doctor side
print()
tok2 = requests.post(f"{BASE}/auth/login",
    json={"email":"dr.david.lyons@secureehr.test","password":"Demo@1234"}).json()["access_token"]
h2 = {"Authorization": f"Bearer {tok2}"}
patients = requests.get(f"{BASE}/doctor/patients", headers=h2).json()
print("=== Dr David Lyons patients ===")
print(patients)
