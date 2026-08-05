"""Category 8: Hardcoded credentials -- static scan of the codebase for
committed secrets not covered by .gitignore. No live requests.
"""
import os
import re

ROOTS = [
    r"C:\Users\shen2\Downloads\SECURE EHR\secureehr-backend-clean",
    r"C:\Users\shen2\Downloads\SECURE EHR\secureehr-web\src",
    r"C:\Users\shen2\Downloads\SECURE EHR\secureehr-app-main\app\src",
]

SKIP_DIRS = {".venv", "venv", "node_modules", "build", ".git", "__pycache__", ".gradle", "dist"}

PATTERNS = [
    # only a bare literal counts -- `X_KEY = os.getenv("X", ...)` is env-backed, matched separately below
    ("hardcoded static admin/API key constant", re.compile(r'_?(ADMIN|API)_KEY\s*=\s*["\'][^"\']{6,}["\']')),
    ("hardcoded password/secret literal (non-test file)", re.compile(r'\b(password|secret|passwd)\s*[:=]\s*["\'][^"\']{4,}["\']', re.IGNORECASE)),
    ("weak secret fallback baked into os.getenv() default", re.compile(r'(SECRET_KEY|_?(ADMIN|API)_KEY)\s*=\s*os\.getenv\([^)]*,\s*["\'][^"\']{6,}["\']\)')),
    ("AWS access key pattern", re.compile(r'AKIA[0-9A-Z]{16}')),
    ("generic bearer/token literal", re.compile(r'["\']Bearer\s+[A-Za-z0-9\-_\.]{20,}["\']')),
    ("private key block", re.compile(r'-----BEGIN (RSA |EC )?PRIVATE KEY-----')),
]

# a getenv-with-default is a weaker issue than a bare committed literal
FALLBACK_LABEL = "weak secret fallback baked into os.getenv() default"

DEMO_SCRIPT_NAMES = {"reset_and_import_demo.py", "setup_demo_doctors.py", "revoke_all_consents.py", "debug_consent.py"}


def run(tokens, fixtures):
    results = []
    hits = []

    for root in ROOTS:
        if not os.path.isdir(root):
            continue
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
            for fname in filenames:
                if not fname.endswith((".py", ".js", ".jsx", ".ts", ".tsx", ".kt", ".env")):
                    continue
                fpath = os.path.join(dirpath, fname)
                is_demo_script = fname in DEMO_SCRIPT_NAMES
                try:
                    with open(fpath, "r", encoding="utf-8", errors="ignore") as fh:
                        text = fh.read()
                except Exception:
                    continue
                for label, pattern in PATTERNS:
                    for m in pattern.finditer(text):
                        line_no = text[: m.start()].count("\n") + 1
                        hits.append({
                            "file": fpath,
                            "line": line_no,
                            "label": label,
                            "is_demo_script": is_demo_script,
                        })

    for h in hits:
        if h["is_demo_script"]:
            severity = "low"
        elif h["label"] == FALLBACK_LABEL:
            severity = "medium"
        else:
            severity = "high"
        note = f"{h['label']} found in {h['file']}:{h['line']}"
        if h["is_demo_script"]:
            note += " (demo/seed data script, expected but should never point at production)"
        elif h["label"] == FALLBACK_LABEL:
            note += " (env var is honoured, but the committed default silently applies when it is unset)"
        results.append(({
            "endpoint": h["file"],
            "method": "STATIC",
            "role": "n/a",
            "status": None,
            "expected_status": "no hardcoded secret",
            "finding": True,
            "severity": severity,
            "response_time_ms": None,
            "test_category": "hardcoded_creds",
            "note": note,
            "timestamp": None,
        }))

    if not hits:
        results.append({
            "endpoint": "(codebase scan)", "method": "STATIC", "role": "n/a", "status": None,
            "expected_status": "no hardcoded secret", "finding": False, "severity": "info",
            "response_time_ms": None, "test_category": "hardcoded_creds", "note": "No hardcoded secrets matched scan patterns",
            "timestamp": None,
        })

    return results
