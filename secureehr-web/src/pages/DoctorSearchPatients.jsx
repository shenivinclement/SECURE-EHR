import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchPatients } from '../api/api';
import Header from '../components/Header';
import Spinner from '../components/Spinner';

function ResultRow({ patient, onOpen }) {
  const [hovered, setHovered] = useState(false);
  const clickable = patient.has_active_consent;

  return (
    <div
      onClick={() => clickable && onOpen(patient)}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: hovered && clickable ? '#1E2435' : '#1A1F2E',
        border: `1px solid ${hovered && clickable ? '#14B8A6' : '#1E2435'}`,
        borderRadius: '14px',
        padding: '18px 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '16px',
        marginBottom: '12px',
        cursor: clickable ? 'pointer' : 'default',
        transition: 'all 0.2s',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div style={{
          width: 44, height: 44,
          background: 'linear-gradient(135deg, rgba(20,184,166,0.25), rgba(20,184,166,0.1))',
          border: '1px solid rgba(20,184,166,0.35)',
          borderRadius: '50%',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: '18px', fontWeight: 700, color: '#14B8A6', flexShrink: 0,
        }}>
          {patient.name?.charAt(0)?.toUpperCase() || '?'}
        </div>
        <div>
          <div style={{ color: '#fff', fontSize: '15px', fontWeight: 700 }}>{patient.name}</div>
          <div style={{ color: '#94A3B8', fontSize: '13px', marginTop: '2px' }}>
            {patient.gender || 'Gender unknown'}
          </div>
        </div>
      </div>

      {clickable ? (
        <span style={{
          background: 'rgba(52,211,153,0.15)', color: '#34D399',
          fontSize: '11px', fontWeight: 700,
          padding: '4px 12px', borderRadius: '20px',
          textTransform: 'uppercase', letterSpacing: '0.5px',
        }}>
          View Records →
        </span>
      ) : (
        <span style={{
          background: 'rgba(148,163,184,0.1)', color: '#94A3B8',
          fontSize: '11px', fontWeight: 700,
          padding: '4px 12px', borderRadius: '20px',
          textTransform: 'uppercase', letterSpacing: '0.5px',
        }}>
          No Active Consent
        </span>
      )}
    </div>
  );
}

export default function DoctorSearchPatients() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const runSearch = async (e) => {
    e.preventDefault();
    const q = query.trim();
    if (!q) return;
    setLoading(true);
    setError(null);
    try {
      const { data } = await searchPatients(q);
      setResults(data);
    } catch (err) {
      setError(err?.response?.data?.detail || 'Search failed. Please try again.');
      setResults(null);
    } finally {
      setLoading(false);
    }
  };

  const openPatient = (patient) => {
    navigate(`/doctor/patient/${patient.patient_id}`, { state: { name: patient.name } });
  };

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header backTo="/doctor" backLabel="Dashboard" />
      <main style={{ maxWidth: '900px', margin: '0 auto', padding: '40px 32px' }}>
        <div style={{ marginBottom: '32px' }}>
          <h1 style={{ color: '#fff', fontSize: '28px', fontWeight: 800, margin: '0 0 8px' }}>Search Patients</h1>
          <p style={{ color: '#94A3B8', fontSize: '15px', margin: 0 }}>
            Find any registered patient by name. Records are only accessible if they've granted you consent.
          </p>
        </div>

        <form onSubmit={runSearch} style={{ display: 'flex', gap: '12px', marginBottom: '28px' }}>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by patient name…"
            autoFocus
            style={{
              flex: 1,
              background: '#1A1F2E',
              border: '1px solid #1E2435',
              borderRadius: '10px',
              padding: '14px 18px',
              color: '#fff',
              fontSize: '15px',
              outline: 'none',
            }}
            onFocus={(e) => { e.target.style.borderColor = '#14B8A6'; }}
            onBlur={(e) => { e.target.style.borderColor = '#1E2435'; }}
          />
          <button
            type="submit"
            disabled={loading || !query.trim()}
            style={{
              background: '#14B8A6',
              border: 'none',
              borderRadius: '10px',
              padding: '14px 28px',
              color: '#0A0E1A',
              fontSize: '15px',
              fontWeight: 700,
              cursor: loading || !query.trim() ? 'not-allowed' : 'pointer',
              opacity: loading || !query.trim() ? 0.6 : 1,
            }}
          >
            Search
          </button>
        </form>

        {loading && <Spinner />}

        {!loading && error && (
          <div style={{
            textAlign: 'center', padding: '24px',
            background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.3)',
            borderRadius: '12px', color: '#F87171', fontSize: '14px', marginBottom: '20px',
          }}>
            {error}
          </div>
        )}

        {!loading && results !== null && (
          results.length === 0 ? (
            <div style={{
              textAlign: 'center', padding: '80px 32px',
              background: '#1A1F2E', borderRadius: '16px',
              border: '1px solid #1E2435',
            }}>
              <div style={{ fontSize: '56px', marginBottom: '20px' }}>🔍</div>
              <h2 style={{ color: '#fff', fontSize: '20px', fontWeight: 700, margin: '0 0 12px' }}>
                No Patients Found
              </h2>
              <p style={{ color: '#94A3B8', fontSize: '15px', maxWidth: '380px', margin: '0 auto' }}>
                No patients match "{query}". Try a different name.
              </p>
            </div>
          ) : (
            <div>
              {results.map((p) => (
                <ResultRow key={p.patient_id} patient={p} onOpen={openPatient} />
              ))}
            </div>
          )
        )}
      </main>
    </div>
  );
}
