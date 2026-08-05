import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import Spinner from '../components/Spinner';
import { getMe } from '../api/api';
import { useAuth } from '../context/AuthContext';

export default function Profile() {
  const { user, setUser, token } = useAuth();
  const [profile, setProfile] = useState(user);
  const [loading, setLoading] = useState(!user);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    if (!token && !localStorage.getItem('token')) {
      navigate('/');
      return;
    }
    if (profile) return;
    let active = true;
    (async () => {
      try {
        const { data: me } = await getMe();
        if (!active) return;
        setProfile(me);
        setUser?.(me);
      } catch {
        if (active) setError('Could not load your profile. Please sign in again.');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [profile, token, navigate, setUser]);

  const dashboardPath = profile?.role === 'doctor' ? '/doctor' : '/dashboard';

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header backTo={dashboardPath} backLabel="Dashboard" />

      <main style={{ maxWidth: '720px', margin: '0 auto', padding: '32px 24px' }}>
        <h1 style={{ color: '#fff', fontSize: '26px', fontWeight: 800, margin: '0 0 4px' }}>
          My Profile
        </h1>
        <p style={{ color: '#94A3B8', fontSize: '14px', margin: '0 0 28px' }}>
          Your account details
        </p>

        {loading && <Spinner />}

        {!loading && error && (
          <div style={{
            background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)',
            color: '#FCA5A5', padding: '14px 18px', borderRadius: '10px', fontSize: '14px',
          }}>
            {error}
          </div>
        )}

        {!loading && !error && profile && (
          <div style={{
            background: '#1A1F2E', border: '1px solid #1E2435',
            borderRadius: '16px', padding: '28px',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '18px', marginBottom: '24px' }}>
              <div style={{
                width: 64, height: 64, borderRadius: '18px',
                background: 'linear-gradient(135deg, #14B8A6, #0D9488)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '26px', fontWeight: 800, color: '#fff',
              }}>
                {(profile.name || '?').charAt(0).toUpperCase()}
              </div>
              <div>
                <div style={{ color: '#fff', fontSize: '20px', fontWeight: 700 }}>{profile.name}</div>
                <div style={{ color: '#14B8A6', fontSize: '13px', textTransform: 'capitalize' }}>
                  {profile.role} account
                </div>
              </div>
            </div>

            <ProfileRow label="Full Name" value={profile.name} />
            <ProfileRow label="Email Address" value={profile.email} />
            <ProfileRow label="Role" value={profile.role} capitalize />
            <ProfileRow label="Account ID" value={profile.id != null ? `#${profile.id}` : '—'} />
            <ProfileRow
              label="Status"
              value={profile.is_active === false ? 'Inactive' : 'Active'}
            />
          </div>
        )}
      </main>
    </div>
  );
}

function ProfileRow({ label, value, capitalize }) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      padding: '14px 0', borderBottom: '1px solid #1E2435',
    }}>
      <span style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
        {label}
      </span>
      <span style={{ color: '#fff', fontSize: '15px', textTransform: capitalize ? 'capitalize' : 'none' }}>
        {value}
      </span>
    </div>
  );
}
