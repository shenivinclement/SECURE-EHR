from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from database import Base


class MedicalRecord(Base):
    __tablename__ = "medical_records"

    id = Column(Integer, primary_key=True, index=True)
    patient_id = Column(Integer, ForeignKey("patients.id"), nullable=False)
    doctor_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    hospital_id = Column(Integer, ForeignKey("hospitals.id"), nullable=True)
    diagnosis = Column(String, nullable=False)
    symptoms = Column(Text, nullable=True)
    treatment = Column(Text, nullable=True)
    medications = Column(Text, nullable=True)
    notes = Column(Text, nullable=True)
    record_date = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    patient = relationship("Patient", back_populates="medical_records")
    hospital = relationship("Hospital")
