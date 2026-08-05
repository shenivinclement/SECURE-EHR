# SecureEHR — CI/CD Automated Test Reporting

This directory wires the project's four automated test suites into GitHub
Actions, following the pipeline in *Git Live Automation Testing Setup*:

```
Developer push
      ↓
GitHub repository
      ↓
GitHub Actions trigger
      ↓
Test report rendering + security gate
      ↓
Pass / fail status + downloadable artifacts
```

## What the workflow does

[`workflows/test-reports.yml`](workflows/test-reports.yml) runs on every push
to `main`, every pull request, and on demand (**Run workflow** button). It:

1. Checks out the repo and installs `openpyxl`.
2. Renders every report spreadsheet into the **run summary** — so results are
   readable directly in the Actions tab, no download required.
3. Uploads each report to the **Artifacts** section as a downloadable `.xlsx`.
4. Fails the build if the security scan contains any critical or high finding.

## Reports published

| Report | Source | Coverage |
| --- | --- | --- |
| `E2E_Test_Report_SecureEHR.xlsx` | `selenium_e2e_tests/` | 300 web E2E cases (Selenium) |
| `Appium_Mobile_E2E_Test_Report_SecureEHR.xlsx` | `appium_mobile_tests/` | 310 Android cases — UI/UX, functional, unit/integration, validation & security |
| `Load_Test_Report_SecureEHR.xlsx` | `load_baseline_tests/` | 300 load cases @ 100 VUs, latency/RPS/SLA |
| `SecureEHR_Vulnerability_Report.xlsx` | `automated_test/` | 284 DAST cases — authn, authz, IDOR, RBAC, token tampering, injection, rate limiting |

## Artifacts

Each run publishes five artifacts (90-day retention):

- `SecureEHR-Test-Reports-<run number>` — everything, bundled
- `web-e2e-selenium-report`
- `mobile-e2e-appium-report`
- `load-performance-report`
- `api-security-dast-report`

Download them from the bottom of any workflow run page under **Artifacts**.

## Security gate

The final step parses `automated_test/report.json` and exits non-zero when
any **critical** or **high** severity finding is present, so a regression in
access control blocks the build. Medium and low findings are reported but do
not fail the run.

## Regenerating the reports locally

```bash
# Web E2E (needs the backend + frontend running)
python selenium_e2e_tests/run_e2e_suite.py

# API security DAST (needs the backend running)
python automated_test/run_all.py
python automated_test/generate_excel_report.py
```

Commit the regenerated `.xlsx` files and push — the workflow republishes them
automatically.
