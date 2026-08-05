"""Category 6: Injection probes (detection only). Sends SQLi/NoSQLi-shaped
payloads into login fields, the doctor-search query param, and the
hospitals condition filter, then flags anomalous status codes (500),
stack-trace leakage, or timing outliers. Does not attempt to extract data.
"""
from common import req, rec, auth_header

PAYLOADS = [
    "' OR '1'='1",
    "' OR '1'='1' --",
    "'; DROP TABLE users; --",
    "1' UNION SELECT NULL--",
    "\" OR \"1\"=\"1",
    "{\"$ne\": null}",
    "{\"$gt\": \"\"}",
    "<script>alert(1)</script>",
    "../../../../etc/passwd",
    "' OR SLEEP(5)-- -",
    "admin'--",
    "%27%20OR%20%271%27%3D%271",
]

BASELINE_MULTIPLIER = 8  # flag as a timing anomaly if a payload is this many times slower than the baseline


def _looks_like_leak(text):
    if not text:
        return False
    markers = ["Traceback (most recent call last)", "sqlalchemy.exc", "psycopg2.", "sqlite3.", "<html", "at line", "syntax error"]
    return any(m in text for m in markers)


def run(tokens, fixtures):
    results = []
    doctor_tok = tokens.get("doctorA")

    # baseline timing for the search endpoint
    _s, baseline_ms, _r = req("GET", "/doctor/search", headers=auth_header(doctor_tok), params={"q": "James"})
    baseline_ms = baseline_ms or 50

    for payload in PAYLOADS:
        # 1) /auth/login -- must NEVER return 200 for a garbage/injected credential pair
        status, elapsed, r = req("POST", "/auth/login", json_body={"email": payload, "password": payload})
        finding = status == 200 or status == 500
        note = "Injection payload in login email+password"
        if status == 200:
            note += " -- CRITICAL: authenticated with an injection payload as credentials!"
        elif status == 500:
            note += " -- server error (possible unhandled exception / injection surface)"
        else:
            note += " -- correctly rejected"
        if r is not None and _looks_like_leak(getattr(r, "text", "")):
            finding = True
            note += " -- response body appears to leak a stack trace / DB error"
        results.append(rec("/auth/login", "POST", "none", status, "401/400", finding, "high" if finding else "info", elapsed, "injection_probe", note))

        # 2) /doctor/search?q=<payload> -- authenticated, should just return an empty/normal result set
        status2, elapsed2, r2 = req("GET", "/doctor/search", headers=auth_header(doctor_tok), params={"q": payload})
        finding2 = status2 == 500
        if r2 is not None and _looks_like_leak(getattr(r2, "text", "")):
            finding2 = True
        if elapsed2 and elapsed2 > baseline_ms * BASELINE_MULTIPLIER:
            finding2 = True
            timing_note = f" -- TIMING ANOMALY: {elapsed2:.0f}ms vs ~{baseline_ms:.0f}ms baseline (possible blind SQLi)"
        else:
            timing_note = ""
        results.append(rec("/doctor/search", "GET", "doctor", status2, "200 (empty/normal result)", finding2, "high" if finding2 else "info", elapsed2, "injection_probe", f"Injection payload in ?q= search param{timing_note}" + (" -- error leak" if finding2 and not timing_note else "")))

        # 3) /hospitals?condition=<payload> -- public, should just return normal/empty results
        status3, elapsed3, r3 = req("GET", "/hospitals", params={"condition": payload})
        finding3 = status3 == 500
        if r3 is not None and _looks_like_leak(getattr(r3, "text", "")):
            finding3 = True
        results.append(rec("/hospitals", "GET", "none", status3, "200", finding3, "high" if finding3 else "info", elapsed3, "injection_probe", "Injection payload in ?condition= filter param" + (" -- error/leak detected" if finding3 else " -- handled safely")))

        # 4) /ai/chat -- authenticated; message field, watches for 500s / leaked internals
        status4, elapsed4, r4 = req("POST", "/ai/chat", headers=auth_header(doctor_tok), json_body={"message": payload})
        finding4 = status4 == 500
        if r4 is not None and _looks_like_leak(getattr(r4, "text", "")):
            finding4 = True
        results.append(rec("/ai/chat", "POST", "doctor", status4, "200", finding4, "medium" if finding4 else "info", elapsed4, "injection_probe", "Injection/prompt-injection payload in chat message" + (" -- error/leak detected" if finding4 else " -- handled safely")))

    return results
