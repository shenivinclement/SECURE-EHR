import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import { getRecords, getHospitals } from '../api/api';
import Header from '../components/Header';
import Spinner from '../components/Spinner';

// Fix broken marker images in Vite/webpack bundlers
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

// Custom teal/green circular marker with rank number and a downward pointer
function makeGreenIcon(rank, isTop) {
  const bg = isTop ? '#0D9488' : '#14B8A6';
  return L.divIcon({
    className: '',
    iconSize: [32, 40],
    iconAnchor: [16, 40],
    popupAnchor: [0, -42],
    html: `
      <div style="
        width:32px;height:32px;
        background:${bg};
        border-radius:50%;
        border:3px solid #fff;
        box-shadow:0 2px 10px rgba(0,0,0,0.45);
        display:flex;align-items:center;justify-content:center;
        color:#fff;font-size:12px;font-weight:800;
        font-family:system-ui,sans-serif;
        position:relative;
      ">
        ${rank}
        <div style="
          position:absolute;bottom:-8px;left:50%;
          transform:translateX(-50%);
          width:0;height:0;
          border-left:6px solid transparent;
          border-right:6px solid transparent;
          border-top:9px solid ${bg};
        "></div>
      </div>`,
  });
}

const CONDITION_CHIPS = ['All', 'Diabetes', 'Hypertension', 'Asthma', 'Arthritis', 'Cancer', 'Obesity'];
// Default map center — hospitals in the demo dataset use placeholder coordinates,
// not tied to any specific real city.
const DEFAULT_CENTER = [20.20, 78.20];
const LAT = 20.20;
const LON = 78.20;

/* ── sub-components ──────────────────────────────────────────── */

function StarRating({ rating }) {
  const full = Math.floor(rating);
  const half = rating % 1 >= 0.5;
  return (
    <span style={{ color: '#FBBF24', fontSize: '14px', letterSpacing: '1px' }}>
      {'★'.repeat(full)}{half ? '½' : ''}{'☆'.repeat(5 - full - (half ? 1 : 0))}
      <span style={{ color: '#94A3B8', fontSize: '13px', marginLeft: '6px' }}>{rating?.toFixed(1)}</span>
    </span>
  );
}

