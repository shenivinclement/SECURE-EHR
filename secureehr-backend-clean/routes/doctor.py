from collections import Counter

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from database import get_db
from dependencies import require_auth
from models.consent import Consent
from models.hospital_visit import HospitalVisit
from models.medical_record import MedicalRecord
from models.patient import Patient

router = APIRouter(prefix="/doctor", tags=["doctor"])


def _require_doctor(current_user=Depends(require_auth)):
    if current_user.role != "doctor":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Doctor access required")
    return current_user


def _name_variants_lower(name: str) -> list:
    """Return lowercase variants of both 'Dr. X' and 'X' forms for case-insensitive matching."""
    name = name.strip()
    bare = name[4:] if name.startswith("Dr. ") else name
    return [bare.lower(), f"dr. {bare.lower()}"]


def _active_consent(db: Session, patient_id: int, doctor_name: str):
    return (
        db.query(Consent)
        .filter(
            Consent.patient_id == patient_id,
            func.lower(Consent.doctor_name).in_(_name_variants_lower(doctor_name)),
            Consent.status == "active",
        )
        .first()
    )


def _doctor_consents(db: Session, doctor_name: str, active_only: bool = False):
    q = db.query(Consent).filter(
        func.lower(Consent.doctor_name).in_(_name_variants_lower(doctor_name))
    )
    if active_only:
        q = q.filter(Consent.status == "active")
    return q.all()


@router.get("/profile")
def get_doctor_profile(current_user=Depends(_require_doctor)):
    return {
        "id": current_user.id,
        "name": current_user.name,
        "email": current_user.email,
        "role": current_user.role,
    }


@router.get("/dashboard")
def get_dashboard(db: Session = Depends(get_db), current_user=Depends(_require_doctor)):
    all_consents = _doctor_consents(db, current_user.name)
    active_patient_ids = list({c.patient_id for c in all_consents if c.status == "active"})

    total_records = (
        db.query(MedicalRecord)
        .filter(MedicalRecord.patient_id.in_(active_patient_ids))
        .count()
        if active_patient_ids
        else 0
    )

    recent_consents = (
        db.query(Consent)
        .filter(func.lower(Consent.doctor_name).in_(_name_variants_lower(current_user.name)))
        .order_by(Consent.granted_at.desc())
        .limit(5)
        .all()
    )

    recent_activity = []
    for c in recent_consents:
        patient = db.query(Patient).filter(Patient.id == c.patient_id).first()
        recent_activity.append({
            "consent_id": c.id,
            "patient_name": patient.name if patient else "Unknown",
            "status": c.status,
            "granted_at": c.granted_at.isoformat() if c.granted_at else None,
            "revoked_at": c.revoked_at.isoformat() if c.revoked_at else None,
        })

    return {
        "patients_with_active_consent": len(active_patient_ids),
        "total_consents": len(all_consents),
        "total_records_accessible": total_records,
        "recent_consent_activity": recent_activity,
    }


@router.get("/patients")
def get_doctor_patients(db: Session = Depends(get_db), current_user=Depends(_require_doctor)):
    active_consents = _doctor_consents(db, current_user.name, active_only=True)

    result = []
    for consent in active_consents:
        patient = db.query(Patient).filter(Patient.id == consent.patient_id).first()
        if not patient:
            continue

        records = db.query(MedicalRecord).filter(MedicalRecord.patient_id == patient.id).all()
        counts = Counter(r.diagnosis for r in records if r.diagnosis)
        primary_condition = counts.most_common(1)[0][0] if counts else None

        result.append({
            "patient_id": patient.id,
            "name": patient.name,
            "gender": patient.gender,
            "blood_group": patient.blood_group,
            "date_of_birth": patient.date_of_birth,
            "primary_condition": primary_condition,
            "total_records": len(records),
            "consent_expiry_date": consent.expiry_date,
            "hospital_name": consent.hospital_name,
        })

    return result


@router.get("/patients/{patient_id}/records")
def get_patient_records(
    patient_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(_require_doctor),
):
    if not _active_consent(db, patient_id, current_user.name):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="No active consent from this patient",
        )
    return db.query(MedicalRecord).filter(MedicalRecord.patient_id == patient_id).all()


@router.get("/patients/{patient_id}/visits")
def get_patient_visits(
    patient_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(_require_doctor),
):
    if not _active_consent(db, patient_id, current_user.name):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="No active consent from this patient",
        )
    return db.query(HospitalVisit).filter(HospitalVisit.patient_id == patient_id).all()


@router.get("/search")
def search_patients(
    q: str = Query(..., min_length=1),
    db: Session = Depends(get_db),
    current_user=Depends(_require_doctor),
):
    patients = db.query(Patient).filter(Patient.name.ilike(f"%{q}%")).all()

    active_ids = {
        c.patient_id
        for c in _doctor_consents(db, current_user.name, active_only=True)
    }

    return [
        {
            "patient_id": p.id,
            "name": p.name,
            "gender": p.gender,
            "has_active_consent": p.id in active_ids,
        }
        for p in patients
    ]


@router.get("/consents")
def get_doctor_consents(db: Session = Depends(get_db), current_user=Depends(_require_doctor)):
    consents = _doctor_consents(db, current_user.name)

    result = []
    for c in consents:
        patient = db.query(Patient).filter(Patient.id == c.patient_id).first()
        result.append({
            "consent_id": c.id,
            "patient_id": c.patient_id,
            "patient_name": patient.name if patient else "Unknown",
            "status": c.status,
            "hospital_name": c.hospital_name,
            "specialization": c.specialization,
            "expiry_date": c.expiry_date,
            "purpose": c.purpose,
            "granted_at": c.granted_at.isoformat() if c.granted_at else None,
            "revoked_at": c.revoked_at.isoformat() if c.revoked_at else None,
        })

    return result
