import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/client';

interface ReconciliationStats {
  total: number;
  matched: number;
  disputed: number;
  pending: number;
  matchRate: number;
}

interface RecentReconciliation {
  id: string;
  transactionRef: string;
  state: string;
  confidenceScore: number;
  createdAt: string;
  source: string;
}

export const ReconciliationDashboard = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<ReconciliationStats>({
    total: 0,
    matched: 0,
    disputed: 0,
    pending: 0,
    matchRate: 0
  });
  const [recentReconciliations, setRecentReconciliations] = useState<RecentReconciliation[]>([]);
  const [loading, setLoading] = useState(true);
  const [runningReconciliation, setRunningReconciliation] = useState(false);
  const [hasUploadedData, setHasUploadedData] = useState(false);

  const fetchData = async () => {
    try {
      // Fetch all reconciliations to calculate stats
      const response = await api.get('/api/disputes');
      const reconciliations = response.data;

      const matched = reconciliations.filter((r: any) => r.state === 'MATCHED').length;
      const disputed = reconciliations.filter((r: any) => r.state === 'DISPUTED' || r.state === 'AWAITING_REVIEW').length;
      const pending = reconciliations.filter((r: any) => r.state === 'PENDING' || r.state === 'PROCESSING').length;
      const total = reconciliations.length;

      setStats({
        total,
        matched,
        disputed,
        pending,
        matchRate: total > 0 ? (matched / total) * 100 : 0
      });

      setRecentReconciliations(reconciliations.slice(0, 10));
      
      // Check if there's any data in the system
      setHasUploadedData(total > 0);
      
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch reconciliation data:', error);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleRunReconciliation = async () => {
    if (!hasUploadedData) {
      // Redirect to upload page if no data
      navigate('/upload');
      return;
    }
    
    setRunningReconciliation(true);
    try {
      await api.post('/api/reconciliations/run', {
        source: 'all',
        dateRange: {
          start: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
          end: new Date().toISOString()
        }
      });
      alert('Reconciliation started! This may take a few minutes.');
      // Refresh data after a delay
      setTimeout(() => {
        fetchData();
      }, 3000);
    } catch (error) {
      alert('Failed to start reconciliation');
    } finally {
      setRunningReconciliation(false);
    }
  };

  const getStateBadge = (state: string) => {
    const stateMap: Record<string, string> = {
      'PENDING': 'badge-pending',
      'PROCESSING': 'badge-pending',
      'MATCHED': 'badge-approved',
      'DISPUTED': 'badge-high',
      'AWAITING_REVIEW': 'badge-pending',
      'APPROVED': 'badge-approved',
      'REJECTED': 'badge-rejected'
    };
    return stateMap[state] || 'badge-pending';
  };

  if (loading) {
    return (
      <div className="loading">
        <div>Loading reconciliation data...</div>
      </div>
    );
  }

  return (
    <div className="container fade-in">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 600, color: '#FAFAFA', marginBottom: '0.5rem' }}>Reconciliation Overview</h1>
          <p style={{ color: '#71717A', fontSize: '0.875rem' }}>
            Monitor transaction matching across payment providers, banks, and internal ledger
          </p>
        </div>
        <button
          onClick={handleRunReconciliation}
          disabled={runningReconciliation}
          className="btn btn-primary"
          title={!hasUploadedData ? 'Upload a CSV file first to run reconciliation' : ''}
        >
          {runningReconciliation ? '⚙ Running...' : !hasUploadedData ? '↑ Upload CSV First' : '▶ Run Reconciliation'}
        </button>
      </div>

      {/* Stats Cards */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', 
        gap: '1rem',
        marginBottom: '1.5rem'
      }}>
        <div className="card" style={{ borderLeft: '3px solid #3B82F6' }}>
          <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Total Transactions
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
            {stats.total.toLocaleString()}
          </p>
        </div>

        <div className="card" style={{ borderLeft: '3px solid #22C55E' }}>
          <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Matched
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
            {stats.matched.toLocaleString()}
          </p>
          <p style={{ fontSize: '0.75rem', color: '#A1A1AA', marginTop: '0.25rem' }}>
            {stats.matchRate.toFixed(1)}% match rate
          </p>
        </div>

        <div className="card" style={{ borderLeft: '3px solid #EF4444' }}>
          <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Disputed
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
            {stats.disputed.toLocaleString()}
          </p>
        </div>

        <div className="card" style={{ borderLeft: '3px solid #F59E0B' }}>
          <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Pending
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
            {stats.pending.toLocaleString()}
          </p>
        </div>
      </div>

      {/* Recent Reconciliations */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '1rem 1.25rem', borderBottom: '1px solid #27272A' }}>
          <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 500, color: '#FAFAFA' }}>Recent Reconciliations</h3>
          <Link to="/disputes" className="btn btn-secondary" style={{ padding: '0.5rem 1rem', fontSize: '0.8125rem' }}>
            View All Disputes
          </Link>
        </div>

        {recentReconciliations.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#71717A' }}>
            No reconciliations found. Upload a CSV or run reconciliation to get started.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', minWidth: '600px' }}>
              <thead>
                <tr style={{ background: '#18181B' }}>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Reference
                  </th>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Status
                  </th>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Confidence
                  </th>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Created
                  </th>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Action
                  </th>
                </tr>
              </thead>
              <tbody>
                {recentReconciliations.map((recon) => (
                  <tr key={recon.id} style={{ borderTop: '1px solid #27272A' }}>
                    <td style={{ padding: '1rem 1.25rem', fontFamily: 'monospace', fontWeight: 500, color: '#FAFAFA', fontSize: '0.8125rem' }}>
                      {recon.transactionRef}
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <span className={`badge ${getStateBadge(recon.state)}`}>
                        {recon.state.replace('_', ' ')}
                      </span>
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <span style={{ fontWeight: 500, color: '#FAFAFA' }}>
                        {recon.confidenceScore}%
                      </span>
                    </td>
                    <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', fontSize: '0.8125rem' }}>
                      {new Date(recon.createdAt).toLocaleDateString('en-US', {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <Link 
                        to={`/disputes/${recon.id}`} 
                        className="btn btn-primary" 
                        style={{ padding: '0.375rem 0.75rem', fontSize: '0.8125rem' }}
                      >
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', 
        gap: '1rem',
        marginTop: '1.5rem'
      }}>
        <Link to="/upload" className="card" style={{ textDecoration: 'none' }}>
          <div style={{ fontSize: '1.5rem', marginBottom: '0.75rem' }}>📁</div>
          <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', marginBottom: '0.5rem' }}>Upload Bank CSV</h3>
          <p style={{ color: '#71717A', fontSize: '0.8125rem' }}>
            Upload settlement files from GTBank, Access, Zenith, or other banks
          </p>
        </Link>

        <Link to="/transactions" className="card" style={{ textDecoration: 'none' }}>
          <div style={{ fontSize: '1.5rem', marginBottom: '0.75rem' }}>🔄</div>
          <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', marginBottom: '0.5rem' }}>Compare Transactions</h3>
          <p style={{ color: '#71717A', fontSize: '0.8125rem' }}>
            View side-by-side comparison of Provider, Bank, and Ledger data
          </p>
        </Link>

        <Link to="/webhooks" className="card" style={{ textDecoration: 'none' }}>
          <div style={{ fontSize: '1.5rem', marginBottom: '0.75rem' }}>⚡</div>
          <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', marginBottom: '0.5rem' }}>Webhook Monitor</h3>
          <p style={{ color: '#71717A', fontSize: '0.8125rem' }}>
            Track webhook delivery status and recovery operations
          </p>
        </Link>
      </div>
    </div>
  );
};
