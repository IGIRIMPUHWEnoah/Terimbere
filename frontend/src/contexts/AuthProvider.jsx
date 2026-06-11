import { useState } from 'react';
import { authService } from '../services/api';
import { SessionContext } from './sessionContext';

const buildInitialUser = () => {
  const token = localStorage.getItem('token');
  if (!token) return null;
  const stored = authService.getStoredUser();
  return stored ? { ...stored, token: stored.token || token } : { token };
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(buildInitialUser);

  const login = async (email, password) => {
    try {
      const data = await authService.login(email, password);
      const profile = {
        token: data.accessToken,
        userId: data.userId,
        email: data.email,
        fullName: data.fullName,
        currencyCode: data.currencyCode,
        timezone: data.timezone
      };
      setUser(profile);
      return { success: true };
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || 'Login failed. Please check your credentials.'
      };
    }
  };

  const register = async (fullName, email, password) => {
    try {
      await authService.register(fullName, email, password);
      return { success: true };
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || 'Registration failed.'
      };
    }
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  return (
    <SessionContext.Provider value={{ user, login, register, logout }}>
      {children}
    </SessionContext.Provider>
  );
};
