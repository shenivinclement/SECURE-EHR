from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session

from database import get_db
from models.consent import Consent
from models.hospital import Hospital
from models.hospital_visit import HospitalVisit
from models.medical_record import MedicalRecord
from models.patient import Patient
from models.user import User

router = APIRouter(prefix="/admin", tags=["admin"])

_ADMIN_KEY = "reset-secureehr-demo"


@router.post("/reset", status_code=200)
def reset_demo_data(
    x_admin_key: str = Header(...),
    db: Session = Depends(get_db),
):
    if x_admin_key != _ADMIN_KEY:
        raise HTTPException(status_code=403, detail="Forbidden")

    deleted = {}
    deleted["consents"] = db.query(Consent).delete()
    deleted["visits"] = db.query(HospitalVisit).delete()
    deleted["records"] = db.query(MedicalRecord).delete()
    deleted["patients"] = db.query(Patient).delete()
    deleted["users"] = db.query(User).delete()
    deleted["hospitals"] = db.query(Hospital).delete()
    db.commit()

    return {"message": "Demo data reset complete", "deleted": deleted}
