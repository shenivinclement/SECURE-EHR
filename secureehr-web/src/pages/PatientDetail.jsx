import { useEffect, useState } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { getPatientRecords } from '../api/api';
import Header from '../components/Header';
import Spinner from '../components/Spinner';

function RecordCard({ record }) {
  const [open, setOpen] = useState(false);

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
  };

  return (
    <div style={{
      background: '#1A1F2E',
      border: `1px solid ${open ? '#14B8A6' : '#1E2435'}`,
      borderRadius: '14px',
      overflow: 'hidden',
      marginBottom: '12px',
      transition: 'border-color 0.2s',
    }}>
      <div
        onClick={() => setOpen(o => !o)}
        style={{
          padding: '20px 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
          userSelect: 'none',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{
            width: 44, height: 44,
            background: 'linear-gradient(135deg, rgba(20,184,166,0.2), rgba(20,184,166,0.1))',
            border: '1px solid rgba(20,184,166,0.3)',
            borderRadius: '12px',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '18px',
          }}>🩺</div>
          <div>
            <div style={{ color: '#fff', fontSize: '16px', fontWeight: 700 }}>{record.diagnosis}</div>
            <div style={{ color: '#94A3B8', fontSize: '13px', marginTop: '2px' }}>{formatDate(record.record_date)}</div>
          </div>
        </div>
        <div style={{
          color: '#14B8A6', fontSize: '20px',
          transform: open ? 'rotate(180deg)' : 'rotate(0)',
          transition: 'transform 0.3s ease',
        }}>⌄</div>
      </div>

      <div style={{
        maxHeight: open ? '600px' : '0',
        overflow: 'hidden',
        transition: 'max-height 0.35s ease',
      }}>
        <div style={{ padding: '0 24px 24px', borderTop: '1px solid #1E2435' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginTop: '20px' }}>
            {[
              { label: 'Diagnosis / Symptoms', value: record.symptoms, icon: '🔍' },
              { label: 'Treatment', value: record.treatment, icon: '💊' },
              { label: 'Prescription / Medications', value: record.medications, icon: '📋' },
              { label: 'Notes', value: record.notes, icon: '📝' },
            ].map(({ label, value, icon }) => (
              <div key={label} style={{
                background: '#0A0E1A',
                borderRadius: '10px',
                padding: '16px',
                border: '1px solid #1E2435',
              }}>
                <div style={{ color: '#94A3B8', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '8px' }}>
                  {icon} {label}
                </div>
                <div style={{ color: '#E2E8F0', fontSize: '14px', lineHeight: 1.6 }}>{value || '—'}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function PatientDetail() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [denied, setDenied] = useState(false);

  const patientName = location.state?.name || `Patient #${id}`;

  useEffect(() => {
    getPatientRecords(id)
      .then(r => setRecords(r.data))
      .catch(err => {
        if (err?.response?.status === 403) setDenied(true);
        else navigate('/doctor');
      })
      .finally(() => setLoading(false));
  }, [id]);

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header backTo="/doctor" backLabel="My Patients" />
      <main style={{ maxWidth: '900px', margin: '0 auto', padding: '40px 32px' }}>
        {/* Patient name */}
        <div style={{ marginBottom: '24px' }}>
          <h1 style={{ color: '#fff', fontSize: '28px', fontWeight: 800, margin: '0 0 4px' }}>{patientName}</h1>
          <p style={{ color: '#94A3B8', fontSize: '14px', margin: 0 }}>Medical Records</p>
        </div>

        {/* Consent banner */}
        {!denied && (
          <div style={{
            background: 'rgba(20,184,166,0.08)',
            border: '1px solid rgba(20,184,166,0.25)',
            borderRadius: '12px',
            padding: '14px 20px',
            marginBottom: '28px',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
          }}>
            <span style={{ fontSize: '18px' }}>⛓️</span>
            <div>
              <span style={{ color: '#14B8A6', fontWeight: 700, fontSize: '14px' }}>Viewing with patient consent</span>
              <span style={{ color: '#94A3B8', fontSize: '14px' }}> · Blockchain verified · Read-only access</span>
            </div>
          </div>
        )}

        {loading ? <Spinner /> : denied ? (
          <div style={{
            textAlign: 'center',
            padding: '80px 32px',
            background: '#1A1F2E',
            borderRadius: '16px',
            border: '1px solid rgba(239,68,68,0.2)',
          }}>
            <div style={{ fontSize: '64px', marginBottom: '20px' }}>🔒</div>
            <h2 style={{ color: '#FCA5A5', fontSize: '22px', fontWeight: 700, margin: '0 0 12px' }}>
              Access Denied
            </h2>
            <p style={{ color: '#94A3B8', fontSize: '15px', maxWidth: '400px', margin: '0 auto' }}>
              No active consent from this patient. Ask the patient to grant access via their Consent Manager.
            </p>
          </div>
        ) : records.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '80px 0', color: '#94A3B8' }}>
            No records found for this patient.
          </div>
        ) : (
          records.map(r => <RecordCard key={r.id} record={r} />)
        )}
      </main>
    </div>
  );
}
