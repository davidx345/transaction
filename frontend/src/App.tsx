import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Link, useLocation, Navigate, Outlet } from 'react-router-dom';
import { DisputeList } from './pages/DisputeList';
import { DisputeDetail } from './pages/DisputeDetail';
import { CSVUpload } from './pages/CSVUpload';
import { ReconciliationDashboard } from './pages/ReconciliationDashboard';
import { TransactionComparison } from './pages/TransactionComparison';
import { WebhookMonitor } from './pages/WebhookMonitor';
import { MetricsDashboard } from './pages/MetricsDashboard';
import { LandingPage } from './pages/LandingPage';
import { Login } from './pages/Login';
import ReportsPage from './pages/ReportsPage';
import { AuthProvider, useAuth } from './context/AuthContext';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<Login />} />
          
          <Route element={<RequireAuth><DashboardLayout /></RequireAuth>}>
            <Route path="/dashboard" element={<ReconciliationDashboard />} />
            <Route path="/upload" element={<CSVUpload />} />
            <Route path="/disputes" element={<DisputeList />} />
            <Route path="/disputes/:id" element={<DisputeDetail />} />
            <Route path="/transactions" element={<TransactionComparison />} />
            <Route path="/webhooks" element={<WebhookMonitor />} />
            <Route path="/metrics" element={<MetricsDashboard />} />
            <Route path="/reports" element={<ReportsPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}

function DashboardLayout() {
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const { logout, user } = useAuth();

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: '▣' },
    { path: '/upload', label: 'CSV Upload', icon: '↑' },
    { path: '/disputes', label: 'Disputes', icon: '!' },
    { path: '/transactions', label: 'Transactions', icon: '≡' },
    { path: '/reports', label: 'Reports', icon: '📊' },
    { path: '/webhooks', label: 'Webhooks', icon: '⚡' },
    { path: '/metrics', label: 'Metrics', icon: '▲' },
  ];

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#0A0A0B' }}>
      {/* Sidebar */}
      <aside style={{
        width: sidebarOpen ? '240px' : '64px',
        background: '#111113',
        borderRight: '1px solid #27272A',
        transition: 'width 0.2s ease',
        position: 'sticky',
        top: 0,
        height: '100vh',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column'
      }}>
        <div style={{
          padding: '1.25rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #27272A'
        }}>
          {sidebarOpen && (
            <h2 style={{ 
              margin: 0, 
              fontSize: '1rem', 
              fontWeight: 600,
              color: '#FAFAFA',
              letterSpacing: '-0.01em'
            }}>
              Reconciliation
            </h2>
          )}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              fontSize: '1rem',
              padding: '0.25rem',
              color: '#71717A'
            }}
          >
            {sidebarOpen ? '◀' : '▶'}
          </button>
        </div>

        <nav style={{ padding: '0.75rem', flex: 1 }}>
          {navItems.map(item => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  padding: '0.625rem 0.875rem',
                  marginBottom: '0.25rem',
                  borderRadius: '6px',
                  textDecoration: 'none',
                  color: isActive ? '#FAFAFA' : '#A1A1AA',
                  background: isActive ? '#27272A' : 'transparent',
                  fontWeight: isActive ? 500 : 400,
                  fontSize: '0.875rem',
                  transition: 'all 0.15s ease'
                }}
                onMouseEnter={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.background = '#18181B';
                    e.currentTarget.style.color = '#FAFAFA';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.background = 'transparent';
                    e.currentTarget.style.color = '#A1A1AA';
                  }
                }}
              >
                <span style={{ fontSize: '1rem', opacity: 0.8 }}>{item.icon}</span>
                {sidebarOpen && <span>{item.label}</span>}
              </Link>
            );
          })}
        </nav>

        <div style={{ padding: '0.75rem', borderTop: '1px solid #27272A' }}>
          {sidebarOpen && (
            <div style={{ marginBottom: '0.75rem', padding: '0 0.875rem' }}>
              <div style={{ fontSize: '0.8125rem', fontWeight: 500, color: '#FAFAFA' }}>{user?.email}</div>
              <div style={{ fontSize: '0.6875rem', color: '#71717A', marginTop: '0.125rem' }}>{user?.role}</div>
            </div>
          )}
          <button
            onClick={logout}
            style={{
              width: '100%',
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              padding: '0.625rem 0.875rem',
              borderRadius: '6px',
              border: 'none',
              background: 'transparent',
              color: '#EF4444',
              cursor: 'pointer',
              fontWeight: 400,
              fontSize: '0.875rem'
            }}
          >
            <span>🚪</span>
            {sidebarOpen && <span>Logout</span>}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main style={{ 
        flex: 1, 
        background: '#0A0A0B',
        minHeight: '100vh',
        overflow: 'auto'
      }}>
        <Outlet />
      </main>
    </div>
  );
}

export default App;
