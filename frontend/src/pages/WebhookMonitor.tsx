import React, { useEffect, useState } from 'react';
import api from '../api/client';

interface WebhookLog {
  id: string;
  transactionRef: string;
  provider: string;
  eventType: string;
  status: string;
  retryCount: number;
  expectedAt: string;
  receivedAt?: string;
  lastRetryAt?: string;
}

interface WebhookStats {
  total: number;
  received: number;
  missing: number;
  recovered: number;
  recoveryRate: number;
}

export const WebhookMonitor = () => {
  const [webhooks, setWebhooks] = useState<WebhookLog[]>([]);
  const [stats, setStats] = useState<WebhookStats>({
    total: 0,
    received: 0,
    missing: 0,
    recovered: 0,
    recoveryRate: 0
  });
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>('all');

  useEffect(() => {
    fetchWebhooks();
  }, [filter]);

  const fetchWebhooks = async () => {
    try {
      const response = await api.get('/api/webhooks', {
        params: filter !== 'all' ? { status: filter } : {}
      });
      
      const webhookData = response.data || [];
      setWebhooks(webhookData);

      // Calculate stats
      const received = webhookData.filter((w: WebhookLog) => w.status === 'received').length;
      const missing = webhookData.filter((w: WebhookLog) => w.status === 'missing').length;
      const recovered = webhookData.filter((w: WebhookLog) => w.status === 'recovered').length;
      const total = webhookData.length;

      setStats({
        total,
        received,
        missing,
        recovered,
        recoveryRate: total > 0 ? (recovered / (recovered + missing)) * 100 : 0
      });

      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch webhook data:', error);
      // Mock data for demo
      const mockWebhooks = [
        {
          id: '1',
          transactionRef: 'PSK_abc123',
          provider: 'paystack',
          eventType: 'charge.success',
          status: 'received',
          retryCount: 0,
          expectedAt: new Date(Date.now() - 3600000).toISOString(),
          receivedAt: new Date(Date.now() - 3550000).toISOString()
        },
        {
          id: '2',
          transactionRef: 'PSK_def456',
          provider: 'paystack',
          eventType: 'charge.success',
          status: 'recovered',
          retryCount: 2,
          expectedAt: new Date(Date.now() - 7200000).toISOString(),
          receivedAt: new Date(Date.now() - 5400000).toISOString(),
          lastRetryAt: new Date(Date.now() - 5400000).toISOString()
        },
        {
          id: '3',
          transactionRef: 'PSK_ghi789',
          provider: 'flutterwave',
          eventType: 'transfer.success',
          status: 'missing',
          retryCount: 3,
          expectedAt: new Date(Date.now() - 1800000).toISOString(),
          lastRetryAt: new Date(Date.now() - 600000).toISOString()
        }
      ];
      setWebhooks(mockWebhooks);
      setStats({
        total: 3,
        received: 1,
        missing: 1,
        recovered: 1,
        recoveryRate: 50
      });
      setLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    const statusMap: Record<string, string> = {
      'received': 'badge-approved',
      'missing': 'badge-high',
      'recovered': 'badge-medium',
      'delayed': 'badge-pending'
    };
    return statusMap[status] || 'badge-pending';
  };

  if (loading) {
    return (
      <div className="loading">
        <div>Loading webhook data...</div>
      </div>
    );
  }

  return (
    <div className="container fade-in">
      <h1 className="mb-2">Webhook Health Monitor</h1>
      <p style={{ marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        Track webhook delivery status, failures, and automatic recovery operations
      </p>

      {/* Stats Cards */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
        gap: '1.5rem',
        marginBottom: '2rem'
      }}>
        <div className="card">
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Total Webhooks
          </p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            {stats.total}
          </p>
        </div>

        <div className="card">
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Received
          </p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--success)' }}>
            {stats.received}
          </p>
        </div>

        <div className="card">
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Missing
          </p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--danger)' }}>
            {stats.missing}
          </p>
        </div>

        <div className="card">
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Recovered
          </p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--warning)' }}>
            {stats.recovered}
          </p>
        </div>

        <div className="card">
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            Recovery Rate
          </p>
          <p style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--primary)' }}>
            {stats.recoveryRate.toFixed(1)}%
          </p>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="card mb-3">
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          {['all', 'received', 'missing', 'recovered', 'delayed'].map((status) => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={filter === status ? 'btn btn-primary' : 'btn btn-secondary'}
              style={{ padding: '0.625rem 1.25rem' }}
            >
              {status.charAt(0).toUpperCase() + status.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* Webhook List */}
      <div className="card">
        <h3 className="mb-3">Webhook Log</h3>

        {webhooks.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
            No webhooks found for this filter
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', minWidth: '800px' }}>
              <thead style={{ background: 'var(--bg-secondary)' }}>
                <tr>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Transaction Ref
                  </th>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Provider
                  </th>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Event Type
                  </th>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Status
                  </th>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Retry Count
                  </th>
                  <th style={{ padding: '1rem', textAlign: 'left', fontSize: '0.875rem', fontWeight: 600 }}>
                    Expected At
                  </th>
                </tr>
              </thead>
              <tbody>
                {webhooks.map((webhook) => (
                  <tr key={webhook.id} style={{ borderTop: '1px solid var(--bg-secondary)' }}>
                    <td style={{ padding: '1rem', fontFamily: 'monospace', fontWeight: 600 }}>
                      {webhook.transactionRef}
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <span className="badge" style={{ background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}>
                        {webhook.provider}
                      </span>
                    </td>
                    <td style={{ padding: '1rem', fontSize: '0.875rem' }}>
                      {webhook.eventType}
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <span className={`badge ${getStatusBadge(webhook.status)}`}>
                        {webhook.status}
                      </span>
                    </td>
                    <td style={{ padding: '1rem', textAlign: 'center' }}>
                      {webhook.retryCount > 0 ? (
                        <span className="badge badge-warning">{webhook.retryCount} retries</span>
                      ) : (
                        <span style={{ color: 'var(--text-secondary)' }}>-</span>
                      )}
                    </td>
                    <td style={{ padding: '1rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                      {new Date(webhook.expectedAt).toLocaleString('en-US', {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Info Panel */}
      <div className="card mt-3" style={{ background: 'var(--bg-secondary)' }}>
        <h3 className="mb-2">Webhook Recovery Process</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem' }}>
          <div>
            <p style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.25rem' }}>Attempt 1</p>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Immediate API verification</p>
          </div>
          <div>
            <p style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.25rem' }}>Attempt 2-3</p>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Retry after 5 & 15 minutes</p>
          </div>
          <div>
            <p style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.25rem' }}>Attempt 4-5</p>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Retry after 30min & 1 hour</p>
          </div>
          <div>
            <p style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.25rem' }}>Final</p>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Move to Dead Letter Queue</p>
          </div>
        </div>
      </div>
    </div>
  );
};
