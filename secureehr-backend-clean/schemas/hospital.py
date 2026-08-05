import json
from pydantic import BaseModel, field_validator
from typing import Optional
from datetime import datetime


class HospitalBase(BaseModel):
    name: str
    address: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    specialty: Optional[str] = None
    rating: Optional[float] = None
    cure_rate: Optional[float] = None
    beds: Optional[int] = None
    phone: Optional[str] = None
    condition_rates: Optional[dict] = None

    @field_validator("condition_rates", mode="before")
    @classmethod
    def parse_condition_rates(cls, v):
        if isinstance(v, str):
            try:
                return json.loads(v)
            except Exception:
                return None
        return v


class HospitalCreate(HospitalBase):
    pass


class HospitalResponse(HospitalBase):
    id: int
    created_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
