import axios from 'axios';

const BASE = 'http://localhost:8000';

const client = axios.create({ baseURL: BASE });

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const login = (email, password) =>
  client.post('/auth/login', { email, password });

export const register = (name, email, password, role = 'patient') =>
  client.post('/auth/register', { name, email, password, role });

export const getMe = () => client.get('/auth/me');

export const getRecords = () => client.get('/records');

export const getConsents = () => client.get('/consent');

export const grantConsent = (body) => client.post('/consent/grant', body);

export const revokeConsent = (body) => client.post('/consent/revoke', body);

export const aiChat = (message, history = []) => client.post('/ai/chat', { message, history });

export const getHospitals = (params) => client.get('/hospitals', { params });

export const getVisits = () => client.get('/visits');

export const getPatients = () => client.get('/doctor/patients');

export const getPatientRecords = (id) =>
  client.get(`/doctor/patients/${id}/records`);

export const searchPatients = (q) =>
  client.get('/doctor/search', { params: { q } });

export const getDoctorConsents = () => client.get('/doctor/consents');
