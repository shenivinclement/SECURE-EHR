"""DAST test runner for SecureEHR (local, authorized). Orchestrates all
categories, writes automated_test/report.json + savepoint.json, and prints
a terminal summary. Only ever issues GET / HEAD / safe POST requests --
see categories/*.py docstrings for exactly what "safe" means per category.
"""
import json
import sys
import time
import os

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from common import get_tokens, BASE_URL
from fixtures import build_fixtures
from categories import authn_bypass, authz_privesc, idor, rbac_matrix, token_tampering, injection_probe, rate_limiting, hardcoded_creds

CATEGORY_MODULES = [
    ("authn_bypass", authn_bypass),
    ("authz_privesc", authz_privesc),
    ("idor", idor),
    ("rbac_matrix", rbac_matrix),
    ("token_tampering", token_tampering),
    ("injection_probe", injection_probe),
    ("rate_limiting", rate_limiting),
    ("hardcoded_creds", hardcoded_creds),
]

HERE = os.path.dirname(os.path.abspath(__file__))


def main():
    print(f"=== SecureEHR DAST Test Run -- target {BASE_URL} ===")
    print("[setup] Logging in demo accounts...")
    tokens = get_tokens()
    for k, v in tokens.items():
        print(f"  {k}: {'OK' if v else 'LOGIN FAILED'}")

    print("[setup] Discovering fixture object ids (read-only)...")
    fixtures = build_fixtures(tokens)
    with open(os.path.join(HERE, "savepoint.json"), "w") as fh:
        json.dump({"tokens_ok": {k: bool(v) for k, v in tokens.items()}, "fixtures": fixtures}, fh, indent=2)

    all_results = []
    t0 = time.time()
    for name, mod in CATEGORY_MODULES:
        print(f"\n[running] {name} ...")
        try:
            results = mod.run(tokens, fixtures)
        except Exception as e:
            print(f"  !! category crashed: {e}")
            results = [{
                "endpoint": "(harness)", "method": "N/A", "role": "n/a", "status": None,
                "expected_status": None, "finding": False, "severity": "info",
                "response_time_ms": None, "test_category": name, "note": f"category crashed: {e}",
                "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
            }]
        n_findings = sum(1 for r in results if r["finding"])
        symbol = "[FAIL]" if n_findings else "[OK]"
        print(f"  {symbol} {len(results)} test cases, {n_findings} finding(s)")
        all_results.extend(results)

    total_ms = (time.time() - t0) * 1000

    report_path = os.path.join(HERE, "report.json")
    with open(report_path, "w") as fh:
        json.dump(all_results, fh, indent=2)

    sev_order = {"critical": 0, "high": 1, "medium": 2, "low": 3, "info": 4}
    findings = [r for r in all_results if r["finding"]]
    findings.sort(key=lambda r: sev_order.get(r["severity"], 9))

    by_sev = {}
    for r in findings:
        by_sev[r["severity"]] = by_sev.get(r["severity"], 0) + 1

    print("\n" + "=" * 60)
    print("DAST SUMMARY")
    print("=" * 60)
    print(f"Endpoints discovered : 36 (see discovered_endpoints.json)")
    print(f"Test cases executed  : {len(all_results)}")
    print(f"Total findings       : {len(findings)}")
    for sev in ("critical", "high", "medium", "low"):
        if by_sev.get(sev):
            print(f"  {sev.upper():8s}: {by_sev[sev]}")
    print(f"Total duration       : {total_ms/1000:.1f}s")
    print("=" * 60)

    print("\nTOP FINDINGS:")
    for r in findings[:15]:
        mark = "[!]" if r["severity"] in ("critical", "high") else "[~]"
        print(f"  {mark} [{r['severity'].upper()}] {r['method']} {r['endpoint']} ({r['test_category']}) -- {r['note']}")

    print(f"\n[+] Full report written to: {report_path}")


if __name__ == "__main__":
    main()
