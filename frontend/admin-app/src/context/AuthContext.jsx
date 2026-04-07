import { createContext, useContext, useState } from 'react';
import { login as apiLogin } from '../api/adminApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('admin_token'));
  const [username, setUsername] = useState(() => localStorage.getItem('admin_user'));

  async function signIn(user, pass) {
    const res = await apiLogin(user, pass);
    const { token: t, username: u } = res.data;
    localStorage.setItem('admin_token', t);
    localStorage.setItem('admin_user', u);
    setToken(t);
    setUsername(u);
    return true;
  }

  function signOut() {
    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_user');
    setToken(null);
    setUsername(null);
  }

  return (
    <AuthContext.Provider value={{ token, username, signIn, signOut, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);