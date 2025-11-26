import React from 'react';
import { SUPPORTED_BANKS, SupportedBank } from '../types/ingestion';

interface BankSelectorProps {
  value: string;
  onChange: (bank: string) => void;
  disabled?: boolean;
  showAutoDetect?: boolean;
}

export const BankSelector: React.FC<BankSelectorProps> = ({
  value,
  onChange,
  disabled = false,
  showAutoDetect = true,
}) => {
  const banks = showAutoDetect 
    ? SUPPORTED_BANKS 
    : SUPPORTED_BANKS.filter(b => b.id !== 'auto');

  return (
    <div style={{ marginBottom: '1.5rem' }}>
      <label
        style={{
          display: 'block',
          marginBottom: '0.5rem',
          fontSize: '0.875rem',
          fontWeight: 600,
          color: 'var(--text-secondary)',
        }}
      >
        Select Bank Format
      </label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        style={{
          width: '100%',
          padding: '0.875rem 1rem',
          border: '1.5px solid var(--border)',
          borderRadius: 'var(--radius-md)',
          fontSize: '1rem',
          fontFamily: 'inherit',
          background: 'var(--bg-primary)',
          color: 'var(--text-primary)',
          cursor: disabled ? 'not-allowed' : 'pointer',
          opacity: disabled ? 0.7 : 1,
        }}
      >
        {banks.map((bank) => (
          <option key={bank.id} value={bank.id}>
            {bank.id === 'auto' ? '🔍 ' : '🏦 '}
            {bank.name} - {bank.description}
          </option>
        ))}
      </select>
      {value === 'auto' && (
        <p
          style={{
            marginTop: '0.5rem',
            fontSize: '0.75rem',
            color: 'var(--text-secondary)',
            fontStyle: 'italic',
          }}
        >
          The system will automatically detect the bank format from your CSV headers
        </p>
      )}
    </div>
  );
};

export default BankSelector;
