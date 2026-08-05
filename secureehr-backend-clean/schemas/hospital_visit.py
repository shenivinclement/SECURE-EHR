from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class HospitalVisitBase(BaseModel):
    patient_id: int
    visit_date: Optional[str] = None
    reason: Optional[str] = None
    doctor_name: Optional[str] = None
    department: Optional[str] = None
    status: str = "scheduled"
    notes: Optional[str] = None


class HospitalVisitCreate(HospitalVisitBase):
    hospital_id: int


class HospitalVisitResponse(HospitalVisitBase):
    id: int
    hospital_id: Optional[int] = None
    hospital_name: Optional[str] = None
    created_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
