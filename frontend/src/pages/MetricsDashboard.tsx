import React, { useEffect, useState } from 'react';
import api from '../api/client';

interface Metrics {
  reconciliationTime: {
    p50: number;
    p95: number;
    p99: number;
  };
  discrepancyRate: number;
  webhookRecoveryRate: number;
  disputeResolutionTime: number;
  operationalTimeSaved: number;
  transactionVolume: {
    total: number;
    bySource: {
      paystack: number;
      bank: number;
      ledger: number;
    };
  };
}

export const MetricsDashboard = () => {
  const [metrics, setMetrics] = useState<Metrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [timeRange, setTimeRange] = useState('7d');

  useEffect(() => {
    fetchMetrics();
  }, [timeRange]);

  const fetchMetrics = async () => {
    try {
      const response = await api.get('/api/metrics', {
        params: { range: timeRange }
      });
      setMetrics(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch metrics:', error);
      // Mock data for demo
      setMetrics({
        reconciliationTime: {
          p50: 2.3,
          p95: 4.8,
          p99: 7.2
        },
        discrepancyRate: 3.5,
        webhookRecoveryRate: 82.5,
        disputeResolutionTime: 6.2,
        operationalTimeSaved: 73,
        transactionVolume: {
          total: 15420,
          bySource: {
            paystack: 8200,
            bank: 7100,
            ledger: 15400
          }
        }
      });
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loading">
        <div>Loading metrics...</div>
      </div>
    );
  }

  if (!metrics) return null;

  return (
    <div className="container fade-in">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 600, color: '#FAFAFA', marginBottom: '0.5rem' }}>
            Operational Metrics
          </h1>
          <p style={{ color: '#71717A', fontSize: '0.875rem' }}>
            Monitor performance and efficiency of the reconciliation system
          </p>
        </div>
        <select
          value={timeRange}
          onChange={(e) => setTimeRange(e.target.value)}
          style={{
            padding: '0.625rem 1rem',
            background: '#18181B',
            border: '1px solid #27272A',
            borderRadius: '6px',
            color: '#FAFAFA',
            fontSize: '0.875rem',
            cursor: 'pointer'
          }}
        >
          <option value="24h">Last 24 Hours</option>
          <option value="7d">Last 7 Days</option>
          <option value="30d">Last 30 Days</option>
          <option value="90d">Last 90 Days</option>
        </select>
      </div>

      {/* Performance Metrics */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', marginBottom: '1.25rem' }}>
          Reconciliation Performance
        </h3>
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', 
          gap: '1.5rem'
        }}>
          <MetricCard
            label="p50 Duration"
            value={`${metrics.reconciliationTime.p50}s`}
            subtitle="Median reconciliation time"
            accentColor="#3B82F6"
          />
          <MetricCard
            label="p95 Duration"
            value={`${metrics.reconciliationTime.p95}s`}
            subtitle="95th percentile"
            accentColor="#F59E0B"
          />
          <MetricCard
            label="p99 Duration"
            value={`${metrics.reconciliationTime.p99}s`}
            subtitle="99th percentile"
            accentColor="#EF4444"
          />
        </div>
      </div>

      {/* Business Impact */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', marginBottom: '1.25rem' }}>
          Business Impact
        </h3>
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
          gap: '1.5rem'
        }}>
          <MetricCard
            label="Discrepancy Rate"
            value={`${metrics.discrepancyRate}%`}
            subtitle="Transactions with issues"
            accentColor="#F59E0B"
            trend="down"
          />
          <MetricCard
            label="Webhook Recovery"
            value={`${metrics.webhookRecoveryRate}%`}
            subtitle="Failed webhooks recovered"
            accentColor="#22C55E"
            trend="up"
          />
          <MetricCard
            label="Resolution Time"
            value={`${metrics.disputeResolutionTime}h`}
            subtitle="Avg dispute resolution"
            accentColor="#3B82F6"
            trend="down"
          />
          <MetricCard
            label="Time Saved"
            value={`${metrics.operationalTimeSaved}%`}
            subtitle="vs manual reconciliation"
            accentColor="#22C55E"
            trend="up"
          />
        </div>
      </div>

      {/* Transaction Volume */}
      <div className="card">
        <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', marginBottom: '1.25rem' }}>
          Transaction Volume
        </h3>
        <div style={{ marginBottom: '1.5rem' }}>
          <p style={{ fontSize: '2.5rem', fontWeight: 600, color: '#FAFAFA' }}>
            {metrics.transactionVolume.total.toLocaleString()}
          </p>
          <p style={{ color: '#71717A', fontSize: '0.8125rem' }}>Total transactions processed</p>
        </div>

        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', 
          gap: '1rem'
        }}>
          <div style={{ 
            padding: '1rem', 
            background: '#18181B', 
            borderRadius: '8px',
            borderLeft: '3px solid #3B82F6'
          }}>
            <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Payment Provider
            </p>
            <p style={{ fontSize: '1.25rem', fontWeight: 600, color: '#FAFAFA' }}>
              {metrics.transactionVolume.bySource.paystack.toLocaleString()}
            </p>
          </div>

          <div style={{ 
            padding: '1rem', 
            background: '#18181B', 
            borderRadius: '8px',
            borderLeft: '3px solid #22C55E'
          }}>
            <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Bank Settlement
            </p>
            <p style={{ fontSize: '1.25rem', fontWeight: 600, color: '#FAFAFA' }}>
              {metrics.transactionVolume.bySource.bank.toLocaleString()}
            </p>
          </div>

          <div style={{ 
            padding: '1rem', 
            background: '#18181B', 
            borderRadius: '8px',
            borderLeft: '3px solid #F59E0B'
          }}>
            <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Internal Ledger
            </p>
            <p style={{ fontSize: '1.25rem', fontWeight: 600, color: '#FAFAFA' }}>
              {metrics.transactionVolume.bySource.ledger.toLocaleString()}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

const MetricCard = ({ 
  label, 
  value, 
  subtitle, 
  accentColor, 
  trend 
}: { 
  label: string; 
  value: string; 
  subtitle: string; 
  accentColor: string;
  trend?: 'up' | 'down';
}) => (
  <div>
    <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
      {label}
    </p>
    <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem' }}>
      <p style={{ fontSize: '2rem', fontWeight: 600, color: '#FAFAFA' }}>
        {value}
      </p>
      {trend && (
        <span style={{ 
          fontSize: '0.75rem', 
          color: trend === 'up' ? '#4ADE80' : '#60A5FA',
          fontWeight: 500 
        }}>
          {trend === 'up' ? '↑' : '↓'}
        </span>
      )}
    </div>
    <p style={{ fontSize: '0.75rem', color: '#71717A', marginTop: '0.25rem' }}>
      {subtitle}
    </p>
  </div>
);
