@echo off
TITLE SecureEHR API Load & Baseline Test Suite Launcher (100 VUs)
color 0B
echo =========================================================================
echo   SECURE EHR API - 100 VIRTUAL USER LOAD TEST & EXCEL REPORT GENERATOR
echo =========================================================================
echo.
echo Running 100 Virtual User (1 Minute Benchmark) Load Test across 300 Test Cases...
echo.
cd /d "%~dp0"
python run_load_tests.py
echo.
echo Opening Excel Load Analysis Report...
if exist "Load_Test_Report_SecureEHR.xlsx" (
    start "" "Load_Test_Report_SecureEHR.xlsx"
)
pause
