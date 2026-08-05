import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getConsents, grantConsent, revokeConsent } from '../api/api';
import Header from '../components/Header';
import Spinner from '../components/Spinner';

function Toggle({ active, onChange, loading }) {
  return (
    <div
      onClick={() => !loading && onChange(!active)}
      style={{
        width: 52, height: 28,
        background: active ? '#14B8A6' : '#2D3748',
        borderRadius: 14,
        cursor: loading ? 'not-allowed' : 'pointer',
        position: 'relative',
        transition: 'background 0.3s ease',
        flexShrink: 0,
        opacity: loading ? 0.6 : 1,
      }}
    >
      <div style={{
        width: 22, height: 22,
        background: '#fff',
        borderRadius: '50%',
        position: 'absolute',
        top: 3,
        left: active ? 27 : 3,
        transition: 'left 0.3s ease',
        boxShadow: '0 2px 6px rgba(0,0,0,0.3)',
      }} />
    </div>
  );
}

function ConsentCard({ consent, onToggle }) {
  const [toggling, setToggling] = useState(false);
  const active = consent.status === 'active';

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  };

  const handleToggle = async (newState) => {
    setToggling(true);
    try {
      await onToggle(consent, newState);
    } finally {
      setToggling(false);
    }
  };

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
      transition: 'border-color 0.3s',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flex: 1 }}>
        <div style={{
          width: 48, height: 48,
          background: active ? 'rgba(20,184,166,0.15)' : 'rgba(45,55,72,0.5)',
          borderRadius: '12px',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: '20px', flexShrink: 0,
          border: `1px solid ${active ? 'rgba(20,184,166,0.3)' : '#2D3748'}`,
          transition: 'all 0.3s',
        }}>👨‍⚕️</div>
        <div style={{ flex: 1 }}>
          <div style={{ color: '#fff', fontSize: '16px', fontWeight: 700 }}>{consent.doctor_name}</div>
          <div style={{ color: '#94A3B8', fontSize: '13px', marginTop: '2px' }}>
            {consent.specialization} · {consent.hospital_name}
          </div>
          <div style={{ display: 'flex', gap: '12px', marginTop: '8px', flexWrap: 'wrap' }}>
            <span style={{
              background: active ? 'rgba(52,211,153,0.15)' : 'rgba(148,163,184,0.1)',
              color: active ? '#34D399' : '#94A3B8',
              fontSize: '11px', fontWeight: 700,
              padding: '3px 10px', borderRadius: '20px',
              textTransform: 'uppercase', letterSpacing: '0.5px',
              transition: 'all 0.3s',
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

      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <span style={{ color: active ? '#14B8A6' : '#4B5563', fontSize: '13px', fontWeight: 600, transition: 'color 0.3s' }}>
          {active ? 'Access On' : 'Access Off'}
        </span>
        <Toggle active={active} onChange={handleToggle} loading={toggling} />
      </div>
    </div>
  );
}

export default function ConsentManager() {
  const [consents, setConsents] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const refetch = async () => {
    setLoading(true);
    try {
      const r = await getConsents();
      setConsents(r.data);
    } catch (err) {
      console.error('Failed to fetch consents:', err?.response?.data || err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    getConsents()
      .then(r => setConsents(r.data))
      .catch(() => navigate('/'))
      .finally(() => setLoading(false));
  }, []);

  const handleToggle = async (consent, newState) => {
    try {
      if (newState) {
        await grantConsent({
          doctor_name: consent.doctor_name,
          hospital_name: consent.hospital_name,
          specialization: consent.specialization,
          expiry_date: consent.expiry_date || '2027-12-31',
        });
      } else {
        // Backend expects {consent_id}, NOT {id}
        const consentId = consent.consent_id ?? consent.id;
        await revokeConsent({ consent_id: consentId });
      }
      await refetch();
    } catch (err) {
      console.error('Consent toggle failed:', err?.response?.status, err?.response?.data || err.message);
    }
  };

  const activeCount = consents.filter(c => c.status === 'active').length;

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header backTo="/dashboard" backLabel="Dashboard" />
      <main style={{ maxWidth: '900px', margin: '0 auto', padding: '40px 32px' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '32px', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <h1 style={{ color: '#fff', fontSize: '28px', fontWeight: 800, margin: '0 0 8px' }}>Consent Manager</h1>
            <p style={{ color: '#94A3B8', fontSize: '15px', margin: 0 }}>
              Control which doctors can access your medical records.
            </p>
          </div>
          <div style={{
            background: 'rgba(20,184,166,0.1)',
            border: '1px solid rgba(20,184,166,0.3)',
            borderRadius: '10px',
            padding: '10px 20px',
            textAlign: 'center',
          }}>
            <div style={{ color: '#14B8A6', fontSize: '24px', fontWeight: 800 }}>{activeCount}</div>
            <div style={{ color: '#94A3B8', fontSize: '12px' }}>Active Consents</div>
          </div>
        </div>

        {/* Blockchain note */}
        <div style={{
          background: 'rgba(20,184,166,0.05)',
          border: '1px solid rgba(20,184,166,0.15)',
          borderRadius: '10px',
          padding: '12px 20px',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
        }}>
          <span>⛓️</span>
          <span style={{ color: '#94A3B8', fontSize: '13px' }}>
            All consent changes are immutably recorded on the blockchain. Toggle to grant or revoke access instantly.
          </span>
        </div>

        {loading ? <Spinner /> : (
          consents.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '80px 0', color: '#94A3B8' }}>
              No consents on record.
            </div>
          ) : (
            consents.map(c => (
              <ConsentCard key={c.id} consent={c} onToggle={handleToggle} />
            ))
          )
        )}
      </main>
    </div>
  );
}
