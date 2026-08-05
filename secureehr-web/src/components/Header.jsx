import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Header({ backTo, backLabel }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '20px 32px',
      borderBottom: '1px solid #1E2435',
      background: '#0A0E1A',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
        {backTo && (
          <button
            onClick={() => navigate(backTo)}
            style={{
              background: 'none',
              border: '1px solid #1E2435',
              color: '#94A3B8',
              padding: '8px 16px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontSize: '14px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'all 0.2s',
            }}
            onMouseEnter={e => { e.target.style.borderColor = '#14B8A6'; e.target.style.color = '#14B8A6'; }}
            onMouseLeave={e => { e.target.style.borderColor = '#1E2435'; e.target.style.color = '#94A3B8'; }}
          >
            ← {backLabel || 'Back'}
          </button>
        )}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: 36, height: 36, background: 'linear-gradient(135deg, #14B8A6, #0D9488)',
            borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '16px', fontWeight: 700, color: '#fff',
          }}>S</div>
          <span style={{ color: '#fff', fontWeight: 700, fontSize: '18px', letterSpacing: '-0.3px' }}>SecureEHR</span>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
        {user && (
          <div style={{ textAlign: 'right' }}>
            <div style={{ color: '#fff', fontSize: '14px', fontWeight: 600 }}>{user.name}</div>
            <div style={{ color: '#14B8A6', fontSize: '12px', textTransform: 'capitalize' }}>{user.role}</div>
          </div>
        )}
        <button
          onClick={handleLogout}
          style={{
            background: 'none',
            border: '1px solid #2D3748',
            color: '#94A3B8',
            padding: '8px 20px',
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: '14px',
            transition: 'all 0.2s',
          }}
          onMouseEnter={e => { e.currentTarget.style.borderColor = '#EF4444'; e.currentTarget.style.color = '#EF4444'; }}
          onMouseLeave={e => { e.currentTarget.style.borderColor = '#2D3748'; e.currentTarget.style.color = '#94A3B8'; }}
        >
          Logout
        </button>
      </div>
    </header>
  );
}
