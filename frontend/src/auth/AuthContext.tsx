import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';

interface AuthUser {
  username: string;
  fullName: string;
  tenantId: string;
  roles: string[];
}

interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(localStorage.getItem('jwt_token'));
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = localStorage.getItem('jwt_user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = async (username: string, password: string) => {
    const resp = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });

    if (!resp.ok) {
      const data = await resp.json().catch(() => ({}));
      throw new Error(data.error || 'Грешка при вход.');
    }

    const data = await resp.json();
    const authUser: AuthUser = {
      username: data.username,
      fullName: data.fullName,
      tenantId: data.tenantId,
      roles: data.roles || [],
    };

    localStorage.setItem('jwt_token', data.token);
    localStorage.setItem('jwt_user', JSON.stringify(authUser));
    setToken(data.token);
    setUser(authUser);
  };

  const logout = () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('jwt_user');
    setToken(null);
    setUser(null);
  };

  const hasRole = (role: string) => user?.roles?.includes(role) ?? false;

  // Validate token on mount
  useEffect(() => {
    if (!token) return;
    fetch('/api/auth/me', {
      headers: { 'Authorization': `Bearer ${token}` },
    }).then(resp => {
      if (!resp.ok) logout();
    }).catch(() => logout());
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token && !!user, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
