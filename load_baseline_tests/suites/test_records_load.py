import unittest
import logging

class TestRecordsLoadSuite(unittest.TestCase):
    """Medical Records Management & File Decryption Load Test Suite (75 Test Cases)."""

    @classmethod
    def setUpClass(cls):
        cls.logger = logging.getLogger("TestRecordsLoadSuite")

    @staticmethod
    def get_test_cases():
        cases = []
        
        # 1. EHR Record Query & History Retrieval Load (25 cases)
        for i in range(1, 26):
            cases.append({
                "test_id": f"LOAD-REC-{i:03d}",
                "title": f"Concurrent Medical Record Search & Pagination Load #{i}",
                "endpoint": "/api/v1/records/search",
                "method": "GET",
                "category": "EHR Search & Query",
                "description": f"100 VUs querying encrypted EHR records filtered by category, date range, and doctor ID #{i}.",
                "base_delay_ms": 160 + (i * 2),
                "severity": "High"
            })

        # 2. Document Upload & AES Encryption Key Exchange Load (25 cases)
        for i in range(26, 51):
            cases.append({
                "test_id": f"LOAD-REC-{i:03d}",
                "title": f"Concurrent Multipart EHR Upload & AES Key Store Load #{i-25}",
                "endpoint": "/api/v1/records/upload",
                "method": "POST",
                "category": "Document Upload & Key Exchange",
                "description": f"100 VUs uploading 2MB encrypted PDF/DICOM health records and registering key hashes #{i-25}.",
                "base_delay_ms": 280 + (i * 1.8),
                "severity": "Critical"
            })

        # 3. Record Decryption & Streaming PDF Download Load (25 cases)
        for i in range(51, 76):
            cases.append({
                "test_id": f"LOAD-REC-{i:03d}",
                "title": f"Concurrent Document Decryption & Secure Stream Load #{i-50}",
                "endpoint": "/api/v1/records/download",
                "method": "GET",
                "category": "Decryption & Streaming",
                "description": f"100 VUs fetching decrypted record payload and stream rendering medical summaries #{i-50}.",
                "base_delay_ms": 190 + (i * 1.2),
                "severity": "High"
            })

        return cases
