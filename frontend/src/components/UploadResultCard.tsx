import React from 'react';
import { IngestionResult } from '../types/ingestion';

interface UploadResultCardProps {
  result: IngestionResult;
  onDismiss?: () => void;
}

export const UploadResultCard: React.FC<UploadResultCardProps> = ({ result, onDismiss }) => {
  const successRate = result.totalRecords > 0 
    ? ((result.successfulRecords / result.totalRecords) * 100).toFixed(1)
    : '0';

  return (
    <div
      className="fade-in"
      style={{
        padding: '1.5rem',
        borderRadius: 'var(--radius-lg)',
        marginBottom: '1.5rem',
        background: result.success
          ? 'linear-gradient(135deg, rgba(52, 199, 89, 0.1), rgba(52, 199, 89, 0.05))'
          : 'linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(255, 59, 48, 0.05))',
        border: `1px solid ${result.success ? 'var(--success)' : 'var(--danger)'}`,
      }}
    >
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span style={{ fontSize: '1.5rem' }}>
            {result.success ? '✅' : '❌'}
          </span>
          <div>
            <h3 style={{ margin: 0, fontSize: '1.125rem', fontWeight: 600 }}>
              {result.success ? 'Upload Successful' : 'Upload Failed'}
            </h3>
            <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              {result.message}
            </p>
          </div>
        </div>
        {onDismiss && (
          <button
            onClick={onDismiss}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              fontSize: '1.25rem',
              color: 'var(--text-secondary)',
              padding: '0.25rem',
            }}
          >
            ×
          </button>
        )}
      </div>

      {/* Statistics */}
      {result.success && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
            gap: '1rem',
            marginBottom: '1rem',
          }}
        >
          <StatBox label="Total Records" value={result.totalRecords} icon="📊" />
          <StatBox label="Successful" value={result.successfulRecords} icon="✅" color="var(--success)" />
          <StatBox label="Failed" value={result.failedRecords} icon="❌" color="var(--danger)" />
          <StatBox label="Skipped" value={result.skippedRecords} icon="⏭️" />
          <StatBox label="Success Rate" value={`${successRate}%`} icon="📈" />
        </div>
      )}

      {/* Auto-detection badge */}
      {result.autoDetected && (
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.5rem 1rem',
            background: 'rgba(0, 122, 255, 0.1)',
            borderRadius: 'var(--radius-md)',
            marginBottom: '1rem',
          }}
        >
          <span>🔍</span>
          <span style={{ fontSize: '0.875rem', fontWeight: 500, color: 'var(--primary)' }}>
            Auto-detected: {result.bankName}
          </span>
        </div>
      )}

      {/* Warnings */}
      {result.warnings && result.warnings.length > 0 && (
        <div style={{ marginBottom: '1rem' }}>
          <h4 style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.5rem', color: 'var(--warning)' }}>
            ⚠️ Warnings ({result.warnings.length})
          </h4>
          <ul
            style={{
              margin: 0,
              paddingLeft: '1.5rem',
              fontSize: '0.875rem',
              color: 'var(--text-secondary)',
            }}
          >
            {result.warnings.slice(0, 5).map((warning, idx) => (
              <li key={idx} style={{ marginBottom: '0.25rem' }}>{warning}</li>
            ))}
            {result.warnings.length > 5 && (
              <li style={{ fontStyle: 'italic' }}>...and {result.warnings.length - 5} more</li>
            )}
          </ul>
        </div>
      )}

      {/* Parse Errors */}
      {result.parseErrors && result.parseErrors.length > 0 && (
        <div>
          <h4 style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.5rem', color: 'var(--danger)' }}>
            ❌ Parse Errors ({result.parseErrors.length})
          </h4>
          <div
            style={{
              maxHeight: '150px',
              overflowY: 'auto',
              background: 'rgba(255, 59, 48, 0.05)',
              borderRadius: 'var(--radius-md)',
              padding: '0.75rem',
            }}
          >
            {result.parseErrors.slice(0, 10).map((error, idx) => (
              <div
                key={idx}
                style={{
                  fontSize: '0.75rem',
                  fontFamily: 'monospace',
                  marginBottom: '0.25rem',
                  color: 'var(--text-secondary)',
                }}
              >
                {error}
              </div>
            ))}
            {result.parseErrors.length > 10 && (
              <div style={{ fontSize: '0.75rem', fontStyle: 'italic', color: 'var(--text-secondary)' }}>
                ...and {result.parseErrors.length - 10} more errors
              </div>
            )}
          </div>
        </div>
      )}

      {/* File info */}
      <div
        style={{
          marginTop: '1rem',
          paddingTop: '1rem',
          borderTop: '1px solid var(--border)',
          display: 'flex',
          justifyContent: 'space-between',
          fontSize: '0.75rem',
          color: 'var(--text-secondary)',
        }}
      >
        <span>📁 {result.fileName}</span>
        <span>🏦 {result.bankName}</span>
        <span>🕐 {new Date(result.timestamp).toLocaleString()}</span>
      </div>
    </div>
  );
};

// Helper component for stats
const StatBox: React.FC<{
  label: string;
  value: number | string;
  icon: string;
  color?: string;
}> = ({ label, value, icon, color }) => (
  <div
    style={{
      textAlign: 'center',
      padding: '0.75rem',
      background: 'var(--bg-primary)',
      borderRadius: 'var(--radius-md)',
    }}
  >
    <div style={{ fontSize: '1.25rem', marginBottom: '0.25rem' }}>{icon}</div>
    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: color || 'var(--text-primary)' }}>
      {value}
    </div>
    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>{label}</div>
  </div>
);

export default UploadResultCard;