function CureBar({ rate }) {
  const pct = Math.round((rate || 0) * 100);
  const color = pct >= 85 ? '#14B8A6' : pct >= 70 ? '#FBBF24' : '#F87171';
  return (
    <div style={{ marginTop: '10px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
        <span style={{ color: '#94A3B8', fontSize: '12px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>Cure Rate</span>
        <span style={{ color, fontSize: '13px', fontWeight: 700 }}>{pct}%</span>
      </div>
      <div style={{ height: 6, background: '#0A0E1A', borderRadius: 3, overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${pct}%`, background: color, borderRadius: 3, transition: 'width 0.6s ease' }} />
      </div>
    </div>
  );
}

function HospitalCard({ hospital, rank }) {
  const isTop = rank === 1;
  return (
    <div style={{
      background: isTop ? 'linear-gradient(135deg, #1A1F2E, #1E2840)' : '#1A1F2E',
      border: `1px solid ${isTop ? 'rgba(20,184,166,0.4)' : '#1E2435'}`,
      borderRadius: '14px',
      padding: '22px 24px',
      marginBottom: '12px',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {isTop && (
        <div style={{
          position: 'absolute', top: 0, right: 0,
          background: 'linear-gradient(135deg, #14B8A6, #0D9488)',
          color: '#fff', fontSize: '11px', fontWeight: 700,
          padding: '6px 16px', borderBottomLeftRadius: '10px', letterSpacing: '0.5px',
        }}>✦ AI PICK</div>
      )}
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '18px' }}>
        <div style={{
          width: 44, height: 44, flexShrink: 0,
          background: isTop ? 'linear-gradient(135deg, #14B8A6, #0D9488)' : '#0A0E1A',
          border: isTop ? 'none' : '1px solid #2D3748',
          borderRadius: '12px',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: isTop ? '#fff' : '#94A3B8', fontSize: '18px', fontWeight: 800,
        }}>#{rank}</div>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
            <h3 style={{ color: '#fff', fontSize: '17px', fontWeight: 700, margin: 0 }}>{hospital.name}</h3>
            <span style={{
              background: 'rgba(129,140,248,0.15)', color: '#818CF8',
              fontSize: '11px', fontWeight: 700, padding: '3px 10px', borderRadius: '20px',
            }}>{hospital.specialty}</span>
          </div>
          <div style={{ color: '#94A3B8', fontSize: '13px', marginTop: '6px' }}>📍 {hospital.address}</div>
          <div style={{ marginTop: '10px' }}><StarRating rating={hospital.rating} /></div>
          <CureBar rate={hospital.cure_rate} />
        </div>
      </div>
    </div>
  );
}

/* ── Map panel ───────────────────────────────────────────────── */

// Leaflet popup content is plain HTML — keep it simple & readable
function popupHtml(h, rank) {
  const pct = Math.round((h.cure_rate || 0) * 100);
  return `
    <div style="font-family:system-ui,sans-serif;min-width:180px;max-width:220px;">
      <div style="font-weight:800;font-size:14px;color:#0D9488;margin-bottom:4px;">
        #${rank} ${h.name}
      </div>
      <div style="font-size:12px;color:#555;margin-bottom:2px;">${h.specialty || ''}</div>
      <div style="font-size:13px;font-weight:700;color:#0D9488;">${pct}% cure rate</div>
    </div>`;
}

function HospitalMap({ hospitals }) {
  // Only render markers for hospitals that have valid coordinates
  const mapped = hospitals
    .map((h, i) => ({ ...h, rank: i + 1 }))
    .filter(h => h.latitude != null && h.longitude != null);

  return (
    <div style={{
      borderRadius: '14px',
      overflow: 'hidden',
      border: '1px solid #1E2435',
      marginBottom: '28px',
      height: '350px',
      // Leaflet needs a positioned parent to size itself
      position: 'relative',
    }}>
      <MapContainer
        center={DEFAULT_CENTER}
        zoom={11}
        style={{ height: '100%', width: '100%' }}
        scrollWheelZoom={false}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {mapped.map(h => (
          <Marker
            key={`${h.name}-${h.rank}`}
            position={[h.latitude, h.longitude]}
            icon={makeGreenIcon(h.rank, h.rank === 1)}
          >
            <Popup>
              <div dangerouslySetInnerHTML={{ __html: popupHtml(h, h.rank) }} />
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  );
}

/* ── Page ────────────────────────────────────────────────────── */

export default function HospitalFinder() {
  const [hospitals, setHospitals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [primaryCondition, setPrimaryCondition] = useState('');
  const [selected, setSelected] = useState('');
  const navigate = useNavigate();

  // Derive primary condition from the patient's most-frequent diagnosis
  useEffect(() => {
    getRecords()
      .then(r => {
        const records = r.data;
        if (records.length > 0) {
          const freq = {};
          records.forEach(rec => {
            if (rec.diagnosis) freq[rec.diagnosis] = (freq[rec.diagnosis] || 0) + 1;
          });
          const top = Object.entries(freq).sort((a, b) => b[1] - a[1])[0]?.[0] || '';
          const matched = CONDITION_CHIPS.find(c =>
            c !== 'All' && top.toLowerCase().includes(c.toLowerCase())
          ) || top;
          setPrimaryCondition(matched);
          setSelected(matched);
        }
      })
      .catch(() => {});
  }, []);

  // Re-fetch whenever selected chip changes (waits for primaryCondition init)
  useEffect(() => {
    if (primaryCondition === '' && selected === '') return;
    fetchHospitals(selected);
  }, [selected, primaryCondition]);

  const fetchHospitals = async (condition) => {
    setLoading(true);
    try {
      const params = { lat: LAT, lon: LON };
      if (condition && condition !== 'All') params.condition = condition;
      const r = await getHospitals(params);
      const sorted = [...(r.data || [])].sort((a, b) => (b.cure_rate || 0) - (a.cure_rate || 0));
      setHospitals(sorted);
    } catch (err) {
      console.error('Hospital fetch failed:', err?.response?.data || err.message);
      setHospitals([]);
    } finally {
      setLoading(false);
    }
  };

  const displayCondition = selected && selected !== 'All' ? selected : 'All Conditions';

  return (
    <div style={{ minHeight: '100vh', background: '#0A0E1A' }}>
      <Header backTo="/dashboard" backLabel="Dashboard" />
      <main style={{ maxWidth: '900px', margin: '0 auto', padding: '40px 32px' }}>

        {/* Title */}
        <div style={{ marginBottom: '28px' }}>
          <h1 style={{ color: '#fff', fontSize: '28px', fontWeight: 800, margin: '0 0 8px' }}>
            Hospital Finder
          </h1>
          <p style={{ color: '#94A3B8', fontSize: '15px', margin: 0 }}>
            AI-ranked hospitals near you · sorted by cure rate
          </p>
        </div>

        {/* Condition filter chips */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '28px' }}>
          {CONDITION_CHIPS.map(chip => {
            const isActive = selected === chip || (chip === 'All' && (!selected || selected === 'All'));
            return (
              <button
                key={chip}
                onClick={() => setSelected(chip)}
                style={{
                  background: isActive ? 'linear-gradient(135deg, #14B8A6, #0D9488)' : '#1A1F2E',
                  border: `1px solid ${isActive ? 'transparent' : '#2D3748'}`,
                  color: isActive ? '#fff' : '#94A3B8',
                  padding: '8px 18px', borderRadius: '20px',
                  cursor: 'pointer', fontSize: '13px',
                  fontWeight: isActive ? 700 : 500, transition: 'all 0.2s',
                }}
                onMouseEnter={e => { if (!isActive) { e.currentTarget.style.borderColor = '#14B8A6'; e.currentTarget.style.color = '#14B8A6'; } }}
                onMouseLeave={e => { if (!isActive) { e.currentTarget.style.borderColor = '#2D3748'; e.currentTarget.style.color = '#94A3B8'; } }}
              >
                {chip}
              </button>
            );
          })}
        </div>

        {loading ? (
          <Spinner />
        ) : hospitals.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '80px 0', color: '#94A3B8' }}>
            No hospitals found for this condition.
          </div>
        ) : (
          <>
            {/* ── MAP (above the list) ── */}
            <HospitalMap hospitals={hospitals} />

            {/* Section label */}
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              marginBottom: '16px',
            }}>
              <h2 style={{ color: '#94A3B8', fontSize: '13px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '1px' }}>
                AI-Ranked for {displayCondition}
              </h2>
              <span style={{ color: '#4B5563', fontSize: '13px' }}>{hospitals.length} hospitals found</span>
            </div>

            {/* ── RANKED LIST (below the map) ── */}
            {hospitals.map((h, i) => (
              <HospitalCard key={h.name + i} hospital={h} rank={i + 1} />
            ))}
          </>
        )}
      </main>
    </div>
  );
}
