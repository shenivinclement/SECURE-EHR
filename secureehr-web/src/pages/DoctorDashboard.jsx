import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getPatients, getMe } from '../api/api';
import Header from '../components/Header';
import Spinner from '../components/Spinner';

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
      <div style={{
        marginTop: '20px', color: '#14B8A6', fontSize: '13px', fontWeight: 600,
        opacity: hovered ? 1 : 0, transition: 'opacity 0.2s',
      }}>
        Open →
      </div>
    </div>
  );
}

export default function DoctorDashboard() {
  const { user, setUser } = useAuth();
  const [patientCount, setPatientCount] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const load = async () => {
      try {
        const [pRes] = await Promise.all([getPatients()]);
        setPatientCount(pRes.data.length);
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

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good Morning' : hour < 17 ? 'Good Afternoon' : 'Good Evening';
  const lastName = user?.name?.split(' ').slice(-1)[0] || '';

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header />
      <main style={{ maxWidth: '1100px', margin: '0 auto', padding: '40px 32px' }}>
        <div style={{ marginBottom: '40px' }}>
          <h1 style={{ color: '#fff', fontSize: '32px', fontWeight: 800, margin: '0 0 8px', letterSpacing: '-0.5px' }}>
            {greeting}, Dr. {lastName} 👋
          </h1>
          <p style={{ color: '#94A3B8', fontSize: '16px', margin: 0 }}>
            Welcome to your clinical dashboard.
          </p>
        </div>

        {loading ? <Spinner /> : (
          <>
            {/* Stat card */}
            <div style={{ display: 'flex', gap: '16px', marginBottom: '48px', flexWrap: 'wrap' }}>
              <div style={{
                background: '#1A1F2E', border: '1px solid #1E2435',
                borderRadius: '14px', padding: '24px 28px', flex: 1, minWidth: '160px',
              }}>
                <div style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '12px' }}>
                  🏥 Accessible Patients
                </div>
                <div style={{ color: '#14B8A6', fontSize: '40px', fontWeight: 800, lineHeight: 1 }}>{patientCount ?? '—'}</div>
              </div>
              <div style={{
                background: '#1A1F2E', border: '1px solid #1E2435',
                borderRadius: '14px', padding: '24px 28px', flex: 1, minWidth: '160px',
              }}>
                <div style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '12px' }}>
                  ⛓️ Blockchain Verified
                </div>
                <div style={{ color: '#34D399', fontSize: '18px', fontWeight: 700, lineHeight: 1.3 }}>All consents<br />on-chain</div>
              </div>
            </div>

            <h2 style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '20px' }}>
              Quick Access
            </h2>
            <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
              <NavCard
                label="My Patients"
                description="View all patients who have granted you consent to access their records."
                icon="👥"
                onClick={() => navigate('/doctor/patients')}
              />
              <NavCard
                label="AI Chat"
                description="Ask the AI assistant about patient data, diagnoses, and clinical decisions."
                icon="🤖"
                onClick={() => navigate('/chat')}
              />
            </div>
          </>
        )}
      </main>
    </div>
  );
}
