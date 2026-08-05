from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from database import get_db
from dependencies import require_auth
from models.hospital_visit import HospitalVisit
from models.patient import Patient
from schemas.hospital_visit import HospitalVisitCreate, HospitalVisitResponse

router = APIRouter(prefix="/visits", tags=["visits"])


@router.get("", response_model=List[HospitalVisitResponse])
def get_visits(db: Session = Depends(get_db), current_user=Depends(require_auth)):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        return []
    return db.query(HospitalVisit).filter(HospitalVisit.patient_id == patient.id).all()


@router.post("", response_model=HospitalVisitResponse, status_code=status.HTTP_201_CREATED)
def create_visit(
    visit_data: HospitalVisitCreate,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="No patient profile for this user")
    data = visit_data.model_dump()
    data["patient_id"] = patient.id
    visit = HospitalVisit(**data)
    db.add(visit)
    db.commit()
    db.refresh(visit)
    return visit


@router.get("/{visit_id}", response_model=HospitalVisitResponse)
def get_visit(
    visit_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="Visit not found")
    visit = db.query(HospitalVisit).filter(
        HospitalVisit.id == visit_id, HospitalVisit.patient_id == patient.id
    ).first()
    if not visit:
        raise HTTPException(status_code=404, detail="Visit not found")
    return visit


@router.delete("/{visit_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_visit(
    visit_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="Visit not found")
    visit = db.query(HospitalVisit).filter(
        HospitalVisit.id == visit_id, HospitalVisit.patient_id == patient.id
    ).first()
    if not visit:
        raise HTTPException(status_code=404, detail="Visit not found")
    db.delete(visit)
    db.commit()
