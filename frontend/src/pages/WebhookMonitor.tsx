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
      <h1 style={{ fontSize: '1.75rem', fontWeight: 600, color: '#FAFAFA', marginBottom: '0.5rem' }}>Webhook Health Monitor</h1>
      <p style={{ marginBottom: '2rem', color: '#71717A', fontSize: '0.875rem' }}>
        Track webhook delivery status, failures, and automatic recovery operations
      </p>

      {/* Stats Cards */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', 
        gap: '1rem',
        marginBottom: '1.5rem'
      }}>
        <div className="card" style={{ borderLeft: '3px solid #3B82F6' }}>
          <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Total Webhooks
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
            {stats.total}
          </p>
        </div>

        <div className="card" style={{ borderLeft: '3px solid #22C55E' }}>
          <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Received
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
            {stats.received}
          </p>
        </div>

        <div className="card" style={{ borderLeft: '3px solid #EF4444' }}>
          <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Missing
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
            {stats.missing}
          </p>
        </div>

        <div className="card" style={{ borderLeft: '3px solid #F59E0B' }}>
          <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Recovered
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
            {stats.recovered}
          </p>
        </div>

        <div className="card" style={{ borderLeft: '3px solid #8B5CF6' }}>
          <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Recovery Rate
          </p>
          <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
            {stats.recoveryRate.toFixed(1)}%
          </p>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {['all', 'received', 'missing', 'recovered', 'delayed'].map((status) => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={filter === status ? 'btn btn-primary' : 'btn btn-secondary'}
              style={{ padding: '0.5rem 1rem', fontSize: '0.8125rem' }}
            >
              {status.charAt(0).toUpperCase() + status.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* Webhook List */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid #27272A' }}>
          <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 500, color: '#FAFAFA' }}>Webhook Log</h3>
        </div>

        {webhooks.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#71717A' }}>
            No webhooks found for this filter
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', minWidth: '800px' }}>
              <thead>
                <tr style={{ background: '#18181B' }}>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Transaction Ref
                  </th>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Provider
                  </th>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Event Type
                  </th>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Status
                  </th>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Retry Count
                  </th>
                  <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', fontWeight: 500, color: '#71717A', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Expected At
                  </th>
                </tr>
              </thead>
              <tbody>
                {webhooks.map((webhook) => (
                  <tr key={webhook.id} style={{ borderTop: '1px solid #27272A' }}>
                    <td style={{ padding: '1rem 1.25rem', fontFamily: 'monospace', fontWeight: 500, color: '#FAFAFA', fontSize: '0.8125rem' }}>
                      {webhook.transactionRef}
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <span style={{ padding: '0.25rem 0.625rem', borderRadius: '100px', fontSize: '0.75rem', background: '#27272A', color: '#A1A1AA' }}>
                        {webhook.provider}
                      </span>
                    </td>
                    <td style={{ padding: '1rem 1.25rem', fontSize: '0.8125rem', color: '#A1A1AA' }}>
                      {webhook.eventType}
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <span className={`badge ${getStatusBadge(webhook.status)}`}>
                        {webhook.status}
                      </span>
                    </td>
                    <td style={{ padding: '1rem 1.25rem', textAlign: 'center' }}>
                      {webhook.retryCount > 0 ? (
                        <span className="badge badge-warning">{webhook.retryCount} retries</span>
                      ) : (
                        <span style={{ color: '#71717A' }}>-</span>
                      )}
                    </td>
                    <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', fontSize: '0.8125rem' }}>
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
      <div className="card" style={{ marginTop: '1.5rem', background: '#18181B' }}>
        <h3 style={{ marginBottom: '1rem', fontSize: '1rem', fontWeight: 500, color: '#FAFAFA' }}>Webhook Recovery Process</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem' }}>
          <div>
            <p style={{ fontSize: '0.8125rem', fontWeight: 500, marginBottom: '0.25rem', color: '#FAFAFA' }}>Attempt 1</p>
            <p style={{ fontSize: '0.75rem', color: '#71717A' }}>Immediate API verification</p>
          </div>
          <div>
            <p style={{ fontSize: '0.8125rem', fontWeight: 500, marginBottom: '0.25rem', color: '#FAFAFA' }}>Attempt 2-3</p>
            <p style={{ fontSize: '0.75rem', color: '#71717A' }}>Retry after 5 & 15 minutes</p>
          </div>
          <div>
            <p style={{ fontSize: '0.8125rem', fontWeight: 500, marginBottom: '0.25rem', color: '#FAFAFA' }}>Attempt 4-5</p>
            <p style={{ fontSize: '0.75rem', color: '#71717A' }}>Retry after 30min & 1 hour</p>
          </div>
          <div>
            <p style={{ fontSize: '0.8125rem', fontWeight: 500, marginBottom: '0.25rem', color: '#FAFAFA' }}>Final</p>
            <p style={{ fontSize: '0.75rem', color: '#71717A' }}>Move to Dead Letter Queue</p>
          </div>
        </div>
      </div>
    </div>
  );
};
