from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text, Boolean
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from database import Base


class Patient(Base):
    __tablename__ = "patients"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    name = Column(String, nullable=False)
    date_of_birth = Column(String, nullable=True)
    gender = Column(String, nullable=True)
    phone = Column(String, nullable=True)
    address = Column(Text, nullable=True)
    blood_group = Column(String, nullable=True)
    allergies = Column(Text, nullable=True)
    emergency_contact = Column(String, nullable=True)
    research_data_sharing = Column(Boolean, nullable=False, default=True, server_default="1")
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    medical_records = relationship("MedicalRecord", back_populates="patient")
    hospital_visits = relationship("HospitalVisit", back_populates="patient")
    consents = relationship("Consent", back_populates="patient")
