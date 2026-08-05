import csv
from collections import defaultdict

hospitals = defaultdict(lambda: {"total": 0, "normal": 0, "conditions": defaultdict(lambda: {"total": 0, "normal": 0})})

with open("healthcare_dataset.csv") as f:
    reader = csv.DictReader(f)
    for row in reader:
        hosp = row["Hospital"].strip()
        cond = row["Medical Condition"].strip()
        result = row["Test Results"].strip()
        hospitals[hosp]["total"] += 1
        if result == "Normal":
            hospitals[hosp]["normal"] += 1
        hospitals[hosp]["conditions"][cond]["total"] += 1
        if result == "Normal":
            hospitals[hosp]["conditions"][cond]["normal"] += 1

ranked = sorted(hospitals.items(), key=lambda x: x[1]["total"], reverse=True)[:20]
for name, d in ranked:
    cure = round(d["normal"] / d["total"] * 100, 1)
    top_cond = max(d["conditions"].items(), key=lambda x: x[1]["total"])
    vol = d["total"]
    # print condition-specific cure rates too
    cond_rates = {c: round(v["normal"]/v["total"]*100,1) for c, v in d["conditions"].items()}
    print(f"{name[:45]:45s} | vol={vol:4d} | cure={cure}% | top_cond={top_cond[0]} | cond_rates={cond_rates}")
