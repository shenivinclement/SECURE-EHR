"""Re-run the categories that had real findings (idor, hardcoded_creds,
rate_limiting is unaffected/unfixed on purpose) against the now-patched
backend, and merge results back into report.json."""
import json
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from common import get_tokens
from fixtures import build_fixtures
from categories import idor, hardcoded_creds

HERE = os.path.dirname(os.path.abspath(__file__))

tokens = get_tokens()
fixtures = build_fixtures(tokens)

new_idor = idor.run(tokens, fixtures)
new_hc = hardcoded_creds.run(tokens, fixtures)

report_path = os.path.join(HERE, "report.json")
with open(report_path, encoding="utf-8") as fh:
    all_results = json.load(fh)

all_results = [r for r in all_results if r["test_category"] not in ("idor", "hardcoded_creds")]
all_results.extend(new_idor)
all_results.extend(new_hc)

with open(report_path, "w", encoding="utf-8") as fh:
    json.dump(all_results, fh, indent=2)

for label, results in (("idor", new_idor), ("hardcoded_creds", new_hc)):
    n = sum(1 for r in results if r["finding"])
    print(f"{label}: {len(results)} test cases, {n} finding(s)")
    for r in results:
        if r["finding"]:
            print(f"  [!] [{r['severity'].upper()}] {r['method']} {r['endpoint']} -> {r['status']} -- {r['note']}")

print(f"Total test cases in report.json: {len(all_results)}")
