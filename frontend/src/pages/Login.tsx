import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError('Invalid email or password');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={{ 
      minHeight: '100vh', 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center', 
      background: '#0A0A0B',
      padding: '1rem'
    }}>
      <div style={{ 
        width: '100%', 
        maxWidth: '380px', 
        padding: '2rem',
        background: '#111113',
        borderRadius: '12px',
        border: '1px solid #27272A'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h1 style={{ fontSize: '1.5rem', marginBottom: '0.5rem', color: '#FAFAFA', fontWeight: 600 }}>Welcome Back</h1>
          <p style={{ color: '#71717A', fontSize: '0.875rem' }}>Sign in to your account</p>
        </div>

        {error && (
          <div style={{ 
            background: 'rgba(239, 68, 68, 0.1)', 
            color: '#F87171', 
            padding: '0.75rem', 
            borderRadius: '6px',
            marginBottom: '1.5rem',
            fontSize: '0.8125rem',
            textAlign: 'center',
            border: '1px solid rgba(239, 68, 68, 0.2)'
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500, color: '#A1A1AA', fontSize: '0.8125rem' }}>Email</label>
            <input 
              type="email" 
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@company.com"
              required 
            />
          </div>
          
          <div className="mb-4">
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500, color: '#A1A1AA', fontSize: '0.8125rem' }}>Password</label>
            <input 
              type="password" 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required 
            />
          </div>

          <button 
            type="submit" 
            className="btn btn-primary" 
            style={{ width: '100%' }}
            disabled={isLoading}
          >
            {isLoading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.8125rem' }}>
          <p style={{ color: '#71717A' }}>
            Don't have an account? <a href="#" style={{ color: '#3B82F6', textDecoration: 'none', fontWeight: 500 }}>Contact Admin</a>
          </p>
        </div>
      </div>
    </div>
  );
};
