from typing import Dict, List, TypedDict


class QAEntry(TypedDict):
    condition: str
    question_type: str
    keywords: List[str]
    answer: str


DISCLAIMER = (
    "This is general information, not a diagnosis — please consult a licensed "
    "doctor for medical advice specific to you."
)

CONDITIONS = ["Diabetes", "Cancer", "Hypertension", "Obesity", "Asthma", "Arthritis"]

CONDITION_KEYWORDS: Dict[str, List[str]] = {
    "Diabetes": ["diabetes", "diabetic", "blood sugar", "glucose"],
    "Cancer": ["cancer", "tumor", "tumour", "oncology", "malignant"],
    "Hypertension": ["hypertension", "high blood pressure", "blood pressure"],
    "Obesity": ["obesity", "obese", "overweight"],
    "Asthma": ["asthma", "asthmatic"],
    "Arthritis": ["arthritis", "arthritic"],
}

QA_DATA: List[QAEntry] = [
    {
        "condition": "Diabetes",
        "question_type": "what_is",
        "keywords": CONDITION_KEYWORDS["Diabetes"],
        "answer": (
            "A chronic condition where the body can't properly regulate blood "
            "sugar (glucose) levels, either because it doesn't produce enough "
            "insulin (Type 1) or can't use insulin effectively (Type 2). "
            + DISCLAIMER
        ),
    },
    {
        "condition": "Diabetes",
        "question_type": "symptoms",
        "keywords": CONDITION_KEYWORDS["Diabetes"],
        "answer": (
            "Increased thirst and urination, unexplained weight loss, fatigue, "
            "blurred vision, and slow-healing wounds. " + DISCLAIMER
        ),
    },
    {
        "condition": "Diabetes",
        "question_type": "treatment",
        "keywords": CONDITION_KEYWORDS["Diabetes"],
        "answer": (
            "Managed through blood sugar monitoring, insulin or oral "
            "medications, diet and exercise changes, and regular checkups to "
            "prevent complications. " + DISCLAIMER
        ),
    },
    {
        "condition": "Cancer",
        "question_type": "what_is",
        "keywords": CONDITION_KEYWORDS["Cancer"],
        "answer": (
            "A group of diseases involving abnormal cell growth that can "
            "invade nearby tissue and spread to other parts of the body. "
            + DISCLAIMER
        ),
    },
    {
        "condition": "Cancer",
        "question_type": "symptoms",
        "keywords": CONDITION_KEYWORDS["Cancer"],
        "answer": (
            "Vary widely by type, but can include unexplained weight loss, "
            "persistent fatigue, unusual lumps, changes in skin, or persistent "
            "pain — early stages often have no symptoms. " + DISCLAIMER
        ),
    },
    {
        "condition": "Cancer",
        "question_type": "treatment",
        "keywords": CONDITION_KEYWORDS["Cancer"],
        "answer": (
            "Depends on type and stage; common approaches include surgery, "
            "chemotherapy, radiation therapy, immunotherapy, or a combination, "
            "guided by an oncologist. " + DISCLAIMER
        ),
    },
    {
        "condition": "Hypertension",
        "question_type": "what_is",
        "keywords": CONDITION_KEYWORDS["Hypertension"],
        "answer": (
            "Also called high blood pressure, a condition where the force of "
            "blood against artery walls is consistently too high, increasing "
            "strain on the heart. " + DISCLAIMER
        ),
    },
    {
        "condition": "Hypertension",
        "question_type": "symptoms",
        "keywords": CONDITION_KEYWORDS["Hypertension"],
        "answer": (
            "Often called a 'silent' condition with no clear symptoms; severe "
            "cases may cause headaches, shortness of breath, or nosebleeds. "
            + DISCLAIMER
        ),
    },
    {
        "condition": "Hypertension",
        "question_type": "treatment",
        "keywords": CONDITION_KEYWORDS["Hypertension"],
        "answer": (
            "Lifestyle changes (reduced salt, exercise, weight management) "
            "and, when needed, medications like ACE inhibitors, diuretics, or "
            "beta-blockers. " + DISCLAIMER
        ),
    },
    {
        "condition": "Obesity",
        "question_type": "what_is",
        "keywords": CONDITION_KEYWORDS["Obesity"],
        "answer": (
            "A condition involving excess body fat that increases risk for "
            "other health issues, typically assessed using BMI. " + DISCLAIMER
        ),
    },
    {
        "condition": "Obesity",
        "question_type": "symptoms",
        "keywords": CONDITION_KEYWORDS["Obesity"],
        "answer": (
            "Not a 'symptom-based' condition itself, but is associated with "
            "fatigue, joint pain, shortness of breath, and increased risk of "
            "related conditions like diabetes and hypertension. " + DISCLAIMER
        ),
    },
    {
        "condition": "Obesity",
        "question_type": "treatment",
        "keywords": CONDITION_KEYWORDS["Obesity"],
        "answer": (
            "Managed through dietary changes, increased physical activity, "
            "behavioral support, and in some cases medication or surgery, "
            "under medical guidance. " + DISCLAIMER
        ),
    },
    {
        "condition": "Asthma",
        "question_type": "what_is",
        "keywords": CONDITION_KEYWORDS["Asthma"],
        "answer": (
            "A chronic condition where the airways become inflamed and "
            "narrowed, making breathing difficult. " + DISCLAIMER
        ),
    },
    {
        "condition": "Asthma",
        "question_type": "symptoms",
        "keywords": CONDITION_KEYWORDS["Asthma"],
        "answer": (
            "Wheezing, shortness of breath, chest tightness, and coughing, "
            "often triggered by allergens, exercise, or cold air. " + DISCLAIMER
        ),
    },
    {
        "condition": "Asthma",
        "question_type": "treatment",
        "keywords": CONDITION_KEYWORDS["Asthma"],
        "answer": (
            "Managed with inhalers (quick-relief and long-term control), "
            "avoiding known triggers, and an asthma action plan from a "
            "doctor. " + DISCLAIMER
        ),
    },
    {
        "condition": "Arthritis",
        "question_type": "what_is",
        "keywords": CONDITION_KEYWORDS["Arthritis"],
        "answer": (
            "Inflammation of one or more joints, causing pain and stiffness "
            "that can worsen with age. " + DISCLAIMER
        ),
    },
    {
        "condition": "Arthritis",
        "question_type": "symptoms",
        "keywords": CONDITION_KEYWORDS["Arthritis"],
        "answer": (
            "Joint pain, swelling, stiffness (especially in the morning), and "
            "reduced range of motion. " + DISCLAIMER
        ),
    },
    {
        "condition": "Arthritis",
        "question_type": "treatment",
        "keywords": CONDITION_KEYWORDS["Arthritis"],
        "answer": (
            "Managed with pain relief medication, physical therapy, exercise, "
            "and in severe cases, joint injections or surgery. " + DISCLAIMER
        ),
    },
]
