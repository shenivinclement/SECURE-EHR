import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getPatients } from '../api/api';
import Header from '../components/Header';
import Spinner from '../components/Spinner';

function calcAge(dob) {
  if (!dob) return null;
  const birth = new Date(dob);
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
  return age;
}

function formatDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function PatientCard({ patient, onClick }) {
  const [hovered, setHovered] = useState(false);
  const age = calcAge(patient.date_of_birth);

  return (
    <div
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: hovered ? '#1E2435' : '#1A1F2E',
        border: `1px solid ${hovered ? '#14B8A6' : '#1E2435'}`,
        borderRadius: '14px',
        padding: '22px 24px',
        cursor: 'pointer',
        transition: 'all 0.25s ease',
        transform: hovered ? 'translateY(-2px)' : 'translateY(0)',
        boxShadow: hovered ? '0 8px 32px rgba(20,184,166,0.1)' : 'none',
      }}
    >
      {/* Top row: avatar + name + badge */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: 50, height: 50,
            background: 'linear-gradient(135deg, rgba(20,184,166,0.25), rgba(20,184,166,0.1))',
            border: '1px solid rgba(20,184,166,0.35)',
            borderRadius: '50%',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '20px', fontWeight: 700, color: '#14B8A6', flexShrink: 0,
          }}>
            {patient.name?.charAt(0)?.toUpperCase() || '?'}
          </div>
          <div>
            <div style={{ color: '#fff', fontSize: '16px', fontWeight: 700 }}>{patient.name}</div>
            <div style={{ color: '#94A3B8', fontSize: '13px', marginTop: '2px' }}>
              🏥 {patient.hospital_name}
            </div>
          </div>
        </div>
        <span style={{
          background: 'rgba(52,211,153,0.15)', color: '#34D399',
          fontSize: '11px', fontWeight: 700,
          padding: '4px 12px', borderRadius: '20px',
          textTransform: 'uppercase', letterSpacing: '0.5px',
          flexShrink: 0,
        }}>Active</span>
      </div>

      {/* Info chips */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '10px' }}>
        <Chip label="Condition" value={patient.primary_condition} accent />
        <Chip label="Gender" value={patient.gender || '—'} />
        <Chip label="Blood Group" value={patient.blood_group || '—'} />
        <Chip label="Age" value={age != null ? `${age} yrs` : '—'} />
      </div>

      {/* Records + expiry */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '14px', alignItems: 'center' }}>
        <span style={{
          background: 'rgba(129,140,248,0.15)', color: '#818CF8',
          fontSize: '12px', fontWeight: 700,
          padding: '3px 10px', borderRadius: '20px',
        }}>
          📋 {patient.total_records} medical record{patient.total_records !== 1 ? 's' : ''}
        </span>
        {patient.consent_expiry_date && (
          <span style={{ color: '#4B5563', fontSize: '12px' }}>
            Consent expires {formatDate(patient.consent_expiry_date)}
          </span>
        )}
      </div>

      <div style={{
        marginTop: '14px', color: '#14B8A6', fontSize: '13px', fontWeight: 600,
        opacity: hovered ? 1 : 0, transition: 'opacity 0.2s',
      }}>
        View Records →
      </div>
    </div>
  );
}

function Chip({ label, value, accent }) {
  return (
    <div style={{
      background: '#0A0E1A', borderRadius: '8px', padding: '10px 12px',
      border: `1px solid ${accent ? 'rgba(20,184,166,0.2)' : '#1E2435'}`,
    }}>
      <div style={{ color: '#4B5563', fontSize: '11px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
        {label}
      </div>
      <div style={{ color: accent ? '#14B8A6' : '#E2E8F0', fontSize: '13px', fontWeight: 600, marginTop: '4px' }}>
        {value || '—'}
      </div>
    </div>
  );
}

export default function DoctorPatients() {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getPatients()
      .then(r => setPatients(r.data))
      .catch(() => navigate('/'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header backTo="/doctor" backLabel="Dashboard" />
      <main style={{ maxWidth: '1100px', margin: '0 auto', padding: '40px 32px' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '32px', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h1 style={{ color: '#fff', fontSize: '28px', fontWeight: 800, margin: '0 0 8px' }}>My Patients</h1>
            <p style={{ color: '#94A3B8', fontSize: '15px', margin: 0 }}>
              Patients who have granted you blockchain-verified consent.
            </p>
          </div>
          {patients.length > 0 && (
            <div style={{
              background: 'rgba(20,184,166,0.1)', border: '1px solid rgba(20,184,166,0.3)',
              borderRadius: '10px', padding: '10px 20px', textAlign: 'center',
            }}>
              <div style={{ color: '#14B8A6', fontSize: '24px', fontWeight: 800 }}>{patients.length}</div>
              <div style={{ color: '#94A3B8', fontSize: '12px' }}>Patients</div>
            </div>
          )}
        </div>

        {loading ? <Spinner /> : patients.length === 0 ? (
          <div style={{
            textAlign: 'center', padding: '80px 32px',
            background: '#1A1F2E', borderRadius: '16px',
            border: '1px solid #1E2435',
          }}>
            <div style={{ fontSize: '56px', marginBottom: '20px' }}>👥</div>
            <h2 style={{ color: '#fff', fontSize: '20px', fontWeight: 700, margin: '0 0 12px' }}>
              No Patients Yet
            </h2>
            <p style={{ color: '#94A3B8', fontSize: '15px', maxWidth: '380px', margin: '0 auto' }}>
              Patients who grant you consent via the Consent Manager will appear here.
            </p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(460px, 1fr))', gap: '16px' }}>
            {patients.map(p => (
              <PatientCard
                key={p.patient_id}
                patient={p}
                onClick={() => navigate(`/doctor/patient/${p.patient_id}`, { state: { name: p.name } })}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
