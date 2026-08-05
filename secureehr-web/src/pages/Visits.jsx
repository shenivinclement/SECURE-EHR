import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getVisits } from '../api/api';
import Header from '../components/Header';
import Spinner from '../components/Spinner';

function isUpcoming(visit) {
  if (visit.status === 'scheduled') return true;
  if (visit.visit_date) {
    return new Date(visit.visit_date) >= new Date();
  }
  return false;
}

function VisitCard({ visit }) {
  const [open, setOpen] = useState(false);
  const upcoming = isUpcoming(visit);

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-US', {
      weekday: 'short', year: 'numeric', month: 'long', day: 'numeric',
    });
  };

  return (
    <div style={{
      background: '#1A1F2E',
      border: `1px solid ${open ? '#14B8A6' : upcoming ? 'rgba(20,184,166,0.2)' : '#1E2435'}`,
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
          gap: '16px',
          cursor: 'pointer',
          userSelect: 'none',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flex: 1 }}>
          <div style={{
            width: 44, height: 44, flexShrink: 0,
            background: upcoming
              ? 'linear-gradient(135deg, rgba(20,184,166,0.2), rgba(20,184,166,0.1))'
              : 'rgba(45,55,72,0.4)',
            border: `1px solid ${upcoming ? 'rgba(20,184,166,0.3)' : '#2D3748'}`,
            borderRadius: '12px',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '18px',
            transition: 'all 0.2s',
          }}>
            {upcoming ? '📅' : '✓'}
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
              <span style={{ color: '#fff', fontSize: '16px', fontWeight: 700 }}>{visit.reason}</span>
              <span style={{
                background: upcoming ? 'rgba(52,211,153,0.15)' : 'rgba(148,163,184,0.1)',
                color: upcoming ? '#34D399' : '#94A3B8',
                fontSize: '11px', fontWeight: 700,
                padding: '3px 10px', borderRadius: '20px',
                textTransform: 'uppercase', letterSpacing: '0.5px',
              }}>
                {upcoming ? 'Upcoming' : 'Completed'}
              </span>
            </div>
            <div style={{ color: '#94A3B8', fontSize: '13px', marginTop: '4px' }}>
              {visit.doctor_name}{visit.department ? ` · ${visit.department}` : ''}
            </div>
            <div style={{ display: 'flex', gap: '16px', marginTop: '6px', flexWrap: 'wrap' }}>
              <span style={{ color: '#4B5563', fontSize: '12px' }}>🏥 {visit.hospital_name}</span>
              <span style={{ color: '#4B5563', fontSize: '12px' }}>🗓 {formatDate(visit.visit_date)}</span>
            </div>
          </div>
        </div>
        <div style={{
          color: '#14B8A6', fontSize: '20px', flexShrink: 0,
          transform: open ? 'rotate(180deg)' : 'rotate(0)',
          transition: 'transform 0.3s ease',
        }}>⌄</div>
      </div>

      {/* Notes expand */}
      <div style={{
        maxHeight: open ? '200px' : '0',
        overflow: 'hidden',
        transition: 'max-height 0.3s ease',
      }}>
        <div style={{ padding: '0 24px 20px', borderTop: '1px solid #1E2435' }}>
          <div style={{
            marginTop: '16px',
            background: '#0A0E1A',
            borderRadius: '10px',
            padding: '14px 16px',
            border: '1px solid #1E2435',
          }}>
            <div style={{ color: '#94A3B8', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '8px' }}>
              📝 Notes
            </div>
            <div style={{ color: '#E2E8F0', fontSize: '14px', lineHeight: 1.6 }}>
              {visit.notes || 'No notes recorded.'}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function Section({ title, visits, emptyText }) {
  if (visits.length === 0) return (
    <div style={{ marginBottom: '40px' }}>
      <h2 style={{
        color: '#94A3B8', fontSize: '13px', fontWeight: 700,
        textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '16px',
      }}>{title}</h2>
      <div style={{
        background: '#1A1F2E', border: '1px solid #1E2435',
        borderRadius: '12px', padding: '32px',
        textAlign: 'center', color: '#4B5563', fontSize: '14px',
      }}>{emptyText}</div>
    </div>
  );

  return (
    <div style={{ marginBottom: '40px' }}>
      <h2 style={{
        color: '#94A3B8', fontSize: '13px', fontWeight: 700,
        textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '16px',
        display: 'flex', alignItems: 'center', gap: '8px',
      }}>
        {title}
        <span style={{
          background: title.includes('Upcoming') ? 'rgba(20,184,166,0.15)' : 'rgba(148,163,184,0.1)',
          color: title.includes('Upcoming') ? '#14B8A6' : '#94A3B8',
          fontSize: '11px', fontWeight: 700,
          padding: '2px 8px', borderRadius: '10px',
        }}>{visits.length}</span>
      </h2>
      {visits.map(v => <VisitCard key={v.id} visit={v} />)}
    </div>
  );
}

export default function Visits() {
  const [upcoming, setUpcoming] = useState([]);
  const [past, setPast] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getVisits()
      .then(r => {
        const all = r.data || [];
        setUpcoming(all.filter(v => isUpcoming(v)));
        setPast(all.filter(v => !isUpcoming(v)));
      })
      .catch(err => {
        console.error('Visits fetch failed:', err?.response?.data || err.message);
        navigate('/');
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header backTo="/dashboard" backLabel="Dashboard" />
      <main style={{ maxWidth: '900px', margin: '0 auto', padding: '40px 32px' }}>
        <div style={{ marginBottom: '36px' }}>
          <h1 style={{ color: '#fff', fontSize: '28px', fontWeight: 800, margin: '0 0 8px' }}>My Visits</h1>
          <p style={{ color: '#94A3B8', fontSize: '15px', margin: 0 }}>
            Your scheduled and completed hospital visits.
          </p>
        </div>

        {loading ? <Spinner /> : (
          <>
            <Section
              title="Upcoming"
              visits={upcoming}
              emptyText="No upcoming visits scheduled."
            />
            <Section
              title="Past Visits"
              visits={past}
              emptyText="No completed visits on record."
            />
          </>
        )}
      </main>
    </div>
  );
}
