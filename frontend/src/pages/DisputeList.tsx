import React, { useEffect, useState } from 'react';
import axios from 'axios';
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
    axios.get('http://localhost:8080/api/disputes')
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
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1>Dispute Triage Dashboard</h1>
          <p style={{ marginTop: '0.5rem' }}>
            Manage and review transaction disputes with confidence-based scoring
          </p>
        </div>
      </div>

      {disputes.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <h3 style={{ color: 'var(--text-secondary)' }}>No disputes found</h3>
          <p style={{ marginTop: '0.5rem' }}>All transactions are reconciled successfully</p>
        </div>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Reference</th>
                <th>Status</th>
                <th>Confidence Score</th>
                <th>Created At</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {disputes.map(d => (
                <tr key={d.id}>
                  <td>
                    <span style={{ fontWeight: 600, fontFamily: 'monospace' }}>
                      {d.transactionRef}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${getStateBadge(d.state)}`}>
                      {d.state.replace('_', ' ')}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${getScoreBadge(d.confidenceScore)}`}>
                      {d.confidenceScore}%
                    </span>
                  </td>
                  <td style={{ color: 'var(--text-secondary)' }}>
                    {new Date(d.createdAt).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </td>
                  <td>
                    <Link to={`/disputes/${d.id}`} className="btn btn-primary" style={{ padding: '0.5rem 1rem' }}>
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
