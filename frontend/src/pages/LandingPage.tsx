import React from 'react';
import { Link } from 'react-router-dom';

export const LandingPage: React.FC = () => {
  return (
    <div className="landing-page" style={{ background: 'var(--bg-primary)', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Navigation */}
      <nav style={{ 
        padding: '1.5rem 2rem', 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center',
        borderBottom: '1px solid var(--bg-secondary)',
        position: 'sticky',
        top: 0,
        background: 'rgba(255, 255, 255, 0.8)',
        backdropFilter: 'blur(10px)',
        zIndex: 100
      }}>
        <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--primary)' }}>
          ReconPlatform
        </div>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <Link to="/login" className="btn btn-secondary" style={{ padding: '0.5rem 1.5rem' }}>
            Login
          </Link>
          <Link to="/login" className="btn btn-primary" style={{ padding: '0.5rem 1.5rem' }}>
            Get Started
          </Link>
        </div>
      </nav>

      {/* Hero Section */}
      <header style={{ 
        padding: '6rem 2rem', 
        textAlign: 'center', 
        maxWidth: '1000px', 
        margin: '0 auto',
        flex: 1
      }}>
        <h1 style={{ 
          fontSize: '4rem', 
          marginBottom: '1.5rem', 
          background: 'linear-gradient(135deg, var(--primary) 0%, #00C6FF 100%)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
          lineHeight: 1.1
        }}>
          Financial Reconciliation <br /> Made Simple
        </h1>
        <p style={{ 
          fontSize: '1.5rem', 
          color: 'var(--text-secondary)', 
          marginBottom: '3rem', 
          maxWidth: '700px', 
          marginLeft: 'auto', 
          marginRight: 'auto' 
        }}>
          Automate your transaction matching, manage disputes effortlessly, and gain real-time insights into your financial health.
        </p>
        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
          <Link to="/login" className="btn btn-primary" style={{ fontSize: '1.25rem', padding: '1rem 2.5rem' }}>
            Start Reconciling Now
          </Link>
          <a href="#features" className="btn btn-secondary" style={{ fontSize: '1.25rem', padding: '1rem 2.5rem' }}>
            Learn More
          </a>
        </div>
      </header>

      {/* Features Section */}
      <section id="features" style={{ padding: '6rem 2rem', background: 'var(--bg-secondary)' }}>
        <div className="container">
          <h2 style={{ textAlign: 'center', marginBottom: '4rem', fontSize: '2.5rem' }}>
            Everything you need to stay balanced
          </h2>
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', 
            gap: '2rem' 
          }}>
            {/* Feature 1 */}
            <div className="card" style={{ padding: '2.5rem' }}>
              <div style={{ fontSize: '3rem', marginBottom: '1.5rem' }}>🔄</div>
              <h3>3-Way Reconciliation</h3>
              <p>Automatically match transactions across your internal database, payment gateways, and bank statements with high precision.</p>
            </div>
            {/* Feature 2 */}
            <div className="card" style={{ padding: '2.5rem' }}>
              <div style={{ fontSize: '3rem', marginBottom: '1.5rem' }}>⚖️</div>
              <h3>Dispute Management</h3>
              <p>Track, manage, and resolve transaction disputes efficiently with a centralized dashboard and audit trails.</p>
            </div>
            {/* Feature 3 */}
            <div className="card" style={{ padding: '2.5rem' }}>
              <div style={{ fontSize: '3rem', marginBottom: '1.5rem' }}>📊</div>
              <h3>Advanced Reporting</h3>
              <p>Gain actionable insights with real-time metrics, discrepancy analysis, and downloadable reconciliation reports.</p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer style={{ 
        padding: '4rem 2rem', 
        background: 'var(--bg-primary)', 
        borderTop: '1px solid var(--bg-tertiary)',
        textAlign: 'center'
      }}>
        <div className="container">
          <div style={{ marginBottom: '2rem', fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            ReconPlatform
          </div>
          <div style={{ display: 'flex', justifyContent: 'center', gap: '2rem', marginBottom: '2rem' }}>
            <a href="#" style={{ color: 'var(--text-secondary)', textDecoration: 'none' }}>About</a>
            <a href="#" style={{ color: 'var(--text-secondary)', textDecoration: 'none' }}>Contact</a>
            <a href="#" style={{ color: 'var(--text-secondary)', textDecoration: 'none' }}>Privacy</a>
            <a href="#" style={{ color: 'var(--text-secondary)', textDecoration: 'none' }}>Terms</a>
          </div>
          <p style={{ fontSize: '0.875rem' }}>© 2025 ReconPlatform. All rights reserved.</p>
        </div>
      </footer>
    </div>
  );
};
