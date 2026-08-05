import sys
import os
import time
import datetime
import traceback

sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from test_suite import TEST_CATALOG
from generate_excel_report import build_excel_report

def main():
    print(f"=== SECURE EHR — E2E Test Suite Execution ===")
    print(f"Total Test Cases Queued: {len(TEST_CATALOG)}")
    
    start_time_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()
    start_clock = time.time()
    
    driver = None

    passed_tests = []
    failed_tests = []
    execution_logs = []
    test_details = []

    passed_count = 0
    failed_count = 0

    for idx, t in enumerate(TEST_CATALOG, start=1):
        category = t["category"]
        test_name = t["test_name"]
        test_func = t["func"]
        
        test_start = time.time()
        now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        try:
            test_func(driver)
            duration = round(time.time() - test_start, 3)
            passed_count += 1
            
            passed_tests.append({
                "category": category,
                "test_name": test_name,
                "duration": duration if duration > 0 else 0.01,
                "status": "PASSED"
            })
            
            execution_logs.append({
                "timestamp": now_str,
                "level": "INFO",
                "message": f"[{category}] {test_name} → PASSED in {duration}s"
            })
            
            test_details.append({
                "category": category,
                "test_name": test_name,
                "status": "PASSED",
                "error_details": "None — test passed successfully."
            })
            
        except Exception as err:
            duration = round(time.time() - test_start, 3)
            failed_count += 1
            err_msg = str(err) or traceback.format_exc()
            
            failed_tests.append({
                "category": category,
                "test_name": test_name,
                "error": err_msg,
                "status": "FAILED",
                "timestamp": now_str
            })
            
            execution_logs.append({
                "timestamp": now_str,
                "level": "ERROR",
                "message": f"[{category}] {test_name} → FAILED in {duration}s: {err_msg[:100]}"
            })
            
            test_details.append({
                "category": category,
                "test_name": test_name,
                "status": "FAILED",
                "error_details": err_msg
            })

    end_clock = time.time()
    end_time_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()
    total_duration = round(end_clock - start_clock, 2)
    pass_rate = round((passed_count / len(TEST_CATALOG)) * 100, 2) if TEST_CATALOG else 0.0

    print("\n" + "="*50)
    print("TEST EXECUTION SUMMARY:")
    print(f"Total Tests Run : {len(TEST_CATALOG)}")
    print(f"Passed          : {passed_count}")
    print(f"Failed          : {failed_count}")
    print(f"Pass Rate       : {pass_rate}%")
    print(f"Total Duration  : {total_duration} seconds")
    print("="*50 + "\n")

    summary_data = {
        "suite_name": "SECURE EHR Web App — Full E2E Workflow",
        "total_tests": len(TEST_CATALOG),
        "passed": passed_count,
        "failed": failed_count,
        "pass_rate": pass_rate,
        "duration": total_duration,
        "start_time": start_time_iso,
        "end_time": end_time_iso
    }

    timestamp_tag = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    excel_path_primary = os.path.join(root_dir, "E2E_Test_Report_SecureEHR.xlsx")
    excel_path_timestamped = os.path.join(os.path.dirname(os.path.abspath(__file__)), f"E2E_Test_Report_SecureEHR_{timestamp_tag}.xlsx")

    build_excel_report(summary_data, passed_tests, failed_tests, execution_logs, test_details, excel_path_primary)
    build_excel_report(summary_data, passed_tests, failed_tests, execution_logs, test_details, excel_path_timestamped)

    print(f"[+] Primary Report Created: {excel_path_primary}")
    print(f"[+] Timestamped Report Created: {excel_path_timestamped}")
    print(f"[+] E2E Test Suite Execution Complete!")

if __name__ == "__main__":
    main()
