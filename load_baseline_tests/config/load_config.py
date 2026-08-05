import os

class LoadConfig:
    """Configuration settings for 100 Virtual User (VU) Baseline & Load Testing."""
    
    CONCURRENT_USERS = 100        # 100 Virtual Users
    DURATION_SECONDS = 60         # 1 Minute Continuous Execution
    TARGET_HOST = os.environ.get("LOAD_TARGET_HOST", "http://127.0.0.1:8000")
    
    # SLA & Performance Benchmarks
    TARGET_RPS = 120              # Target Requests Per Second benchmark
    MAX_AVG_LATENCY_MS = 300.0    # SLA: Avg response time must be <= 300ms
    MAX_ERROR_RATE_PCT = 0.5      # SLA: Maximum allowed failure rate 0.5%
    
    # Simulates realistic network latency jitter if running offline mock benchmark
    MOCK_NETWORK_LATENCY = True
