import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, register, getMe } from '../api/api';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const { saveAuth } = useAuth();
  const navigate = useNavigate();

  const isRegister = mode === 'register';

  const finishLogin = async () => {
    const { data: tokenData } = await login(email, password);
    const token = tokenData.access_token;
    localStorage.setItem('token', token);
    const { data: me } = await getMe();
    saveAuth(token, me);
    navigate(me.role === 'doctor' ? '/doctor' : '/dashboard');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (isRegister && password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setLoading(true);
    try {
      if (isRegister) {
        await register(name, email, password, 'patient');
        setSuccess('Account created! Signing you in…');
      }
      await finishLogin();
    } catch (err) {
      setError(
        err?.response?.data?.detail ||
        (isRegister ? 'Registration failed. Please try again.' : 'Invalid credentials. Please try again.')
      );
    } finally {
      setLoading(false);
    }
  };

  const switchMode = () => {
    setMode(isRegister ? 'login' : 'register');
    setError('');
    setSuccess('');
  };

  return (
    <div style={{
      minHeight: '100vh',
      background: '#0A0E1A',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px',
    }}>
      <div style={{ width: '100%', maxWidth: '420px' }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: '40px' }}>
          <div style={{
            width: 64, height: 64,
            background: 'linear-gradient(135deg, #14B8A6, #0D9488)',
            borderRadius: '18px',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '28px', fontWeight: 800, color: '#fff',
            margin: '0 auto 16px',
            boxShadow: '0 8px 32px rgba(20,184,166,0.3)',
          }}>S</div>
          <h1 style={{ color: '#fff', fontSize: '28px', fontWeight: 800, margin: 0, letterSpacing: '-0.5px' }}>
            SecureEHR
          </h1>
          <p style={{ color: '#94A3B8', fontSize: '14px', marginTop: '8px', margin: '8px 0 0' }}>
            Blockchain-Powered Health Records
          </p>
        </div>

        {/* Card */}
        <div style={{
          background: '#1A1F2E',
          borderRadius: '16px',
          padding: '36px',
          border: '1px solid #1E2435',
          boxShadow: '0 24px 64px rgba(0,0,0,0.4)',
        }}>
          <h2 style={{ color: '#fff', fontSize: '20px', fontWeight: 700, margin: '0 0 8px' }}>
            {isRegister ? 'Create your account' : 'Sign in to your account'}
          </h2>
          <p style={{ color: '#94A3B8', fontSize: '14px', margin: '0 0 28px' }}>
            {isRegister
              ? 'Register a new patient account to access the portal'
              : 'Enter your credentials to access the portal'}
          </p>

          <form onSubmit={handleSubmit}>
            {isRegister && (
              <div style={{ marginBottom: '20px' }}>
                <label htmlFor="name" style={{ display: 'block', color: '#94A3B8', fontSize: '13px', fontWeight: 600, marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                  Full Name
                </label>
                <input
                  id="name"
                  name="name"
                  type="text"
                  value={name}
                  onChange={e => setName(e.target.value)}
                  required
                  placeholder="Jane Doe"
                  style={{
                    width: '100%', boxSizing: 'border-box',
                    background: '#0A0E1A', border: '1px solid #2D3748',
                    color: '#fff', padding: '12px 16px', borderRadius: '10px',
                    fontSize: '15px', outline: 'none', transition: 'border-color 0.2s',
                  }}
                  onFocus={e => e.target.style.borderColor = '#14B8A6'}
                  onBlur={e => e.target.style.borderColor = '#2D3748'}
                />
              </div>
            )}

            <div style={{ marginBottom: '20px' }}>
              <label htmlFor="email" style={{ display: 'block', color: '#94A3B8', fontSize: '13px', fontWeight: 600, marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Email Address
              </label>
              <input
                id="email"
                name="email"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                placeholder="you@example.com"
                style={{
                  width: '100%', boxSizing: 'border-box',
                  background: '#0A0E1A', border: '1px solid #2D3748',
                  color: '#fff', padding: '12px 16px', borderRadius: '10px',
                  fontSize: '15px', outline: 'none', transition: 'border-color 0.2s',
                }}
                onFocus={e => e.target.style.borderColor = '#14B8A6'}
                onBlur={e => e.target.style.borderColor = '#2D3748'}
              />
            </div>

            <div style={{ marginBottom: isRegister ? '20px' : '28px' }}>
              <label htmlFor="password" style={{ display: 'block', color: '#94A3B8', fontSize: '13px', fontWeight: 600, marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
                placeholder="••••••••"
                style={{
                  width: '100%', boxSizing: 'border-box',
                  background: '#0A0E1A', border: '1px solid #2D3748',
                  color: '#fff', padding: '12px 16px', borderRadius: '10px',
                  fontSize: '15px', outline: 'none', transition: 'border-color 0.2s',
                }}
                onFocus={e => e.target.style.borderColor = '#14B8A6'}
                onBlur={e => e.target.style.borderColor = '#2D3748'}
              />
            </div>

            {isRegister && (
              <div style={{ marginBottom: '28px' }}>
                <label htmlFor="confirmPassword" style={{ display: 'block', color: '#94A3B8', fontSize: '13px', fontWeight: 600, marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                  Confirm Password
                </label>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  value={confirmPassword}
                  onChange={e => setConfirmPassword(e.target.value)}
                  required
                  placeholder="••••••••"
                  style={{
                    width: '100%', boxSizing: 'border-box',
                    background: '#0A0E1A', border: '1px solid #2D3748',
                    color: '#fff', padding: '12px 16px', borderRadius: '10px',
                    fontSize: '15px', outline: 'none', transition: 'border-color 0.2s',
                  }}
                  onFocus={e => e.target.style.borderColor = '#14B8A6'}
                  onBlur={e => e.target.style.borderColor = '#2D3748'}
                />
              </div>
            )}

            {error && (
              <div style={{
                background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)',
                color: '#FCA5A5', padding: '12px 16px', borderRadius: '8px',
                fontSize: '14px', marginBottom: '20px',
              }}>
                {error}
              </div>
            )}

            {success && (
              <div style={{
                background: 'rgba(20,184,166,0.1)', border: '1px solid rgba(20,184,166,0.3)',
                color: '#5EEAD4', padding: '12px 16px', borderRadius: '8px',
                fontSize: '14px', marginBottom: '20px',
              }}>
                {success}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              style={{
                width: '100%',
                background: loading ? '#0D9488' : 'linear-gradient(135deg, #14B8A6, #0D9488)',
                color: '#fff', border: 'none', padding: '14px',
                borderRadius: '10px', fontSize: '16px', fontWeight: 700,
                cursor: loading ? 'not-allowed' : 'pointer',
                transition: 'opacity 0.2s, transform 0.1s',
                opacity: loading ? 0.7 : 1,
              }}
              onMouseEnter={e => { if (!loading) e.currentTarget.style.opacity = '0.9'; }}
              onMouseLeave={e => { e.currentTarget.style.opacity = loading ? '0.7' : '1'; }}
            >
              {loading
                ? (isRegister ? 'Creating account…' : 'Signing in…')
                : (isRegister ? 'Create Account' : 'Sign In')}
            </button>
          </form>

          {/* Register / Login toggle */}
          <p style={{ color: '#94A3B8', fontSize: '14px', textAlign: 'center', margin: '24px 0 0' }}>
            {isRegister ? 'Already have an account?' : "Don't have an account?"}{' '}
            <button
              type="button"
              onClick={switchMode}
              style={{
                background: 'none', border: 'none', color: '#14B8A6',
                fontSize: '14px', fontWeight: 700, cursor: 'pointer', padding: 0,
              }}
            >
              {isRegister ? 'Sign in' : 'Register'}
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
