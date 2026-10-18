import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('drippay_user');
    return raw ? JSON.parse(raw) : null;
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('drippay_token');
    if (!token) {
      setLoading(false);
      return;
    }
    api
      .get('/users/me')
      .then((res) => {
        setUser(res.data);
        localStorage.setItem('drippay_user', JSON.stringify(res.data));
      })
      .catch(() => {
        localStorage.removeItem('drippay_token');
        localStorage.removeItem('drippay_refresh');
        localStorage.removeItem('drippay_user');
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      isAuthenticated: !!user,
      isAdmin: user?.role === 'ADMIN',
      async login(email, password) {
        const { data } = await api.post('/auth/login', { email, password });
        localStorage.setItem('drippay_token', data.accessToken);
        if (data.refreshToken) localStorage.setItem('drippay_refresh', data.refreshToken);
        localStorage.setItem('drippay_user', JSON.stringify(data.user));
        setUser(data.user);
        return data;
      },
      async register(payload) {
        const { data } = await api.post('/auth/register', payload);
        localStorage.setItem('drippay_token', data.accessToken);
        if (data.refreshToken) localStorage.setItem('drippay_refresh', data.refreshToken);
        localStorage.setItem('drippay_user', JSON.stringify(data.user));
        setUser(data.user);
        return data;
      },
      logout() {
        const refresh = localStorage.getItem('drippay_refresh');
        localStorage.removeItem('drippay_token');
        localStorage.removeItem('drippay_refresh');
        localStorage.removeItem('drippay_user');
        setUser(null);
        api.post('/auth/logout', refresh ? { refreshToken: refresh } : {}).catch(() => {});
      },
    }),
    [user, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
