from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class PatientBase(BaseModel):
    name: str
    date_of_birth: Optional[str] = None
    gender: Optional[str] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    blood_group: Optional[str] = None
    allergies: Optional[str] = None
    emergency_contact: Optional[str] = None
    research_data_sharing: bool = True


class ResearchSharingUpdate(BaseModel):
    research_data_sharing: bool


class PatientUpdate(BaseModel):
    name: Optional[str] = None
    date_of_birth: Optional[str] = None
    gender: Optional[str] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    blood_group: Optional[str] = None
    allergies: Optional[str] = None
    emergency_contact: Optional[str] = None


class PatientCreate(PatientBase):
    user_id: Optional[int] = None


class PatientResponse(PatientBase):
    id: int
    user_id: Optional[int] = None
    created_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
