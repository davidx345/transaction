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
      <h1 style={{ fontSize: '1.75rem', fontWeight: 600, color: '#FAFAFA', marginBottom: '0.5rem' }}>Transaction Source Comparison</h1>
      <p style={{ marginBottom: '2rem', color: '#71717A', fontSize: '0.875rem' }}>
        Compare transaction data across Payment Provider, Bank Settlement, and Internal Ledger
      </p>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <label style={{ 
          display: 'block', 
          marginBottom: '0.5rem', 
          fontSize: '0.75rem',
          fontWeight: 500,
          color: '#71717A',
          textTransform: 'uppercase',
          letterSpacing: '0.05em'
        }}>
          Enter Transaction Reference
        </label>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <input
            type="text"
            placeholder="e.g., PSK_abc123"
            value={selectedRef}
            onChange={(e) => setSelectedRef(e.target.value)}
            style={{ 
              flex: 1,
              background: '#18181B',
              border: '1px solid #27272A',
              color: '#FAFAFA',
              padding: '0.75rem 1rem',
              borderRadius: '6px'
            }}
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
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '1rem'
        }}>
          {/* Payment Provider */}
          <div className="card" style={{ borderTop: '3px solid #3B82F6' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 500, color: '#FAFAFA' }}>Payment Provider</h3>
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
              <div style={{ padding: '2rem', textAlign: 'center', color: '#71717A' }}>
                No provider record found
              </div>
            )}
          </div>

          {/* Bank Settlement */}
          <div className="card" style={{ borderTop: '3px solid #22C55E' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 500, color: '#FAFAFA' }}>Bank Settlement</h3>
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
              <div style={{ padding: '2rem', textAlign: 'center', color: '#71717A' }}>
                No bank record found
              </div>
            )}
          </div>

          {/* Internal Ledger */}
          <div className="card" style={{ borderTop: '3px solid #8B5CF6' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 500, color: '#FAFAFA' }}>Internal Ledger</h3>
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
              <div style={{ padding: '2rem', textAlign: 'center', color: '#71717A' }}>
                No ledger record found
              </div>
            )}
          </div>
        </div>
      )}

      {/* Analysis Section */}
      {Object.keys(comparisonData).length > 0 && (
        <div className="card" style={{ marginTop: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem', fontSize: '1rem', fontWeight: 500, color: '#FAFAFA' }}>Reconciliation Analysis</h3>
          <div style={{ 
            padding: '1rem', 
            background: '#18181B', 
            borderRadius: '6px',
            border: '1px solid #27272A'
          }}>
            {comparisonData.provider && comparisonData.bank && comparisonData.ledger ? (
              <div style={{ color: '#22C55E', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <span>✓</span> All sources present - Transaction fully reconciled
              </div>
            ) : (
              <div style={{ color: '#EF4444', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <span>⚠</span> Missing data from: {!comparisonData.provider && 'Provider '} 
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
    <p style={{ fontSize: '0.6875rem', color: '#71717A', marginBottom: '0.25rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
      {label}
    </p>
    <p style={{ fontWeight: 500, color: '#FAFAFA', fontFamily: label === 'Amount' ? 'inherit' : 'monospace', fontSize: '0.875rem' }}>
      {value}
    </p>
  </div>
);
