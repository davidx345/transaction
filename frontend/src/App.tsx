import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Link, useLocation } from 'react-router-dom';
import { DisputeList } from './pages/DisputeList';
import { DisputeDetail } from './pages/DisputeDetail';
import { CSVUpload } from './pages/CSVUpload';
import { ReconciliationDashboard } from './pages/ReconciliationDashboard';
import { TransactionComparison } from './pages/TransactionComparison';
import { WebhookMonitor } from './pages/WebhookMonitor';
import { MetricsDashboard } from './pages/MetricsDashboard';

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

function AppContent() {
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: '▣' },
    { path: '/upload', label: 'CSV Upload', icon: '↑' },
    { path: '/', label: 'Disputes', icon: '!' },
    { path: '/transactions', label: 'Transactions', icon: '≡' },
    { path: '/webhooks', label: 'Webhooks', icon: '⚡' },
    { path: '/metrics', label: 'Metrics', icon: '▲' },
  ];

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      {/* Sidebar */}
      <aside style={{
        width: sidebarOpen ? '250px' : '70px',
        background: 'var(--bg-secondary)',
        borderRight: '1px solid var(--bg-tertiary)',
        transition: 'width 0.3s ease',
        position: 'sticky',
        top: 0,
        height: '100vh',
        overflow: 'hidden'
      }}>
        <div style={{
          padding: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid var(--bg-tertiary)'
        }}>
          {sidebarOpen && (
            <h2 style={{ 
              margin: 0, 
              fontSize: '1.25rem', 
              fontWeight: 700,
              color: 'var(--text-primary)'
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
              fontSize: '1.5rem',
              padding: '0.25rem',
              color: 'var(--text-secondary)'
            }}
          >
            {sidebarOpen ? '◀' : '▶'}
          </button>
        </div>

        <nav style={{ padding: '1rem' }}>
          {navItems.map(item => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  padding: '0.875rem 1rem',
                  marginBottom: '0.5rem',
                  borderRadius: 'var(--radius-md)',
                  textDecoration: 'none',
                  color: isActive ? 'var(--primary)' : 'var(--text-primary)',
                  background: isActive ? 'var(--bg-tertiary)' : 'transparent',
                  fontWeight: isActive ? 600 : 500,
                  transition: 'all 0.2s ease'
                }}
                onMouseEnter={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.background = 'var(--bg-tertiary)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.background = 'transparent';
                  }
                }}
              >
                <span style={{ fontSize: '1.25rem' }}>{item.icon}</span>
                {sidebarOpen && <span>{item.label}</span>}
              </Link>
            );
          })}
        </nav>
      </aside>

      {/* Main Content */}
      <main style={{ 
        flex: 1, 
        background: 'var(--bg-primary)',
        minHeight: '100vh',
        overflow: 'auto'
      }}>
        <Routes>
          <Route path="/" element={<DisputeList />} />
          <Route path="/disputes/:id" element={<DisputeDetail />} />
          <Route path="/dashboard" element={<ReconciliationDashboard />} />
          <Route path="/upload" element={<CSVUpload />} />
          <Route path="/transactions" element={<TransactionComparison />} />
          <Route path="/webhooks" element={<WebhookMonitor />} />
          <Route path="/metrics" element={<MetricsDashboard />} />
        </Routes>
      </main>
    </div>
  );
}

export default App;
