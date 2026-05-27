import axios from 'axios';

// Create an Axios instance with base URL for the Spring Boot backend
const API_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Auth Service wrapper
export const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    if (response.data.accessToken) {
      localStorage.setItem('token', response.data.accessToken);
    }
    return response.data;
  },
  register: async (fullName, email, password) => {
    return await api.post('/auth/register', { fullName, email, password });
  },
  logout: () => {
    localStorage.removeItem('token');
  }
};

// Debt Service wrapper
export const debtService = {
  getContacts: async () => {
    return await api.get('/debts/contacts');
  },
  getDebtRecords: async () => {
    return await api.get('/debts');
  },
  getRemainingSum: async (direction) => {
    return await api.get(`/debts/remaining-sum?direction=${direction}`);
  },
  getOverdue: async () => {
    return await api.get('/debts/overdue');
  }
};

// Budget Service wrapper
export const budgetService = {
  getBudgets: async () => {
    return await api.get('/budgets');
  }
};

export default api;
