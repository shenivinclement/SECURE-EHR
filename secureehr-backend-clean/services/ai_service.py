import os
from typing import List, Dict


SYSTEM_PROMPT = (
    "You are MedBot, a knowledgeable medical assistant for SecureEHR. "
    "Provide clear, evidence-based health information. "
    "Always advise users to consult a licensed healthcare professional for personal medical decisions. "
    "Do not diagnose or prescribe — guide and inform only."
)


def chat_with_ai(message: str, conversation_history: List[Dict] | None = None) -> str:
    api_key = os.getenv("GROQ_API_KEY", "")
    if not api_key:
        return (
            "AI assistant is not configured. "
            "Please set the GROQ_API_KEY environment variable to enable this feature."
        )

    try:
        from groq import Groq

        client = Groq(api_key=api_key)
        messages = [{"role": "system", "content": SYSTEM_PROMPT}]
        if conversation_history:
            messages.extend(conversation_history)
        messages.append({"role": "user", "content": message})

        completion = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=messages,
            temperature=0.7,
            max_tokens=1024,
        )
        return completion.choices[0].message.content
    except Exception as e:
        return f"AI service error: {str(e)}"
