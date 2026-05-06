import axios from 'axios';

const api = axios.create({ baseURL: '/api/admin' });

// Attach JWT to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// On 401/403 → clear token and redirect to login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.removeItem('admin_token');
      window.location.replace('/login');
    }
    return Promise.reject(err);
  }
);

export const login = (username, password) =>
  api.post('/login', { username, password });

export const getDashboard = () => api.get('/dashboard');

export const getUsers = (page = 0, size = 20) =>
  api.get('/users', { params: { page, size } });
export const approveUser = (id) => api.put(`/users/${id}/approve`);
export const rejectUser  = (id, reason) => api.put(`/users/${id}/reject`, null, { params: { reason } });
export const blockUser   = (id) => api.put(`/users/${id}/block`);
export const unblockUser = (id) => api.put(`/users/${id}/unblock`);
export const deleteUser  = (id) => api.delete(`/users/${id}`);

export const getJobs   = (page = 0, size = 20) =>
  api.get('/jobs', { params: { page, size } });
export const deleteJob = (id) => api.delete(`/jobs/${id}`);

export const getRecruiters      = (page = 0, size = 20) =>
  api.get('/recruiters', { params: { page, size } });
export const approveRecruiter   = (id) => api.put(`/recruiters/${id}/approve`);
export const rejectRecruiter    = (id) => api.put(`/recruiters/${id}/reject`);
export const suspendRecruiter   = (id) => api.put(`/recruiters/${id}/suspend`);

export const getReferrals      = (status = 'all') => api.get('/referrals', { params: { status } });
export const markReferralPaid  = (id) => api.put(`/referrals/${id}/mark-paid`);