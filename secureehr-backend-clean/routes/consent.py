from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func
from sqlalchemy.orm import Session
from typing import List
from datetime import datetime, timezone

from database import get_db
from dependencies import require_auth
from models.consent import Consent
from models.patient import Patient
from schemas.consent import ConsentGrant, ConsentRevoke, ConsentResponse

router = APIRouter(prefix="/consent", tags=["consent"])


@router.get("", response_model=List[ConsentResponse])
def get_consents(db: Session = Depends(get_db), current_user=Depends(require_auth)):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        return []
    return db.query(Consent).filter(Consent.patient_id == patient.id).all()


@router.post("/grant", response_model=ConsentResponse)
def grant_consent(
    consent_data: ConsentGrant,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="No patient profile for this user")

    existing = (
        db.query(Consent)
        .filter(
            Consent.patient_id == patient.id,
            func.lower(Consent.doctor_name) == consent_data.doctor_name.strip().lower(),
        )
        .first()
    )

    if existing:
        existing.status = "active"
        existing.is_active = True
        existing.revoked_at = None
        existing.hospital_name = consent_data.hospital_name
        existing.specialization = consent_data.specialization
        existing.expiry_date = consent_data.expiry_date
        existing.purpose = consent_data.purpose
        db.commit()
        db.refresh(existing)
        return existing

    consent = Consent(
        patient_id=patient.id,
        doctor_name=consent_data.doctor_name,
        specialization=consent_data.specialization,
        hospital_name=consent_data.hospital_name,
        expiry_date=consent_data.expiry_date,
        purpose=consent_data.purpose,
        status="active",
        is_active=True,
    )
    db.add(consent)
    db.commit()
    db.refresh(consent)
    return consent


@router.post("/revoke")
def revoke_consent(
    revoke_data: ConsentRevoke,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    consent = db.query(Consent).filter(Consent.id == revoke_data.consent_id).first()
    if not consent:
        raise HTTPException(status_code=404, detail="Consent record not found")
    if not consent.is_active:
        return {"message": "Consent was already revoked", "consent_id": revoke_data.consent_id}
    consent.is_active = False
    consent.status = "revoked"
    consent.revoked_at = datetime.now(timezone.utc)
    db.commit()
    return {"message": "Consent revoked successfully", "consent_id": revoke_data.consent_id}


@router.delete("/{consent_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_consent(
    consent_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(require_auth),
):
    patient = db.query(Patient).filter(Patient.user_id == current_user.id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="No patient profile for this user")
    consent = db.query(Consent).filter(
        Consent.id == consent_id, Consent.patient_id == patient.id
    ).first()
    if not consent:
        raise HTTPException(status_code=404, detail="Consent record not found")
    db.delete(consent)
    db.commit()
