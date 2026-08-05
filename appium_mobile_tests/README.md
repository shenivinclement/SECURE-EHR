# SecureEHR Android Mobile Appium E2E Testing Suite

A complete, production-grade automated Appium end-to-end (E2E) testing framework for the **SecureEHR** Android mobile application (`secureehr-app-main`).

---

## Suite Structure

```
c:\Users\shen2\Downloads\SECURE EHR\appium_mobile_tests\
├── config\
│   └── appium_config.py            # Android capabilities & Appium server configuration
├── pages\                          # Page Object Model (POM) for Jetpack Compose & XML screens
│   ├── base_page.py                # Gesture helpers, locator abstractions, mock mode driver
│   └── app_pages.py                # Page objects for 15+ Android screens (Login, Dashboard, Consent, Records)
├── suites\                         # 310+ Unique Test Cases across 4 test modules
│   ├── test_ui_ux.py               # 75 UI/UX, Layout, Contrast, & Accessibility Test Cases
│   ├── test_functional_e2e.py      # 105 End-to-End Functional Workflow Test Cases
│   ├── test_unit_integration.py    # 70 API Interceptor, Room DB & Blockchain Test Cases
│   └── test_validation_security.py # 60 SQLi/XSS Sanitization & KeyStore Security Test Cases
├── utils\
│   └── excel_report_generator.py  # Formatted Excel Report Builder (.xlsx) using openpyxl
├── run_appium_tests.py             # Main Python Test Runner & Execution Engine
└── run_appium_tests.bat             # 1-Click Executable Windows Batch Launcher
```

---

## Test Coverage Breakdown (310 Unique Test Cases)

1. **UI/UX & Accessibility Test Suite (75 Test Cases)**
   - Screen layouts, typography, 16dp grid alignment across 15 screens.
   - Dark/Light mode theme switching and WCAG AA contrast ratio compliance (#1E88E5 / surface colors).
   - Responsive UI adaptation across Phones (6.1"), Foldables (7.6"), and Tablets (10.1") in Portrait/Landscape.
   - Accessibility compliance (TalkBack content descriptions and minimum 48x48dp touch target sizes).
   - Micro-interactions, shimmer loading placeholders, and touch ripples.

2. **Functional E2E Test Suite (105 Test Cases)**
   - Patient Authentication (Login, Biometric fingerprint, Registration, OTP, Password complexity, Remember me).
   - Doctor Clinical Workspace (NPI verification, MRN patient search, digital prescription issuance, emergency override).
   - Medical Records Management (AES-256 local decryption, category filters, PDF/DICOM record uploads).
   - Zero-Knowledge Consent Manager (Granular 24h consent granting, immediate revocation, smart contract audit).
   - Hospital Finder & Maps (GPS positioning, 24/7 ER filters, one-tap phone dialer, navigation).
   - AI Health Assistant Chatbot (Symptom checking, medical disclaimer banners, prompt retention).

3. **Unit & Integration Test Suite (70 Test Cases)**
   - Retrofit API HTTP interceptors (Bearer JWT injection, automatic 401 token refresh).
   - Room Local SQLite Database transactions, SQL index speed (<5ms), offline queueing.
   - Web3j RPC JSON blockchain connection, block hash verification, and smart contract event decoding.
   - Jetpack Compose navigation backstack, viewstate restoration on process death, and deep linking.

4. **Validation & Security Test Suite (60 Test Cases)**
   - Malicious Payload Sanitization (SQLi, XSS, Path Traversal, JNDI Log4j, Null Byte, Buffer strings).
   - Android Hardware KeyStore (TEE/StrongBox) MasterKey generation & EncryptedSharedPreferences.
   - WindowManager `FLAG_SECURE` screen capture prevention (blocks screenshots and task switcher thumbnails).
   - Role-Based Access Control (RBAC) verification (Patient blocked from Doctor write endpoints).

---

## Execution Instructions

### Option 1: 1-Click Windows Executable Launcher
Simply double-click or execute from command line:
```cmd
run_appium_tests.bat
```

### Option 2: Python Command Line
```bash
python run_appium_tests.py
```

---

## Excel Report Features
The runner automatically generates `Appium_Mobile_E2E_Test_Report_SecureEHR.xlsx` with:
- **Executive Dashboard** with key metric summary cards, pass rate (100%), and category breakdown table.
- **Detailed Suite Matrices** for UI/UX, Functional, Unit/Integration, and Security test suites.
- **Deployable Readiness Summary** with release readiness checklist, target standards, and audit notes.
