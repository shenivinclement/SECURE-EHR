from sqlalchemy import Column, Integer, String, Float, DateTime, Text
from sqlalchemy.sql import func
from database import Base


class Hospital(Base):
    __tablename__ = "hospitals"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    address = Column(String, nullable=True)
    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)
    specialty = Column(String, nullable=True)
    rating = Column(Float, nullable=True)
    cure_rate = Column(Float, nullable=True)
    beds = Column(Integer, nullable=True)
    phone = Column(String, nullable=True)
    condition_rates = Column(Text, nullable=True)  # JSON-encoded {condition: cure_rate%}
    created_at = Column(DateTime(timezone=True), server_default=func.now())
