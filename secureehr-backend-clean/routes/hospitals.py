import json
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from typing import List, Optional

from database import get_db
from dependencies import require_auth
from models.hospital import Hospital
from schemas.hospital import HospitalCreate, HospitalResponse

router = APIRouter(prefix="/hospitals", tags=["hospitals"])


@router.get("", response_model=List[HospitalResponse])
def get_hospitals(
    lat: Optional[float] = Query(None),
    lon: Optional[float] = Query(None),
    condition: Optional[str] = Query(None),
    db: Session = Depends(get_db),
):
    hospitals = db.query(Hospital).all()

    if condition:
        cond_lower = condition.lower()

        def lookup_condition_rate(h: Hospital) -> float:
            if h.condition_rates:
                try:
                    rates = (
                        h.condition_rates
                        if isinstance(h.condition_rates, dict)
                        else json.loads(h.condition_rates)
                    )
                    for k, v in rates.items():
                        if k.lower() == cond_lower:
                            return float(v)
                except Exception:
                    pass
            return 0.0

        pairs = [(h, lookup_condition_rate(h)) for h in hospitals]
        pairs.sort(key=lambda p: -p[1])
        result = []
        for h, rate in pairs:
            resp = HospitalResponse.model_validate(h)
            resp.cure_rate = rate
            result.append(resp)
        return result

    return sorted(hospitals, key=lambda h: -(h.cure_rate or 0))


@router.post("", response_model=HospitalResponse, status_code=201)
def create_hospital(
    hospital_data: HospitalCreate,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    data = hospital_data.model_dump()
    # Serialize dict → JSON string for TEXT column
    if data.get("condition_rates") is not None:
        data["condition_rates"] = json.dumps(data["condition_rates"])
    hospital = Hospital(**data)
    db.add(hospital)
    db.commit()
    db.refresh(hospital)
    return hospital


@router.put("/{hospital_id}", response_model=HospitalResponse)
def update_hospital(
    hospital_id: int,
    hospital_data: HospitalCreate,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    hospital = db.query(Hospital).filter(Hospital.id == hospital_id).first()
    if not hospital:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Hospital not found")
    data = hospital_data.model_dump(exclude_unset=True)
    if data.get("condition_rates") is not None:
        data["condition_rates"] = json.dumps(data["condition_rates"])
    for key, val in data.items():
        setattr(hospital, key, val)
    db.commit()
    db.refresh(hospital)
    return hospital


@router.delete("/{hospital_id}", status_code=204)
def delete_hospital(
    hospital_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    from fastapi import HTTPException
    hospital = db.query(Hospital).filter(Hospital.id == hospital_id).first()
    if not hospital:
        raise HTTPException(status_code=404, detail="Hospital not found")
    db.delete(hospital)
    db.commit()
