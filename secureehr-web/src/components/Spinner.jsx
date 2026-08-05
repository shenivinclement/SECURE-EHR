export default function Spinner({ size = 40 }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '48px' }}>
      <div
        style={{
          width: size,
          height: size,
          border: '3px solid #1E2435',
          borderTop: '3px solid #14B8A6',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
        }}
      />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
