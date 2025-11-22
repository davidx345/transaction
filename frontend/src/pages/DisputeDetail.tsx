import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom';

interface DisputeDetail {
  id: string;
  transactionRef: string;
  state: string;
  confidenceScore: number;
  rulesFired: { rule: string; weight: number }[];
}

export const DisputeDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [dispute, setDispute] = useState<DisputeDetail | null>(null);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    axios.get(`http://localhost:8080/api/disputes/${id}`)
      .then(res => {
        setDispute(res.data);
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setLoading(false);
      });
  }, [id]);

  const handleAction = async (action: 'approve' | 'reject') => {
    if (!reason.trim()) {
      alert('Please provide a reason for your decision');
      return;
    }

    setSubmitting(true);
    try {
      await axios.post(`http://localhost:8080/api/disputes/${id}/${action}`, { reason });
      navigate('/');
    } catch (err: any) {
      alert('Error: ' + (err.response?.data?.message || err.message));
      setSubmitting(false);
    }
  };

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
        <div>Loading dispute details...</div>
      </div>
    );
  }

  if (!dispute) {
    return (
      <div className="container">
        <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
          <h3 style={{ color: 'var(--text-secondary)' }}>Dispute not found</h3>
          <button onClick={() => navigate('/')} className="btn btn-primary mt-3">
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container fade-in">
      <button 
        onClick={() => navigate('/')} 
        className="btn btn-secondary mb-3"
        style={{ padding: '0.625rem 1.25rem' }}
      >
        ← Back to Dashboard
      </button>

      <div className="card mb-3">
        <div className="flex items-center justify-between mb-3">
          <h2 style={{ margin: 0 }}>Dispute Details</h2>
          <span className={`badge ${getStateBadge(dispute.state)}`}>
            {dispute.state.replace('_', ' ')}
          </span>
        </div>

        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', 
          gap: '1.5rem',
          marginTop: '1.5rem'
        }}>
          <div>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
              Transaction Reference
            </p>
            <p style={{ 
              fontSize: '1.125rem', 
              fontWeight: 600, 
              fontFamily: 'monospace',
              color: 'var(--text-primary)'
            }}>
              {dispute.transactionRef}
            </p>
          </div>

          <div>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
              Confidence Score
            </p>
            <span className={`badge ${getScoreBadge(dispute.confidenceScore)}`} style={{ fontSize: '1.125rem' }}>
              {dispute.confidenceScore}%
            </span>
          </div>
        </div>
      </div>

      <div className="card mb-3">
        <h3 className="mb-3">Rules Analysis</h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {dispute.rulesFired.map((r, idx) => (
            <div 
              key={idx}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '1rem',
                background: 'var(--bg-secondary)',
                borderRadius: 'var(--radius-md)',
                transition: 'all 0.2s ease'
              }}
            >
              <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
                {r.rule}
              </span>
              <span 
                className="badge"
                style={{ 
                  background: 'var(--bg-tertiary)',
                  color: 'var(--text-primary)'
                }}
              >
                Weight: {r.weight}
              </span>
            </div>
          ))}
        </div>
      </div>

      {dispute.state === 'PENDING' || dispute.state === 'AWAITING_REVIEW' ? (
        <div className="card">
          <h3 className="mb-3">Take Action</h3>
          <div style={{ marginBottom: '1.5rem' }}>
            <label style={{ 
              display: 'block', 
              marginBottom: '0.5rem', 
              fontSize: '0.875rem',
              fontWeight: 600,
              color: 'var(--text-secondary)'
            }}>
              Decision Reason *
            </label>
            <textarea 
              placeholder="Provide a detailed reason for your decision..." 
              value={reason}
              onChange={e => setReason(e.target.value)}
              rows={4}
              disabled={submitting}
            />
          </div>

          <div style={{ display: 'flex', gap: '1rem' }}>
            <button 
              onClick={() => handleAction('approve')} 
              className="btn btn-success"
              disabled={submitting || !reason.trim()}
              style={{ flex: 1 }}
            >
              {submitting ? 'Processing...' : '✓ Approve & Initiate Refund'}
            </button>
            <button 
              onClick={() => handleAction('reject')} 
              className="btn btn-danger"
              disabled={submitting || !reason.trim()}
              style={{ flex: 1 }}
            >
              {submitting ? 'Processing...' : '✕ Reject Dispute'}
            </button>
          </div>
        </div>
      ) : (
        <div className="card" style={{ textAlign: 'center', padding: '2rem', background: 'var(--bg-secondary)' }}>
          <p style={{ fontSize: '1.125rem', color: 'var(--text-secondary)' }}>
            This dispute has been {dispute.state.toLowerCase()} and cannot be modified.
          </p>
        </div>
      )}
    </div>
  );
};
