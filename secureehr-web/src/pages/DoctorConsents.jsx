import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDoctorConsents } from '../api/api';
import Header from '../components/Header';
import Spinner from '../components/Spinner';

function formatDate(d) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

function ConsentRow({ consent }) {
  const active = consent.status === 'active';

  return (
    <div style={{
      background: '#1A1F2E',
      border: `1px solid ${active ? 'rgba(20,184,166,0.3)' : '#1E2435'}`,
      borderRadius: '14px',
      padding: '22px 24px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: '20px',
      marginBottom: '12px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flex: 1 }}>
        <div style={{
          width: 48, height: 48,
          background: active ? 'rgba(20,184,166,0.15)' : 'rgba(45,55,72,0.5)',
          borderRadius: '12px',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: '20px', flexShrink: 0,
          border: `1px solid ${active ? 'rgba(20,184,166,0.3)' : '#2D3748'}`,
        }}>🧑</div>
        <div style={{ flex: 1 }}>
          <div style={{ color: '#fff', fontSize: '16px', fontWeight: 700 }}>{consent.patient_name}</div>
          <div style={{ color: '#94A3B8', fontSize: '13px', marginTop: '2px' }}>
            {consent.hospital_name}{consent.specialization ? ` · ${consent.specialization}` : ''}
          </div>
          {consent.purpose && (
            <div style={{ color: '#4B5563', fontSize: '12px', marginTop: '4px' }}>
              Purpose: {consent.purpose}
            </div>
          )}
          <div style={{ display: 'flex', gap: '12px', marginTop: '8px', flexWrap: 'wrap' }}>
            <span style={{
              background: active ? 'rgba(52,211,153,0.15)' : 'rgba(148,163,184,0.1)',
              color: active ? '#34D399' : '#94A3B8',
              fontSize: '11px', fontWeight: 700,
              padding: '3px 10px', borderRadius: '20px',
              textTransform: 'uppercase', letterSpacing: '0.5px',
            }}>
              {active ? 'Active' : 'Revoked'}
            </span>
            {consent.expiry_date && (
              <span style={{ color: '#4B5563', fontSize: '12px' }}>
                Expires {formatDate(consent.expiry_date)}
              </span>
            )}
          </div>
        </div>
      </div>

      <div style={{ textAlign: 'right', flexShrink: 0 }}>
        <div style={{ color: '#4B5563', fontSize: '12px' }}>
          Granted {formatDate(consent.granted_at)}
        </div>
        {consent.revoked_at && (
          <div style={{ color: '#4B5563', fontSize: '12px', marginTop: '2px' }}>
            Revoked {formatDate(consent.revoked_at)}
          </div>
        )}
      </div>
    </div>
  );
}

export default function DoctorConsents() {
  const [consents, setConsents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('active');
  const navigate = useNavigate();

  useEffect(() => {
    getDoctorConsents()
      .then(r => setConsents(r.data))
      .catch(() => navigate('/'))
      .finally(() => setLoading(false));
  }, []);

  const activeCount = consents.filter(c => c.status === 'active').length;
  const visible = filter === 'all' ? consents : consents.filter(c => c.status === filter);

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header backTo="/doctor" backLabel="Dashboard" />
      <main style={{ maxWidth: '900px', margin: '0 auto', padding: '40px 32px' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '32px', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h1 style={{ color: '#fff', fontSize: '28px', fontWeight: 800, margin: '0 0 8px' }}>Consent Manager</h1>
            <p style={{ color: '#94A3B8', fontSize: '15px', margin: 0 }}>
              Patients who have granted or revoked your access to their records.
            </p>
          </div>
          <div style={{
            background: 'rgba(20,184,166,0.1)', border: '1px solid rgba(20,184,166,0.3)',
            borderRadius: '10px', padding: '10px 20px', textAlign: 'center',
          }}>
            <div style={{ color: '#14B8A6', fontSize: '24px', fontWeight: 800 }}>{activeCount}</div>
            <div style={{ color: '#94A3B8', fontSize: '12px' }}>Active Consents</div>
          </div>
        </div>

        <div style={{
          background: 'rgba(20,184,166,0.05)', border: '1px solid rgba(20,184,166,0.15)',
          borderRadius: '10px', padding: '12px 20px', marginBottom: '24px',
          display: 'flex', alignItems: 'center', gap: '10px',
        }}>
          <span>ℹ️</span>
          <span style={{ color: '#94A3B8', fontSize: '13px' }}>
            This is a read-only view. Patients control access from their own Consent Manager.
          </span>
        </div>

        <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
          {['active', 'revoked', 'all'].map(f => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              style={{
                background: filter === f ? '#14B8A6' : '#1A1F2E',
                border: `1px solid ${filter === f ? '#14B8A6' : '#1E2435'}`,
                color: filter === f ? '#0A0E1A' : '#94A3B8',
                borderRadius: '8px',
                padding: '8px 18px',
                fontSize: '13px',
                fontWeight: 700,
                textTransform: 'capitalize',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
            >
              {f}
            </button>
          ))}
        </div>

        {loading ? <Spinner /> : (
          visible.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '80px 0', color: '#94A3B8' }}>
              No {filter !== 'all' ? filter : ''} consents on record.
            </div>
          ) : (
            visible.map(c => <ConsentRow key={c.consent_id} consent={c} />)
          )
        )}
      </main>
    </div>
  );
}
