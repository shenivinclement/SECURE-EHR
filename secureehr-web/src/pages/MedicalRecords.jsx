import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getRecords } from '../api/api';
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
      border: '1px solid #1E2435',
      borderRadius: '14px',
      overflow: 'hidden',
      marginBottom: '12px',
      transition: 'border-color 0.2s',
      borderColor: open ? '#14B8A6' : '#1E2435',
    }}>
      {/* Header row */}
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
          color: '#14B8A6',
          fontSize: '20px',
          transform: open ? 'rotate(180deg)' : 'rotate(0)',
          transition: 'transform 0.3s ease',
        }}>⌄</div>
      </div>

      {/* Expanded content */}
      <div style={{
        maxHeight: open ? '600px' : '0',
        overflow: 'hidden',
        transition: 'max-height 0.35s ease',
      }}>
        <div style={{ padding: '0 24px 24px', borderTop: '1px solid #1E2435' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginTop: '20px' }}>
            <DetailSection label="Diagnosis / Symptoms" value={record.symptoms} icon="🔍" />
            <DetailSection label="Treatment" value={record.treatment} icon="💊" />
            <DetailSection label="Prescription / Medications" value={record.medications} icon="📋" />
            <DetailSection label="Notes" value={record.notes} icon="📝" />
          </div>
        </div>
      </div>
    </div>
  );
}

function DetailSection({ label, value, icon }) {
  return (
    <div style={{
      background: '#0A0E1A',
      borderRadius: '10px',
      padding: '16px',
      border: '1px solid #1E2435',
    }}>
      <div style={{ color: '#94A3B8', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '8px' }}>
        {icon} {label}
      </div>
      <div style={{ color: '#E2E8F0', fontSize: '14px', lineHeight: 1.6 }}>
        {value || '—'}
      </div>
    </div>
  );
}

export default function MedicalRecords() {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getRecords()
      .then(r => setRecords(r.data))
      .catch(() => navigate('/'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header backTo="/dashboard" backLabel="Dashboard" />
      <main style={{ maxWidth: '900px', margin: '0 auto', padding: '40px 32px' }}>
        <div style={{ marginBottom: '32px' }}>
          <h1 style={{ color: '#fff', fontSize: '28px', fontWeight: 800, margin: '0 0 8px' }}>Medical Records</h1>
          <p style={{ color: '#94A3B8', fontSize: '15px', margin: 0 }}>
            Your complete health history — click any record to expand details.
          </p>
        </div>

        {loading ? <Spinner /> : (
          records.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '80px 0', color: '#94A3B8' }}>
              No records found.
            </div>
          ) : (
            records.map(r => <RecordCard key={r.id} record={r} />)
          )
        )}
      </main>
    </div>
  );
}
