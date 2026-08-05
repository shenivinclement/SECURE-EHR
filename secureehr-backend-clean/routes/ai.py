from fastapi import APIRouter, Depends
from pydantic import BaseModel
from typing import List, Optional

from dependencies import require_auth
from services.ai_service import chat_with_ai

router = APIRouter(prefix="/ai", tags=["ai"])


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    message: str
    history: Optional[List[ChatMessage]] = []


@router.post("/chat")
def chat(request: ChatRequest, current_user=Depends(require_auth)):
    history = [{"role": m.role, "content": m.content} for m in (request.history or [])]
    response = chat_with_ai(request.message, history)
    return {
        "role": "assistant",
        "response": response,
        "user": current_user.email,
    }
