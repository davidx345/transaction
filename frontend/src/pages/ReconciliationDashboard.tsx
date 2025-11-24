import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
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
    setRunningReconciliation(true);
    try {
      await api.post('/api/reconciliations/run', {
        source: 'all',
        date_range: {
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
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1>Reconciliation Overview</h1>
          <p style={{ marginTop: '0.5rem' }}>
            Monitor transaction matching across payment providers, banks, and internal ledger
          </p>
        </div>
        <button
          onClick={handleRunReconciliation}
          disabled={runningReconciliation}
          className="btn btn-primary"
        >
          {runningReconciliation ? '⚙ Running...' : '▶ Run Reconciliation'}
        </button>
      </div>

      {/* Stats Cards */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', 
        gap: '1.5rem',
        marginBottom: '2rem'
      }}>
        <div className="card">
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Total Transactions
          </p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            {stats.total.toLocaleString()}
          </p>
        </div>

        <div className="card">
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Matched
          </p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--success)' }}>
            {stats.matched.toLocaleString()}
          </p>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
            {stats.matchRate.toFixed(1)}% match rate
          </p>
        </div>

        <div className="card">
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Disputed
          </p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--danger)' }}>
            {stats.disputed.toLocaleString()}
          </p>
        </div>

        <div className="card">
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Pending
          </p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--warning)' }}>
            {stats.pending.toLocaleString()}
          </p>
        </div>
      </div>

      {/* Recent Reconciliations */}
      <div className="card">
        <div className="flex items-center justify-between mb-3">
          <h3 style={{ margin: 0 }}>Recent Reconciliations</h3>
          <Link to="/disputes" className="btn btn-secondary" style={{ padding: '0.5rem 1rem' }}>
            View All Disputes
          </Link>
        </div>

        {recentReconciliations.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>
            No reconciliations found. Upload a CSV or run reconciliation to get started.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', minWidth: '600px' }}>
              <thead style={{ background: 'var(--bg-secondary)' }}>
                <tr>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Reference
                  </th>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Status
                  </th>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Confidence
                  </th>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Created
                  </th>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Action
                  </th>
                </tr>
              </thead>
              <tbody>
                {recentReconciliations.map((recon) => (
                  <tr key={recon.id} style={{ borderTop: '1px solid var(--bg-secondary)' }}>
                    <td style={{ padding: '1rem', fontFamily: 'monospace', fontWeight: 600 }}>
                      {recon.transactionRef}
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <span className={`badge ${getStateBadge(recon.state)}`}>
                        {recon.state.replace('_', ' ')}
                      </span>
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <span className="badge badge-medium">
                        {recon.confidenceScore}%
                      </span>
                    </td>
                    <td style={{ padding: '1rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                      {new Date(recon.createdAt).toLocaleDateString('en-US', {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <Link 
                        to={`/disputes/${recon.id}`} 
                        className="btn btn-primary" 
                        style={{ padding: '0.5rem 1rem', fontSize: '0.875rem' }}
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
        gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', 
        gap: '1.5rem',
        marginTop: '2rem'
      }}>
        <Link to="/upload" className="card" style={{ textDecoration: 'none', transition: 'transform 0.2s' }}>
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}></div>
          <h3>Upload Bank CSV</h3>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
            Upload settlement files from GTBank, Access, Zenith, or other banks
          </p>
        </Link>

        <Link to="/transactions" className="card" style={{ textDecoration: 'none', transition: 'transform 0.2s' }}>
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}></div>
          <h3>Compare Transactions</h3>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
            View side-by-side comparison of Provider, Bank, and Ledger data
          </p>
        </Link>

        <Link to="/webhooks" className="card" style={{ textDecoration: 'none', transition: 'transform 0.2s' }}>
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}></div>
          <h3>Webhook Monitor</h3>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
            Track webhook delivery status and recovery operations
          </p>
        </Link>
      </div>
    </div>
  );
};
