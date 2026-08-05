from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Boolean, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from database import Base


class Consent(Base):
    __tablename__ = "consents"

    id = Column(Integer, primary_key=True, index=True)
    patient_id = Column(Integer, ForeignKey("patients.id"), nullable=False)
    doctor_name = Column("granted_to", String, nullable=False)
    specialization = Column("resource_type", String, nullable=False)
    hospital_name = Column(String, nullable=True)
    expiry_date = Column(String, nullable=True)
    status = Column(String, nullable=False, default="active")
    is_active = Column(Boolean, default=True)
    purpose = Column(Text, nullable=True)
    granted_at = Column(DateTime(timezone=True), server_default=func.now())
    revoked_at = Column(DateTime(timezone=True), nullable=True)

    patient = relationship("Patient", back_populates="consents")
