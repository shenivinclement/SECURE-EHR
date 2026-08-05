"""Static endpoint inventory used by the DAST harness. Mirrors
discovered_endpoints.json. GET-safe endpoints are exercised live; endpoints
whose only "real" test would require a destructive write (DELETE, or
POST /admin/reset) are marked destructive=True and are NOT called live --
they are reported from static code review only.
"""

PUBLIC = [
    ("GET", "/"),
    ("GET", "/docs"),
    ("GET", "/redoc"),
    ("GET", "/openapi.json"),
    ("GET", "/hospitals"),
]

# (method, path_template, destructive)
AUTH_REQUIRED = [
    ("GET", "/auth/me", False),
    ("PATCH", "/auth/me/two-factor", False),
    ("POST", "/ai/chat", False),
    ("GET", "/consent", False),
    ("POST", "/consent/grant", False),
    ("POST", "/consent/revoke", False),
    ("DELETE", "/consent/{consent_id}", True),
    ("POST", "/hospitals", False),
    ("PUT", "/hospitals/{hospital_id}", True),
    ("DELETE", "/hospitals/{hospital_id}", True),
    ("GET", "/patients", False),
    ("POST", "/patients", False),
    ("GET", "/patients/me", False),
    ("PUT", "/patients/me", True),
    ("GET", "/patients/{patient_id}", False),
    ("PATCH", "/patients/me/research-sharing", False),
    ("GET", "/records", False),
    ("POST", "/records", False),
    ("GET", "/records/{record_id}", False),
    ("GET", "/visits", False),
    ("POST", "/visits", False),
    ("GET", "/visits/{visit_id}", False),
    ("DELETE", "/visits/{visit_id}", True),
]

DOCTOR_ONLY = [
    ("GET", "/doctor/profile"),
    ("GET", "/doctor/dashboard"),
    ("GET", "/doctor/patients"),
    ("GET", "/doctor/patients/{patient_id}/records"),
    ("GET", "/doctor/patients/{patient_id}/visits"),
    ("GET", "/doctor/search"),
    ("GET", "/doctor/consents"),
]

ADMIN_KEY_PROTECTED = [
    ("POST", "/admin/reset", True),
]
