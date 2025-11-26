import React, { useEffect, useState } from 'react';
import api from '../api/client';
import { useParams, useNavigate } from 'react-router-dom';

interface AuditEntry {
  timestamp: string;
  actor: string;
  action: string;
  reason?: string;
}

interface DisputeDetail {
  id: string;
  transactionRef: string;
  state: string;
  confidenceScore: number;
  rulesFired: { rule: string; weight: number }[];
  createdAt: string;
  auditTrail?: AuditEntry[];
}

export const DisputeDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [dispute, setDispute] = useState<DisputeDetail | null>(null);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    api.get(`/api/disputes/${id}`)
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
      await api.post(`/api/disputes/${id}/${action}`, { reason });
      navigate('/disputes');
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
          <button onClick={() => navigate('/disputes')} className="btn btn-primary mt-3">
            Back to Disputes
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container fade-in">
      <button 
        onClick={() => navigate('/disputes')} 
        className="btn btn-secondary mb-3"
        style={{ padding: '0.625rem 1.25rem' }}
      >
        ← Back to Disputes
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
            <span style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              {dispute.confidenceScore}%
            </span>
          </div>
        </div>
      </div>

      <div className="card mb-3">
        <h3 className="mb-3">Rules Analysis</h3>
        <div style={{ 
          marginBottom: '1.5rem', 
          padding: '1rem', 
          background: 'var(--bg-secondary)', 
          borderRadius: 'var(--radius-md)' 
        }}>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Total Confidence Score
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            {dispute.confidenceScore}%
          </p>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
            Calculated from {dispute.rulesFired.length} triggered rules
          </p>
        </div>

        <h4 style={{ fontSize: '1rem', marginBottom: '1rem', color: 'var(--text-secondary)' }}>
          Triggered Rules Breakdown
        </h4>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {dispute.rulesFired.map((r, idx) => {
            const contribution = ((r.weight / dispute.confidenceScore) * 100).toFixed(1);
            return (
              <div 
                key={idx}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '1rem',
                  background: 'var(--bg-secondary)',
                  borderRadius: 'var(--radius-md)',
                  transition: 'all 0.2s ease',
                  border: '1px solid var(--bg-tertiary)'
                }}
              >
                <div style={{ flex: 1 }}>
                  <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
                    {r.rule}
                  </span>
                  <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                    Contributes {contribution}% to total confidence
                  </p>
                </div>
                <span 
                  className="badge"
                  style={{ 
                    background: 'var(--primary)',
                    color: 'white',
                    fontWeight: 600
                  }}
                >
                  +{r.weight}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Audit Trail Timeline */}
      {dispute.auditTrail && dispute.auditTrail.length > 0 && (
        <div className="card mb-3">
          <h3 className="mb-3">Activity Timeline</h3>
          <div style={{ position: 'relative', paddingLeft: '2rem' }}>
            {/* Timeline line */}
            <div style={{
              position: 'absolute',
              left: '0.5rem',
              top: '0.5rem',
              bottom: '0.5rem',
              width: '2px',
              background: 'var(--bg-tertiary)'
            }} />

            {dispute.auditTrail.map((entry, idx) => (
              <div 
                key={idx}
                style={{
                  position: 'relative',
                  marginBottom: '1.5rem'
                }}
              >
                {/* Timeline dot */}
                <div style={{
                  position: 'absolute',
                  left: '-1.5rem',
                  top: '0.25rem',
                  width: '0.75rem',
                  height: '0.75rem',
                  borderRadius: '50%',
                  background: 'var(--primary)',
                  border: '2px solid var(--bg-primary)'
                }} />

                <div style={{
                  padding: '1rem',
                  background: 'var(--bg-secondary)',
                  borderRadius: 'var(--radius-md)'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {entry.action}
                    </span>
                    <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                      {new Date(entry.timestamp).toLocaleString()}
                    </span>
                  </div>
                  <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                    By: {entry.actor}
                  </p>
                  {entry.reason && (
                    <p style={{ 
                      marginTop: '0.5rem', 
                      fontSize: '0.875rem', 
                      color: 'var(--text-primary)',
                      fontStyle: 'italic',
                      padding: '0.5rem',
                      background: 'var(--bg-tertiary)',
                      borderRadius: 'var(--radius-sm)'
                    }}>
                      "{entry.reason}"
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

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
