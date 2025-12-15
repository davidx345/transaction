import React, { useState, useEffect } from 'react';
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
  const [sidebarOpen, setSidebarOpen] = useState<boolean>(() => {
    try {
      const raw = localStorage.getItem('sidebarOpen');
      if (raw === null) return true;
      return raw === 'true';
    } catch (e) {
      return true;
    }
  });

  const [isMobile, setIsMobile] = useState<boolean>(false);

  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth < 768;
      setIsMobile(mobile);

      if (mobile) {
        // always retract on mobile by default
        setSidebarOpen(false);
      } else {
        // on larger screens respect saved preference
        try {
          const raw = localStorage.getItem('sidebarOpen');
          if (raw === null) setSidebarOpen(true);
          else setSidebarOpen(raw === 'true');
        } catch (e) {
          setSidebarOpen(true);
        }
      }
    };

    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);
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
        // desktop: fixed width; mobile: slide in/out
        width: isMobile ? '240px' : (sidebarOpen ? '240px' : '64px'),
        background: '#111113',
        borderRight: '1px solid #27272A',
        transition: isMobile ? 'transform 300ms ease' : 'width 200ms ease',
        position: isMobile ? 'fixed' as const : 'sticky' as const,
        left: 0,
        top: 0,
        height: '100vh',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        zIndex: isMobile ? 70 : 'auto',
        transform: isMobile ? (sidebarOpen ? 'translateX(0%)' : 'translateX(-100%)') : 'none'
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
            onClick={() => {
              setSidebarOpen(prev => {
                const next = !prev;
                try { localStorage.setItem('sidebarOpen', String(next)); } catch (e) {}
                return next;
              });
            }}
            aria-label={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
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
              <div style={{ fontSize: '0.8125rem', fontWeight: 500, color: '#FAFAFA' }}>
                {user?.fullName || user?.username || user?.email}
              </div>
              <div style={{ fontSize: '0.6875rem', color: '#71717A', marginTop: '0.125rem' }}>
                {user?.roles?.[0] || 'User'}
              </div>
            </div>
          )}
          <button
            onClick={() => logout()}
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

      {isMobile && (
        <>
          {/* overlay */}
          {sidebarOpen && (
            <div
              onClick={() => setSidebarOpen(false)}
              style={{
                position: 'fixed',
                inset: 0,
                background: 'rgba(0,0,0,0.45)',
                zIndex: 60
              }}
            />
          )}

          {/* hamburger */}
          <button
            onClick={() => setSidebarOpen(s => {
              const next = !s;
              try { localStorage.setItem('sidebarOpen', String(next)); } catch (e) {}
              return next;
            })}
            aria-label="Toggle menu"
            style={{
              position: 'fixed',
              top: 12,
              left: 12,
              zIndex: 80,
              background: '#0B0B0C',
              border: '1px solid #27272A',
              color: '#FAFAFA',
              padding: '0.45rem 0.5rem',
              borderRadius: 10,
              boxShadow: '0 6px 20px rgba(0,0,0,0.4)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <svg width="20" height="14" viewBox="0 0 20 14" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden>
              <rect width="20" height="2" rx="1" fill="#E5E7EB" />
              <rect y="6" width="20" height="2" rx="1" fill="#E5E7EB" />
              <rect y="12" width="20" height="2" rx="1" fill="#E5E7EB" />
            </svg>
          </button>
        </>
      )}

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
