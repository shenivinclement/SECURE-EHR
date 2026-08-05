from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class UserBase(BaseModel):
    name: str
    email: str
    role: str = "patient"
    two_factor_enabled: bool = False


class TwoFactorUpdate(BaseModel):
    two_factor_enabled: bool


class UserCreate(UserBase):
    password: str


class UserLogin(BaseModel):
    email: str
    password: str


class UserResponse(UserBase):
    id: int
    is_active: bool
    created_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
