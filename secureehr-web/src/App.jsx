import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Login from './pages/Login';
import PatientDashboard from './pages/PatientDashboard';
import MedicalRecords from './pages/MedicalRecords';
import ConsentManager from './pages/ConsentManager';
import DoctorDashboard from './pages/DoctorDashboard';
import PatientDetail from './pages/PatientDetail';
import HospitalFinder from './pages/HospitalFinder';
import Visits from './pages/Visits';
import DoctorPatients from './pages/DoctorPatients';
import AiChat from './pages/AiChat';
import Profile from './pages/Profile';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/dashboard" element={<PatientDashboard />} />
          <Route path="/records" element={<MedicalRecords />} />
          <Route path="/consent" element={<ConsentManager />} />
          <Route path="/doctor" element={<DoctorDashboard />} />
          <Route path="/doctor/patient/:id" element={<PatientDetail />} />
          <Route path="/hospitals" element={<HospitalFinder />} />
          <Route path="/visits" element={<Visits />} />
          <Route path="/doctor/patients" element={<DoctorPatients />} />
          <Route path="/chat" element={<AiChat />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
