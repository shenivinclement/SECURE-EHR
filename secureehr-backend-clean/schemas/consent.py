from pydantic import BaseModel
from typing import Optional


class ConsentGrant(BaseModel):
    doctor_name: str
    hospital_name: str
    specialization: str
    expiry_date: Optional[str] = None
    purpose: Optional[str] = None


class ConsentRevoke(BaseModel):
    consent_id: int


class ConsentResponse(BaseModel):
    id: int
    patient_id: int
    doctor_name: str
    hospital_name: Optional[str] = None
    status: str
    expiry_date: Optional[str] = None
    specialization: str

    model_config = {"from_attributes": True}
