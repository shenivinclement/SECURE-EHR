from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import inspect, text

from database import engine, Base

# Import all models so SQLAlchemy registers them before create_all
import models  # noqa: F401

from routes import auth, patients, records, ai, visits, consent, hospitals, admin, doctor

Base.metadata.create_all(bind=engine)

# Add condition_rates column to existing hospitals tables that predate the field
def _migrate():
    try:
        cols = {c["name"] for c in inspect(engine).get_columns("hospitals")}
        if "condition_rates" not in cols:
            with engine.connect() as conn:
                if engine.dialect.name == "postgresql":
                    conn.execute(text(
                        "ALTER TABLE hospitals ADD COLUMN IF NOT EXISTS condition_rates TEXT"
                    ))
                else:
                    conn.execute(text(
                        "ALTER TABLE hospitals ADD COLUMN condition_rates TEXT"
                    ))
                conn.commit()
    except Exception as e:
        print(f"[migration] condition_rates: {e}")

_migrate()


def _migrate_consent():
    try:
        cols = {c["name"] for c in inspect(engine).get_columns("consents")}
        with engine.connect() as conn:
            for col, definition in [
                ("hospital_name", "VARCHAR"),
                ("expiry_date", "VARCHAR"),
                ("status", "VARCHAR NOT NULL DEFAULT 'active'"),
            ]:
                if col not in cols:
                    if engine.dialect.name == "postgresql":
                        conn.execute(text(
                            f"ALTER TABLE consents ADD COLUMN IF NOT EXISTS {col} {definition}"
                        ))
                    else:
                        conn.execute(text(
                            f"ALTER TABLE consents ADD COLUMN {col} {definition}"
                        ))
            conn.commit()
    except Exception as e:
        print(f"[migration] consent columns: {e}")

_migrate_consent()


# Replace hospital_visits.hospital_name (free text) with hospital_id (FK to hospitals)
def _migrate_hospital_visits():
    cols = {c["name"] for c in inspect(engine).get_columns("hospital_visits")}

    if "hospital_id" not in cols:
        try:
            with engine.connect() as conn:
                if engine.dialect.name == "postgresql":
                    conn.execute(text(
                        "ALTER TABLE hospital_visits ADD COLUMN IF NOT EXISTS hospital_id INTEGER REFERENCES hospitals(id)"
                    ))
                else:
                    conn.execute(text(
                        "ALTER TABLE hospital_visits ADD COLUMN hospital_id INTEGER REFERENCES hospitals(id)"
                    ))
                conn.commit()
        except Exception as e:
            print(f"[migration] hospital_visits.hospital_id: {e}")

    if "hospital_name" in cols:
        try:
            with engine.connect() as conn:
                if engine.dialect.name == "postgresql":
                    conn.execute(text(
                        "ALTER TABLE hospital_visits DROP COLUMN IF EXISTS hospital_name"
                    ))
                else:
                    conn.execute(text(
                        "ALTER TABLE hospital_visits DROP COLUMN hospital_name"
                    ))
                conn.commit()
        except Exception as e:
            print(f"[migration] hospital_visits.hospital_name drop: {e}")

_migrate_hospital_visits()


def _migrate_medical_records():
    try:
        cols = {c["name"] for c in inspect(engine).get_columns("medical_records")}
        if "hospital_id" not in cols:
            with engine.connect() as conn:
                if engine.dialect.name == "postgresql":
                    conn.execute(text(
                        "ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS hospital_id INTEGER REFERENCES hospitals(id)"
                    ))
                else:
                    conn.execute(text(
                        "ALTER TABLE medical_records ADD COLUMN hospital_id INTEGER REFERENCES hospitals(id)"
                    ))
                conn.commit()
    except Exception as e:
        print(f"[migration] medical_records.hospital_id: {e}")

_migrate_medical_records()

app = FastAPI(
    title="SecureEHR API",
    description="Secure Electronic Health Records System",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(patients.router)
app.include_router(records.router)
app.include_router(ai.router)
app.include_router(visits.router)
app.include_router(consent.router)
app.include_router(hospitals.router)
app.include_router(admin.router)
app.include_router(doctor.router)


@app.get("/", tags=["root"])
def root():
    return {
        "app": "SecureEHR API",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs",
    }


@app.get("/health", tags=["root"])
def health():
    return {"status": "healthy"}
