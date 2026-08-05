from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from database import Base


class HospitalVisit(Base):
    __tablename__ = "hospital_visits"

    id = Column(Integer, primary_key=True, index=True)
    patient_id = Column(Integer, ForeignKey("patients.id"), nullable=False)
    hospital_id = Column(Integer, ForeignKey("hospitals.id"), nullable=True)
    visit_date = Column(String, nullable=True)
    reason = Column(Text, nullable=True)
    doctor_name = Column(String, nullable=True)
    department = Column(String, nullable=True)
    status = Column(String, default="scheduled")
    notes = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    patient = relationship("Patient", back_populates="hospital_visits")
    hospital = relationship("Hospital")

    @property
    def hospital_name(self):
        return self.hospital.name if self.hospital else None
