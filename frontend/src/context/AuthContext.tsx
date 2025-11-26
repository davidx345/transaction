import React, { createContext, useContext, useState, useEffect } from 'react';

interface User {
  email: string;
  role: 'ADMIN' | 'RECONCILER' | 'AUDITOR';
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    // Check for existing session
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
      setIsAuthenticated(true);
    }
  }, []);

  const login = async (email: string, password: string) => {
    // Mock authentication
    return new Promise<void>((resolve, reject) => {
      setTimeout(() => {
        if (email && password) {
          // Simulate different roles based on email
          let role: User['role'] = 'RECONCILER';
          if (email.includes('admin')) role = 'ADMIN';
          if (email.includes('audit')) role = 'AUDITOR';

          const newUser = { email, role };
          setUser(newUser);
          setIsAuthenticated(true);
          localStorage.setItem('user', JSON.stringify(newUser));
          resolve();
        } else {
          reject(new Error('Invalid credentials'));
        }
      }, 1000);
    });
  };

  const logout = () => {
    setUser(null);
    setIsAuthenticated(false);
    localStorage.removeItem('user');
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
