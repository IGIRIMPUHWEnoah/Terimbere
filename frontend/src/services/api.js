import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' }
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    if (response.data.accessToken) {
      localStorage.setItem('token', response.data.accessToken);
      const profile = {
        token: response.data.accessToken,
        userId: response.data.userId,
        email: response.data.email,
        fullName: response.data.fullName,
        currencyCode: response.data.currencyCode,
        timezone: response.data.timezone
      };
      localStorage.setItem('user', JSON.stringify(profile));
    }
    return response.data;
  },
  register: async (fullName, email, password) => {
    return api.post('/auth/register', { fullName, email, password });
  },
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },
  getStoredUser: () => {
    try {
      const raw = localStorage.getItem('user');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
};

export const budgetService = {
  getBudgets: () => api.get('/budgets'),
  getBudgetById: (budgetId) => api.get(`/budgets/${budgetId}`),
  createBudget: (payload) => api.post('/budgets', payload),
  updateBudget: (budgetId, payload) => api.put(`/budgets/${budgetId}`, payload),
  deleteBudget: (budgetId) => api.delete(`/budgets/${budgetId}`),
  addEntry: (budgetId, payload) => api.post(`/budgets/${budgetId}/entries`, payload),
  updateEntry: (budgetId, entryId, payload) => api.put(`/budgets/${budgetId}/entries/${entryId}`, payload),
  deleteEntry: (budgetId, entryId) => api.delete(`/budgets/${budgetId}/entries/${entryId}`)
};

export const debtService = {
  getContacts: () => api.get('/debts/contacts'),
  createContact: (payload) => api.post('/debts/contacts', payload),
  updateContact: (contactId, payload) => api.put(`/debts/contacts/${contactId}`, payload),
  deleteContact: (contactId) => api.delete(`/debts/contacts/${contactId}`),
  getDebtRecords: (page = 0, size = 10, sort = 'createdAt,desc') => api.get('/debts', { params: { page, size, sort } }),
  getDebtsByDirection: (direction, page = 0, size = 10, sort = 'createdAt,desc') => api.get('/debts/filter', { params: { direction, page, size, sort } }),
  createDebtRecord: (payload) => api.post('/debts', payload),
  updateDebtRecord: (debtId, payload) => api.put(`/debts/${debtId}`, payload),
  deleteDebtRecord: (debtId) => api.delete(`/debts/${debtId}`),
  getPayments: (debtId) => api.get(`/debts/${debtId}/payments`),
  recordPayment: (debtId, payload) => api.post(`/debts/${debtId}/payments`, payload),
  getRemainingSum: (direction) => api.get(`/debts/remaining-sum?direction=${direction}`),
  getOverdue: () => api.get('/debts/overdue')
};

export const billService = {
  getBills: () => api.get('/bills'),
  getBillById: (billId) => api.get(`/bills/${billId}`),
  createBill: (payload) => api.post('/bills', payload),
  updateBill: (billId, payload) => api.put(`/bills/${billId}`, payload),
  deleteBill: (billId) => api.delete(`/bills/${billId}`),
  payBill: (billId, payload) => api.post(`/bills/${billId}/pay`, payload)
};

export const incomePlanService = {
  getIncomePlans: () => api.get('/income-plans'),
  getIncomePlanById: (planId) => api.get(`/income-plans/${planId}`),
  createIncomePlan: (payload) => api.post('/income-plans', payload),
  updateIncomePlan: (planId, payload) => api.put(`/income-plans/${planId}`, payload),
  deleteIncomePlan: (planId) => api.delete(`/income-plans/${planId}`),
  addSourceToPlan: (planId, payload) => api.post(`/income-plans/${planId}/sources`, payload),
  updateIncomeSource: (planId, sourceId, payload) => api.put(`/income-plans/${planId}/sources/${sourceId}`, payload),
  deleteIncomeSource: (planId, sourceId) => api.delete(`/income-plans/${planId}/sources/${sourceId}`)
};

export const reportService = {
  downloadBudgetPdf: (budgetId) => api.get(`/reports/budgets/${budgetId}/pdf`, { responseType: 'blob' }),
  downloadBudgetExcel: (budgetId) => api.get(`/reports/budgets/${budgetId}/excel`, { responseType: 'blob' })
};

export default api;
