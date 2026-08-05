from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from database import get_db
from dependencies import require_auth
from models.medical_record import MedicalRecord
from models.patient import Patient
from schemas.medical_record import MedicalRecordCreate, MedicalRecordResponse

router = APIRouter(prefix="/records", tags=["records"])


@router.get("", response_model=List[MedicalRecordResponse])
def get_records(db: Session = Depends(get_db), current_user=Depends(require_auth)):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        return []
    return db.query(MedicalRecord).filter(MedicalRecord.patient_id == patient.id).all()


@router.post("", response_model=MedicalRecordResponse, status_code=status.HTTP_201_CREATED)
def create_record(
    record_data: MedicalRecordCreate,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    record = MedicalRecord(**record_data.model_dump())
    db.add(record)
    db.commit()
    db.refresh(record)
    return record


@router.get("/{record_id}", response_model=MedicalRecordResponse)
def get_record(
    record_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    record = db.query(MedicalRecord).filter(MedicalRecord.id == record_id).first()
    if not record:
        raise HTTPException(status_code=404, detail="Medical record not found")
    return record
