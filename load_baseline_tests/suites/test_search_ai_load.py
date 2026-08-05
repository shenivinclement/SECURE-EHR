import unittest
import logging

class TestSearchAILoadSuite(unittest.TestCase):
    """Doctor Search, Hospital Finder & AI Health Assistant Load Suite (75 Test Cases)."""

    @classmethod
    def setUpClass(cls):
        cls.logger = logging.getLogger("TestSearchAILoadSuite")

    @staticmethod
    def get_test_cases():
        cases = []
        
        # 1. Doctor Directory & Specialty Search Load (25 cases)
        for i in range(1, 26):
            cases.append({
                "test_id": f"LOAD-AI-{i:03d}",
                "title": f"Concurrent Doctor Specialty & Location Search Load #{i}",
                "endpoint": "/api/v1/doctor/search",
                "method": "GET",
                "category": "Doctor Search",
                "description": f"100 VUs searching doctor directory by specialty (Cardiology, Neurology), rating, and availability #{i}.",
                "base_delay_ms": 130 + (i * 1.5),
                "severity": "Medium"
            })

        # 2. Emergency Hospital Map & GPS Distance Query Load (25 cases)
        for i in range(26, 51):
            cases.append({
                "test_id": f"LOAD-AI-{i:03d}",
                "title": f"Concurrent Hospital ER Spatial Query & Route Distance Load #{i-25}",
                "endpoint": "/api/v1/hospitals/nearby",
                "method": "GET",
                "category": "Hospital Finder Maps",
                "description": f"100 VUs submitting GPS geolocation coordinates (Lat/Lng) to compute nearest 24/7 ER trauma centers #{i-25}.",
                "base_delay_ms": 170 + (i * 2.0),
                "severity": "High"
            })

        # 3. AI Health Assistant Medical Inquiry Load (25 cases)
        for i in range(51, 76):
            cases.append({
                "test_id": f"LOAD-AI-{i:03d}",
                "title": f"Concurrent AI Health Chatbot Inference Prompt Load #{i-50}",
                "endpoint": "/api/v1/ai/chat",
                "method": "POST",
                "category": "AI Health Assistant",
                "description": f"100 VUs submitting medical symptom queries to AI assistant and parsing structured disclaimers #{i-50}.",
                "base_delay_ms": 290 + (i * 2.5),
                "severity": "Critical"
            })

        return cases
