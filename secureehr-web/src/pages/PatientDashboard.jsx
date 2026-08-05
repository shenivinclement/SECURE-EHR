import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getRecords, getConsents, getMe } from '../api/api';
import Header from '../components/Header';
import Spinner from '../components/Spinner';

function StatCard({ label, value, color = '#14B8A6', icon }) {
  return (
    <div style={{
      background: '#1A1F2E',
      border: '1px solid #1E2435',
      borderRadius: '14px',
      padding: '24px 28px',
      flex: 1,
      minWidth: '160px',
    }}>
      <div style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '12px' }}>
        {icon} {label}
      </div>
      <div style={{ color, fontSize: '40px', fontWeight: 800, lineHeight: 1 }}>{value}</div>
    </div>
  );
}

function NavCard({ label, description, icon, onClick }) {
  const [hovered, setHovered] = useState(false);
  return (
    <div
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: hovered ? '#1E2435' : '#1A1F2E',
        border: `1px solid ${hovered ? '#14B8A6' : '#1E2435'}`,
        borderRadius: '16px',
        padding: '32px',
        cursor: 'pointer',
        transition: 'all 0.25s ease',
        transform: hovered ? 'translateY(-4px)' : 'translateY(0)',
        boxShadow: hovered ? '0 12px 40px rgba(20,184,166,0.15)' : 'none',
        flex: 1,
        minWidth: '200px',
      }}
    >
      <div style={{ fontSize: '36px', marginBottom: '16px' }}>{icon}</div>
      <h3 style={{ color: '#fff', fontSize: '18px', fontWeight: 700, margin: '0 0 8px' }}>{label}</h3>
      <p style={{ color: '#94A3B8', fontSize: '14px', margin: 0, lineHeight: 1.5 }}>{description}</p>
      <div style={{ marginTop: '20px', color: '#14B8A6', fontSize: '13px', fontWeight: 600, opacity: hovered ? 1 : 0, transition: 'opacity 0.2s' }}>
        Open →
      </div>
    </div>
  );
}

export default function PatientDashboard() {
  const { user, setUser } = useAuth();
  const [records, setRecords] = useState([]);
  const [consents, setConsents] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const load = async () => {
      try {
        const [rRes, cRes] = await Promise.all([getRecords(), getConsents()]);
        setRecords(rRes.data);
        setConsents(cRes.data);
        if (!user) {
          const { data: me } = await getMe();
          setUser(me);
        }
      } catch {
        navigate('/');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const activeConsents = consents.filter(c => c.status === 'active').length;
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good Morning' : hour < 17 ? 'Good Afternoon' : 'Good Evening';

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header />
      <main style={{ maxWidth: '1100px', margin: '0 auto', padding: '40px 32px' }}>
        {/* Greeting */}
        <div style={{ marginBottom: '40px' }}>
          <h1 style={{ color: '#fff', fontSize: '32px', fontWeight: 800, margin: '0 0 8px', letterSpacing: '-0.5px' }}>
            {greeting}, {user?.name?.split(' ')[0] || 'there'} 👋
          </h1>
          <p style={{ color: '#94A3B8', fontSize: '16px', margin: 0 }}>
            Here's an overview of your health data today.
          </p>
        </div>

        {loading ? <Spinner /> : (
          <>
            {/* Stat Cards */}
            <div style={{ display: 'flex', gap: '16px', marginBottom: '48px', flexWrap: 'wrap' }}>
              <StatCard label="Medical Records" value={records.length} icon="📋" />
              <StatCard label="Consents Issued" value={consents.length} color="#818CF8" icon="🔐" />
              <StatCard label="Active Consents" value={activeConsents} color="#34D399" icon="✅" />
            </div>

            {/* Nav Cards */}
            <h2 style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '20px' }}>
              Quick Access
            </h2>
            <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
              <NavCard
                label="Medical Records"
                description="View your complete health history, diagnoses, and prescriptions."
                icon="🏥"
                onClick={() => navigate('/records')}
              />
              <NavCard
                label="Consent Manager"
                description="Control which doctors can access your records via blockchain consent."
                icon="🔐"
                onClick={() => navigate('/consent')}
              />
              <NavCard
                label="Hospital Finder"
                description="AI-ranked hospitals near you, matched to your primary diagnosis."
                icon="🔬"
                onClick={() => navigate('/hospitals')}
              />
              <NavCard
                label="My Visits"
                description="View upcoming appointments and your complete visit history."
                icon="📅"
                onClick={() => navigate('/visits')}
              />
              <NavCard
                label="AI Chat"
                description="Ask our AI assistant about your health data and get instant answers."
                icon="🤖"
                onClick={() => navigate('/chat')}
              />
            </div>

            {/* Blockchain badge */}
            <div style={{
              marginTop: '40px',
              background: 'rgba(20,184,166,0.05)',
              border: '1px solid rgba(20,184,166,0.2)',
              borderRadius: '12px',
              padding: '16px 24px',
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
            }}>
              <span style={{ fontSize: '20px' }}>⛓️</span>
              <div>
                <div style={{ color: '#14B8A6', fontSize: '13px', fontWeight: 700 }}>Blockchain Verified</div>
                <div style={{ color: '#94A3B8', fontSize: '13px' }}>All consent grants and revocations are immutably recorded on-chain.</div>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
