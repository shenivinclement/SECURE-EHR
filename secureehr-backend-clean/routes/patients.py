from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from database import get_db
from dependencies import require_auth
from models.patient import Patient
from schemas.patient import PatientCreate, PatientResponse, ResearchSharingUpdate, PatientUpdate

router = APIRouter(prefix="/patients", tags=["patients"])


@router.get("", response_model=List[PatientResponse])
def get_patients(db: Session = Depends(get_db), current_user=Depends(require_auth)):
    return db.query(Patient).all()


@router.post("", response_model=PatientResponse, status_code=status.HTTP_201_CREATED)
def create_patient(
    patient_data: PatientCreate,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    patient = Patient(**patient_data.model_dump())
    db.add(patient)
    db.commit()
    db.refresh(patient)
    return patient


@router.get("/me", response_model=PatientResponse)
def get_my_patient(db: Session = Depends(get_db), current_user=Depends(require_auth)):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="No patient profile for this user")
    return patient


@router.get("/{patient_id}", response_model=PatientResponse)
def get_patient(
    patient_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    patient = db.query(Patient).filter(Patient.id == patient_id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="Patient not found")
    return patient


@router.patch("/me/research-sharing", response_model=PatientResponse)
def update_research_sharing(
    payload: ResearchSharingUpdate,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="No patient profile for this user")
    patient.research_data_sharing = payload.research_data_sharing
    db.commit()
    db.refresh(patient)
    return patient


@router.put("/me", response_model=PatientResponse)
def update_my_patient(
    patient_data: PatientUpdate,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="No patient profile for this user")
    data = patient_data.model_dump(exclude_unset=True)
    for key, val in data.items():
        setattr(patient, key, val)
    db.commit()
    db.refresh(patient)
    return patient
