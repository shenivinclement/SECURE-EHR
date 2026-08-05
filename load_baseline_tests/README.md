# SecureEHR API Load & Baseline Testing Suite (100 Virtual Users)

Automated **Load & Baseline Testing Framework** for the **SecureEHR** system backend (`secureehr-backend-clean`), simulating **100 concurrent virtual users (VUs)** running continuously for **1 minute (60 seconds)** across **300 unique API load test cases**.

---

## Directory Structure

```
c:\Users\shen2\Downloads\SECURE EHR\load_baseline_tests\
├── config\
│   └── load_config.py                 # Load test parameters (100 VUs, 60s duration, target host)
├── engine\
│   └── load_engine.py                 # Virtual user benchmark simulation engine & metrics recorder
├── suites\                            # 300 Unique Load Test Cases
│   ├── test_auth_load.py              # 75 Auth & JWT Token Load Test Cases
│   ├── test_records_load.py           # 75 Medical Records & AES Decryption Load Test Cases
│   ├── test_consent_load.py           # 75 Zero-Knowledge Consent & Blockchain Sync Load Cases
│   └── test_search_ai_load.py         # 75 Doctor Search, ER Map & AI Assistant Load Cases
├── utils\
│   └── excel_load_report_generator.py # Formatted Excel Report Builder (.xlsx) using openpyxl
├── run_load_tests.py                  # Main Python Load Test Runner
└── run_load_tests.bat                  # 1-Click Executable Windows Batch Launcher
```

---

## Load Simulation Parameters & SLA Benchmarks

- **Concurrent Virtual Users**: 100 Virtual Users (VUs)
- **Execution Duration**: 60 Seconds Continuous Load (1 Minute)
- **Target RPS Benchmark**: ~120 – 180 Requests Per Second (req/sec)
- **Response Time Target (Avg)**: < 300 ms
- **SLA Max Latency**: < 1500 ms
- **Success Rate Target**: 100.0%

---

## Benchmark Metrics Captured in Excel Report

1. **Requests Per Second (RPS)**: Measure of system throughput (e.g. 145.8 req/sec).
2. **Average Response Time**: Mean request handling time (e.g. 224.5 ms).
3. **Minimum Response Time (Min)**: Fastest response recorded (e.g. 48.2 ms).
4. **Maximum Response Time (Max)**: Slowest response recorded (e.g. 1380.0 ms).
5. **95th Percentile Latency (P95)**: Latency threshold under which 95% of requests complete.
6. **99th Percentile Latency (P99)**: Latency threshold under which 99% of requests complete.
7. **Success Rate**: Percentage of successful requests (200 OK) without 5xx server errors.

---

## Execution Instructions

### 1-Click Executable Windows Launcher
Double-click or run from terminal:
```cmd
run_load_tests.bat
```

### Command Line Execution
```bash
python run_load_tests.py
```
