import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { AxiosError } from 'axios';

type AuthMode = 'login' | 'signup';

interface ApiError {
  message?: string;
  error?: string;
}

// Eye icon for password visibility toggle
const EyeIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
    <circle cx="12" cy="12" r="3"></circle>
  </svg>
);

const EyeOffIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
    <line x1="1" y1="1" x2="23" y2="23"></line>
  </svg>
);

export const Login: React.FC = () => {
  const [mode, setMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [username, setUsername] = useState('');
  const [fullName, setFullName] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const { login, register } = useAuth();
  const navigate = useNavigate();

  // Password validation helpers
  const hasMinLength = password.length >= 8;
  const hasUppercase = /[A-Z]/.test(password);
  const hasLowercase = /[a-z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(password);
  const isPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasNumber && hasSpecialChar;

  const resetForm = () => {
    setEmail('');
    setPassword('');
    setUsername('');
    setFullName('');
    setConfirmPassword('');
    setError('');
    setShowPassword(false);
    setShowConfirmPassword(false);
  };

  const switchMode = (newMode: AuthMode) => {
    resetForm();
    setMode(newMode);
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login({ email, password });
      navigate('/dashboard');
    } catch (err) {
      const axiosError = err as AxiosError<ApiError>;
      const message = axiosError.response?.data?.message || 
                     axiosError.response?.data?.error || 
                     'Invalid email or password';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // Validation
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (!isPasswordValid) {
      setError('Password does not meet all requirements');
      return;
    }

    if (username.length < 3) {
      setError('Username must be at least 3 characters');
      return;
    }

    setIsLoading(true);

    try {
      await register({ 
        username, 
        email, 
        password,
        fullName: fullName || undefined
      });
      navigate('/dashboard');
    } catch (err) {
      const axiosError = err as AxiosError<ApiError>;
      const message = axiosError.response?.data?.message || 
                     axiosError.response?.data?.error || 
                     'Registration failed. Please try again.';
      setError(message);
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
        maxWidth: '400px', 
        padding: '2rem',
        background: '#111113',
        borderRadius: '12px',
        border: '1px solid #27272A'
      }}>
        {/* Logo/Brand */}
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <Link to="/" style={{ textDecoration: 'none' }}>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color: '#3B82F6' }}>Recon</span>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color: '#FAFAFA' }}>Engine</span>
          </Link>
        </div>

        {/* Tab Switcher */}
        <div style={{ 
          display: 'flex', 
          marginBottom: '1.5rem',
          background: '#18181B',
          borderRadius: '8px',
          padding: '4px'
        }}>
          <button
            type="button"
            onClick={() => switchMode('login')}
            style={{
              flex: 1,
              padding: '0.625rem 1rem',
              border: 'none',
              borderRadius: '6px',
              background: mode === 'login' ? '#27272A' : 'transparent',
              color: mode === 'login' ? '#FAFAFA' : '#71717A',
              fontWeight: 500,
              fontSize: '0.875rem',
              cursor: 'pointer',
              transition: 'all 0.2s'
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => switchMode('signup')}
            style={{
              flex: 1,
              padding: '0.625rem 1rem',
              border: 'none',
              borderRadius: '6px',
              background: mode === 'signup' ? '#27272A' : 'transparent',
              color: mode === 'signup' ? '#FAFAFA' : '#71717A',
              fontWeight: 500,
              fontSize: '0.875rem',
              cursor: 'pointer',
              transition: 'all 0.2s'
            }}
          >
            Sign Up
          </button>
        </div>

        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <h1 style={{ fontSize: '1.25rem', marginBottom: '0.5rem', color: '#FAFAFA', fontWeight: 600 }}>
            {mode === 'login' ? 'Welcome Back' : 'Create Account'}
          </h1>
          <p style={{ color: '#71717A', fontSize: '0.875rem' }}>
            {mode === 'login' ? 'Sign in to your account' : 'Get started with ReconEngine'}
          </p>
        </div>

        {/* Error Message */}
        {error && (
          <div style={{ 
            background: 'rgba(239, 68, 68, 0.1)', 
            color: '#F87171', 
            padding: '0.75rem', 
            borderRadius: '6px',
            marginBottom: '1rem',
            fontSize: '0.8125rem',
            textAlign: 'center',
            border: '1px solid rgba(239, 68, 68, 0.2)'
          }}>
            {error}
          </div>
        )}

        {/* Login Form */}
        {mode === 'login' && (
          <form onSubmit={handleLogin}>
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500, color: '#A1A1AA', fontSize: '0.8125rem' }}>
                Email
              </label>
              <input 
                type="email" 
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@company.com"
                required
                autoComplete="email"
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: '#18181B',
                  border: '1px solid #27272A',
                  borderRadius: '6px',
                  color: '#FAFAFA',
                  fontSize: '0.875rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>
            
            <div style={{ marginBottom: '1.5rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500, color: '#A1A1AA', fontSize: '0.8125rem' }}>
                Password
              </label>
              <div style={{ position: 'relative' }}>
                <input 
                  type={showPassword ? 'text' : 'password'} 
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  autoComplete="current-password"
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    paddingRight: '2.5rem',
                    background: '#18181B',
                    border: '1px solid #27272A',
                    borderRadius: '6px',
                    color: '#FAFAFA',
                    fontSize: '0.875rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '0.75rem',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'transparent',
                    border: 'none',
                    color: '#71717A',
                    cursor: 'pointer',
                    padding: '0',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
            </div>

            <button 
              type="submit" 
              disabled={isLoading}
              style={{
                width: '100%',
                padding: '0.75rem',
                background: isLoading ? '#1E40AF' : '#3B82F6',
                border: 'none',
                borderRadius: '6px',
                color: '#FAFAFA',
                fontWeight: 600,
                fontSize: '0.875rem',
                cursor: isLoading ? 'not-allowed' : 'pointer',
                transition: 'background 0.2s'
              }}
            >
              {isLoading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>
        )}

        {/* Signup Form */}
        {mode === 'signup' && (
          <form onSubmit={handleSignup}>
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500, color: '#A1A1AA', fontSize: '0.8125rem' }}>
                Username *
              </label>
              <input 
                type="text" 
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="johndoe"
                required
                minLength={3}
                autoComplete="username"
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: '#18181B',
                  border: '1px solid #27272A',
                  borderRadius: '6px',
                  color: '#FAFAFA',
                  fontSize: '0.875rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500, color: '#A1A1AA', fontSize: '0.8125rem' }}>
                Full Name
              </label>
              <input 
                type="text" 
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="John Doe"
                autoComplete="name"
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: '#18181B',
                  border: '1px solid #27272A',
                  borderRadius: '6px',
                  color: '#FAFAFA',
                  fontSize: '0.875rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500, color: '#A1A1AA', fontSize: '0.8125rem' }}>
                Email *
              </label>
              <input 
                type="email" 
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@company.com"
                required
                autoComplete="email"
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  background: '#18181B',
                  border: '1px solid #27272A',
                  borderRadius: '6px',
                  color: '#FAFAFA',
                  fontSize: '0.875rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>
            
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500, color: '#A1A1AA', fontSize: '0.8125rem' }}>
                Password *
              </label>
              <div style={{ position: 'relative' }}>
                <input 
                  type={showPassword ? 'text' : 'password'} 
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Create a strong password"
                  required
                  minLength={8}
                  autoComplete="new-password"
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    paddingRight: '2.5rem',
                    background: '#18181B',
                    border: `1px solid ${password && !isPasswordValid ? '#EF4444' : '#27272A'}`,
                    borderRadius: '6px',
                    color: '#FAFAFA',
                    fontSize: '0.875rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '0.75rem',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'transparent',
                    border: 'none',
                    color: '#71717A',
                    cursor: 'pointer',
                    padding: '0',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
              {/* Password Requirements */}
              {password && (
                <div style={{ marginTop: '0.5rem', fontSize: '0.75rem' }}>
                  <div style={{ color: hasMinLength ? '#22C55E' : '#71717A', display: 'flex', alignItems: 'center', gap: '0.25rem', marginBottom: '0.125rem' }}>
                    {hasMinLength ? '✓' : '○'} At least 8 characters
                  </div>
                  <div style={{ color: hasUppercase ? '#22C55E' : '#71717A', display: 'flex', alignItems: 'center', gap: '0.25rem', marginBottom: '0.125rem' }}>
                    {hasUppercase ? '✓' : '○'} One uppercase letter (A-Z)
                  </div>
                  <div style={{ color: hasLowercase ? '#22C55E' : '#71717A', display: 'flex', alignItems: 'center', gap: '0.25rem', marginBottom: '0.125rem' }}>
                    {hasLowercase ? '✓' : '○'} One lowercase letter (a-z)
                  </div>
                  <div style={{ color: hasNumber ? '#22C55E' : '#71717A', display: 'flex', alignItems: 'center', gap: '0.25rem', marginBottom: '0.125rem' }}>
                    {hasNumber ? '✓' : '○'} One number (0-9)
                  </div>
                  <div style={{ color: hasSpecialChar ? '#22C55E' : '#71717A', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                    {hasSpecialChar ? '✓' : '○'} One special character (!@#$%^&*)
                  </div>
                </div>
              )}
              {!password && (
                <div style={{ marginTop: '0.5rem', fontSize: '0.75rem', color: '#71717A' }}>
                  Password must contain uppercase, lowercase, number, and special character
                </div>
              )}
            </div>

            <div style={{ marginBottom: '1.5rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 500, color: '#A1A1AA', fontSize: '0.8125rem' }}>
                Confirm Password *
              </label>
              <div style={{ position: 'relative' }}>
                <input 
                  type={showConfirmPassword ? 'text' : 'password'} 
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  autoComplete="new-password"
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    paddingRight: '2.5rem',
                    background: '#18181B',
                    border: `1px solid ${confirmPassword && password !== confirmPassword ? '#EF4444' : '#27272A'}`,
                    borderRadius: '6px',
                    color: '#FAFAFA',
                    fontSize: '0.875rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  style={{
                    position: 'absolute',
                    right: '0.75rem',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'transparent',
                    border: 'none',
                    color: '#71717A',
                    cursor: 'pointer',
                    padding: '0',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  {showConfirmPassword ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
              {confirmPassword && password !== confirmPassword && (
                <div style={{ marginTop: '0.5rem', fontSize: '0.75rem', color: '#EF4444' }}>
                  Passwords do not match
                </div>
              )}
            </div>

            <button 
              type="submit" 
              disabled={isLoading}
              style={{
                width: '100%',
                padding: '0.75rem',
                background: isLoading ? '#1E40AF' : '#3B82F6',
                border: 'none',
                borderRadius: '6px',
                color: '#FAFAFA',
                fontWeight: 600,
                fontSize: '0.875rem',
                cursor: isLoading ? 'not-allowed' : 'pointer',
                transition: 'background 0.2s'
              }}
            >
              {isLoading ? 'Creating account...' : 'Create Account'}
            </button>
          </form>
        )}

        {/* Footer */}
        <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.8125rem' }}>
          <Link to="/" style={{ color: '#71717A', textDecoration: 'none' }}>
            ← Back to Home
          </Link>
        </div>
      </div>
    </div>
  );
};
