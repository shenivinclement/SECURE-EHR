from typing import Dict, List

from services.medical_qa_data import CONDITION_KEYWORDS, CONDITIONS, QA_DATA


SYMPTOM_KEYWORDS = ["symptom", "symptoms", "sign", "signs", "warning sign"]
TREATMENT_KEYWORDS = [
    "treat",
    "treatment",
    "treatments",
    "cure",
    "manage",
    "management",
    "medication",
    "medicine",
    "therapy",
    "remedy",
]

FALLBACK_MESSAGE = (
    "I can answer questions about " + ", ".join(CONDITIONS[:-1]) + ", and "
    + CONDITIONS[-1] + " — their symptoms, and treatment options. "
    "Try asking something like 'What are the symptoms of asthma?'"
)


def _detect_condition(text: str) -> str | None:
    for condition, keywords in CONDITION_KEYWORDS.items():
        if any(keyword in text for keyword in keywords):
            return condition
    return None


def _detect_question_type(text: str) -> str:
    if any(keyword in text for keyword in TREATMENT_KEYWORDS):
        return "treatment"
    if any(keyword in text for keyword in SYMPTOM_KEYWORDS):
        return "symptoms"
    return "what_is"


def _lookup_answer(condition: str, question_type: str) -> str | None:
    for entry in QA_DATA:
        if entry["condition"] == condition and entry["question_type"] == question_type:
            return entry["answer"]
    return None


def chat_with_ai(message: str, conversation_history: List[Dict] | None = None) -> str:
    text = (message or "").lower()

    condition = _detect_condition(text)
    if not condition:
        return FALLBACK_MESSAGE

    question_type = _detect_question_type(text)
    return _lookup_answer(condition, question_type) or FALLBACK_MESSAGE
