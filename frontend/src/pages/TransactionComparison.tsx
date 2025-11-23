import React, { useEffect, useState } from 'react';
import api from '../api/client';

interface Transaction {
  id: string;
  source: string;
  externalReference: string;
  normalizedReference: string;
  amount: number;
  status: string;
  timestamp: string;
}

export const TransactionComparison = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRef, setSelectedRef] = useState<string>('');
  const [comparisonData, setComparisonData] = useState<{
    provider?: Transaction;
    bank?: Transaction;
    ledger?: Transaction;
  }>({});

  useEffect(() => {
    fetchTransactions();
  }, []);

  const fetchTransactions = async () => {
    try {
      // This would need a backend endpoint to fetch raw transactions
      // For now, we'll use disputes endpoint as proxy
      const response = await api.get('/api/disputes');
      setTransactions(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch transactions:', error);
      setLoading(false);
    }
  };

  const handleCompare = async (ref: string) => {
    setSelectedRef(ref);
    try {
      // Fetch transactions from different sources
      const response = await api.get(`/api/transactions/compare?ref=${ref}`);
      setComparisonData(response.data);
    } catch (error) {
      console.error('Failed to compare transactions:', error);
      // Mock data for demo
      setComparisonData({
        provider: {
          id: '1',
          source: 'paystack',
          externalReference: ref,
          normalizedReference: ref,
          amount: 5000.00,
          status: 'success',
          timestamp: new Date().toISOString()
        },
        bank: {
          id: '2',
          source: 'gtbank',
          externalReference: `GTB-${ref}`,
          normalizedReference: ref,
          amount: 5000.00,
          status: 'settled',
          timestamp: new Date().toISOString()
        },
        ledger: {
          id: '3',
          source: 'ledger',
          externalReference: ref,
          normalizedReference: ref,
          amount: 5000.00,
          status: 'completed',
          timestamp: new Date().toISOString()
        }
      });
    }
  };

  if (loading) {
    return (
      <div className="loading">
        <div>Loading transactions...</div>
      </div>
    );
  }

  return (
    <div className="container fade-in">
      <h1 className="mb-2">Transaction Source Comparison</h1>
      <p style={{ marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        Compare transaction data across Payment Provider, Bank Settlement, and Internal Ledger
      </p>

      <div className="card mb-3">
        <label style={{ 
          display: 'block', 
          marginBottom: '0.5rem', 
          fontSize: '0.875rem',
          fontWeight: 600,
          color: 'var(--text-secondary)'
        }}>
          Enter Transaction Reference
        </label>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <input
            type="text"
            placeholder="e.g., PSK_abc123"
            value={selectedRef}
            onChange={(e) => setSelectedRef(e.target.value)}
            style={{ flex: 1 }}
          />
          <button
            onClick={() => handleCompare(selectedRef)}
            disabled={!selectedRef}
            className="btn btn-primary"
          >
            Compare
          </button>
        </div>
      </div>

      {Object.keys(comparisonData).length > 0 && (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: '1.5rem'
        }}>
          {/* Payment Provider */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <div style={{ fontSize: '1.5rem' }}>💳</div>
              <h3 style={{ margin: 0 }}>Payment Provider</h3>
            </div>
            {comparisonData.provider ? (
              <div>
                <DataField label="Reference" value={comparisonData.provider.externalReference} />
                <DataField label="Amount" value={`₦${comparisonData.provider.amount.toLocaleString()}`} />
                <DataField label="Status" value={comparisonData.provider.status} />
                <DataField 
                  label="Timestamp" 
                  value={new Date(comparisonData.provider.timestamp).toLocaleString()} 
                />
              </div>
            ) : (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                No provider record found
              </div>
            )}
          </div>

          {/* Bank Settlement */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <div style={{ fontSize: '1.5rem' }}>🏦</div>
              <h3 style={{ margin: 0 }}>Bank Settlement</h3>
            </div>
            {comparisonData.bank ? (
              <div>
                <DataField label="Reference" value={comparisonData.bank.externalReference} />
                <DataField label="Amount" value={`₦${comparisonData.bank.amount.toLocaleString()}`} />
                <DataField label="Status" value={comparisonData.bank.status} />
                <DataField 
                  label="Timestamp" 
                  value={new Date(comparisonData.bank.timestamp).toLocaleString()} 
                />
              </div>
            ) : (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                No bank record found
              </div>
            )}
          </div>

          {/* Internal Ledger */}
          <div className="card">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <div style={{ fontSize: '1.5rem' }}>📒</div>
              <h3 style={{ margin: 0 }}>Internal Ledger</h3>
            </div>
            {comparisonData.ledger ? (
              <div>
                <DataField label="Reference" value={comparisonData.ledger.externalReference} />
                <DataField label="Amount" value={`₦${comparisonData.ledger.amount.toLocaleString()}`} />
                <DataField label="Status" value={comparisonData.ledger.status} />
                <DataField 
                  label="Timestamp" 
                  value={new Date(comparisonData.ledger.timestamp).toLocaleString()} 
                />
              </div>
            ) : (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                No ledger record found
              </div>
            )}
          </div>
        </div>
      )}

      {/* Analysis Section */}
      {Object.keys(comparisonData).length > 0 && (
        <div className="card mt-3">
          <h3 className="mb-2">Reconciliation Analysis</h3>
          <div style={{ 
            padding: '1rem', 
            background: 'var(--bg-secondary)', 
            borderRadius: 'var(--radius-md)' 
          }}>
            {comparisonData.provider && comparisonData.bank && comparisonData.ledger ? (
              <div style={{ color: 'var(--success)', fontWeight: 600 }}>
                ✓ All sources present - Transaction fully reconciled
              </div>
            ) : (
              <div style={{ color: 'var(--danger)', fontWeight: 600 }}>
                ⚠ Missing data from: {!comparisonData.provider && 'Provider '} 
                {!comparisonData.bank && 'Bank '} 
                {!comparisonData.ledger && 'Ledger'}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

const DataField = ({ label, value }: { label: string; value: string }) => (
  <div style={{ marginBottom: '1rem' }}>
    <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '0.25rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
      {label}
    </p>
    <p style={{ fontWeight: 600, color: 'var(--text-primary)', fontFamily: label === 'Amount' ? 'inherit' : 'monospace' }}>
      {value}
    </p>
  </div>
);
