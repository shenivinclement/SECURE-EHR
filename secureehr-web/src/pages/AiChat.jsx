import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { aiChat } from '../api/api';
import Header from '../components/Header';

function Message({ msg }) {
  const isUser = msg.role === 'user';
  return (
    <div style={{
      display: 'flex',
      justifyContent: isUser ? 'flex-end' : 'flex-start',
      marginBottom: '16px',
    }}>
      {!isUser && (
        <div style={{
          width: 32, height: 32, flexShrink: 0,
          background: 'linear-gradient(135deg, #14B8A6, #0D9488)',
          borderRadius: '50%',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: '14px', marginRight: '10px', alignSelf: 'flex-end',
        }}>🤖</div>
      )}
      <div style={{
        maxWidth: '72%',
        background: isUser
          ? 'linear-gradient(135deg, #14B8A6, #0D9488)'
          : '#1E2435',
        color: '#fff',
        padding: '12px 16px',
        borderRadius: isUser ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
        fontSize: '15px',
        lineHeight: 1.6,
        border: isUser ? 'none' : '1px solid #2D3748',
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
      }}>
        {msg.content}
        {msg.source && (
          <div style={{ fontSize: '11px', opacity: 0.6, marginTop: '6px' }}>
            via {msg.source}
          </div>
        )}
      </div>
      {isUser && (
        <div style={{
          width: 32, height: 32, flexShrink: 0,
          background: '#2D3748',
          borderRadius: '50%',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: '14px', marginLeft: '10px', alignSelf: 'flex-end',
        }}>👤</div>
      )}
    </div>
  );
}

function TypingIndicator() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
      <div style={{
        width: 32, height: 32,
        background: 'linear-gradient(135deg, #14B8A6, #0D9488)',
        borderRadius: '50%',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: '14px',
      }}>🤖</div>
      <div style={{
        background: '#1E2435', border: '1px solid #2D3748',
        padding: '12px 16px', borderRadius: '18px 18px 18px 4px',
        display: 'flex', gap: '5px', alignItems: 'center',
      }}>
        {[0, 1, 2].map(i => (
          <div key={i} style={{
            width: 7, height: 7, background: '#14B8A6', borderRadius: '50%',
            animation: `bounce 1.2s ease-in-out ${i * 0.2}s infinite`,
          }} />
        ))}
        <style>{`
          @keyframes bounce {
            0%, 60%, 100% { transform: translateY(0); }
            30% { transform: translateY(-6px); }
          }
        `}</style>
      </div>
    </div>
  );
}

export default function AiChat() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const isDoctor = user?.role === 'doctor';

  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: `Hello${user?.name ? `, ${user.name.split(' ')[0]}` : ''}! I'm your SecureEHR AI assistant. I can help you understand health data, explain medical terms, and answer general health questions. How can I help you today?`,
      source: null,
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);

  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;

    const userMsg = { role: 'user', content: text };
    const conversationHistory = messages.map(m => ({ role: m.role, content: m.content }));
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const res = await aiChat(text, conversationHistory);
      const reply = res.data?.response || res.data?.message || String(res.data);
      setMessages(prev => [...prev, { role: 'assistant', content: reply, source: 'SecureEHR AI' }]);
    } catch (err) {
      console.error('AI chat failed:', err?.response?.data || err.message);
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'AI service unavailable. Please try again later.',
        source: null,
      }]);
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  const backTo = isDoctor ? '/doctor' : '/dashboard';

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A', display: 'flex', flexDirection: 'column' }}>
      <Header backTo={backTo} backLabel="Dashboard" />

      <main style={{ flex: 1, maxWidth: '820px', width: '100%', margin: '0 auto', padding: '24px 32px', display: 'flex', flexDirection: 'column', boxSizing: 'border-box' }}>
        {/* Page title */}
        <div style={{ marginBottom: '20px' }}>
          <h1 style={{ color: '#fff', fontSize: '24px', fontWeight: 800, margin: '0 0 4px' }}>AI Medical Assistant</h1>
          <p style={{ color: '#94A3B8', fontSize: '14px', margin: 0 }}>Powered by SecureEHR AI · Always consult a doctor for medical decisions</p>
        </div>

        {/* Message list */}
        <div style={{
          flex: 1,
          background: '#0D1117',
          border: '1px solid #1E2435',
          borderRadius: '14px',
          padding: '20px',
          overflowY: 'auto',
          minHeight: '380px',
          maxHeight: '520px',
          marginBottom: '16px',
        }}>
          {messages.map((m, i) => <Message key={i} msg={m} />)}
          {loading && <TypingIndicator />}
          <div ref={messagesEndRef} />
        </div>

        {/* Input row */}
        <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-end' }}>
          <textarea
            ref={inputRef}
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask a health question… (Enter to send, Shift+Enter for newline)"
            rows={2}
            style={{
              flex: 1,
              background: '#1A1F2E', border: '1px solid #2D3748',
              color: '#fff', padding: '12px 16px',
              borderRadius: '12px', fontSize: '15px',
              outline: 'none', resize: 'none',
              fontFamily: 'inherit', lineHeight: 1.5,
              transition: 'border-color 0.2s',
            }}
            onFocus={e => e.target.style.borderColor = '#14B8A6'}
            onBlur={e => e.target.style.borderColor = '#2D3748'}
          />
          <button
            onClick={send}
            disabled={loading || !input.trim()}
            style={{
              background: loading || !input.trim()
                ? '#1A1F2E'
                : 'linear-gradient(135deg, #14B8A6, #0D9488)',
              color: loading || !input.trim() ? '#4B5563' : '#fff',
              border: '1px solid #2D3748',
              padding: '12px 24px', borderRadius: '12px',
              cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
              fontSize: '15px', fontWeight: 700,
              transition: 'all 0.2s',
              whiteSpace: 'nowrap',
            }}
          >
            {loading ? '…' : 'Send ↑'}
          </button>
        </div>
        <div style={{ color: '#374151', fontSize: '12px', marginTop: '8px', textAlign: 'center' }}>
          Not a substitute for professional medical advice.
        </div>
      </main>
    </div>
  );
}
