import React, { useEffect, useState } from 'react';
import api from '../api/client';
import { Link } from 'react-router-dom';

interface Dispute {
  id: string;
  transactionRef: string;
  state: string;
  confidenceScore: number;
  createdAt: string;
}

export const DisputeList = () => {
  const [disputes, setDisputes] = useState<Dispute[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/api/disputes')
      .then(res => {
        setDisputes(res.data);
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  const getStateBadge = (state: string) => {
    const stateMap: Record<string, string> = {
      'PENDING': 'badge-pending',
      'APPROVED': 'badge-approved',
      'REJECTED': 'badge-rejected',
      'AWAITING_REVIEW': 'badge-pending'
    };
    return stateMap[state] || 'badge-pending';
  };

  const getScoreBadge = (score: number) => {
    if (score >= 90) return 'badge-high';
    if (score >= 70) return 'badge-medium';
    return 'badge-low';
  };

  if (loading) {
    return (
      <div className="loading">
        <div>Loading disputes...</div>
      </div>
    );
  }

  return (
    <div className="container fade-in">
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 600, color: '#FAFAFA', marginBottom: '0.5rem' }}>Dispute Triage Dashboard</h1>
        <p style={{ color: '#71717A', fontSize: '0.875rem' }}>
          Manage and review transaction disputes with confidence-based scoring
        </p>
      </div>

      {disputes.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <h3 style={{ color: '#A1A1AA', fontWeight: 500 }}>No disputes found</h3>
          <p style={{ marginTop: '0.5rem', color: '#71717A', fontSize: '0.875rem' }}>All transactions are reconciled successfully</p>
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table style={{ width: '100%' }}>
            <thead>
              <tr style={{ background: '#18181B' }}>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Reference</th>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Status</th>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Confidence Score</th>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Created At</th>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {disputes.map(d => (
                <tr key={d.id} style={{ borderTop: '1px solid #27272A' }}>
                  <td style={{ padding: '1rem 1.25rem' }}>
                    <span style={{ fontWeight: 500, fontFamily: 'monospace', color: '#FAFAFA', fontSize: '0.8125rem' }}>
                      {d.transactionRef}
                    </span>
                  </td>
                  <td style={{ padding: '1rem 1.25rem' }}>
                    <span className={`badge ${getStateBadge(d.state)}`}>
                      {d.state.replace('_', ' ')}
                    </span>
                  </td>
                  <td style={{ padding: '1rem 1.25rem' }}>
                    <span style={{ fontWeight: 500, color: '#FAFAFA' }}>
                      {d.confidenceScore}%
                    </span>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', fontSize: '0.8125rem' }}>
                    {new Date(d.createdAt).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </td>
                  <td style={{ padding: '1rem 1.25rem' }}>
                    <Link to={`/disputes/${d.id}`} className="btn btn-primary" style={{ padding: '0.375rem 0.75rem', fontSize: '0.8125rem' }}>
                      View Details
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
