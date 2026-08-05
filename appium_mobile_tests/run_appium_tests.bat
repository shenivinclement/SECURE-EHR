@echo off
TITLE SecureEHR Android Appium E2E Test Suite Launcher
color 0A
echo =========================================================================
echo   SECURE EHR - ANDROID APPIUM E2E TEST SUITE & EXCEL REPORT GENERATOR
echo =========================================================================
echo.
echo Running 310+ Unique Appium E2E Test Cases for SecureEHR Mobile Application...
echo.
cd /d "%~dp0"
python run_appium_tests.py
echo.
echo Launching Excel Analysis Report...
if exist "Appium_Mobile_E2E_Test_Report_SecureEHR.xlsx" (
    start "" "Appium_Mobile_E2E_Test_Report_SecureEHR.xlsx"
)
pause
