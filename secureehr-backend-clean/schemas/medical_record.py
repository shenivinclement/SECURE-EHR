from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class MedicalRecordBase(BaseModel):
    patient_id: int
    diagnosis: str
    symptoms: Optional[str] = None
    treatment: Optional[str] = None
    medications: Optional[str] = None
    notes: Optional[str] = None
    record_date: Optional[str] = None


class MedicalRecordCreate(MedicalRecordBase):
    doctor_id: Optional[int] = None
    hospital_id: Optional[int] = None


class MedicalRecordResponse(MedicalRecordBase):
    id: int
    doctor_id: Optional[int] = None
    hospital_id: Optional[int] = None
    created_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
