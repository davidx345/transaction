import React, { useState, useEffect } from 'react';
import {
  getDailySummary,
  getDiscrepancyReport,
  getSettlementReport,
  getAuditTrailReport,
  exportDailySummary,
  exportDiscrepancyReport,
  exportSettlementReport,
  exportAuditTrail,
  formatCurrency,
  formatPercent,
  formatDate,
  formatDateTime,
} from '../api/reports';
import { getSupportedBanks } from '../api/ingestion';
import {
  DailySummaryReport,
  DiscrepancyReport,
  SettlementReport,
  AuditTrailReport,
} from '../types/reports';

type ReportTab = 'daily' | 'discrepancy' | 'settlement' | 'audit';

const ReportsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ReportTab>('daily');
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [banks, setBanks] = useState<string[]>([]);

  const [date, setDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [startDate, setStartDate] = useState<string>(
    new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );
  const [endDate, setEndDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [selectedBank, setSelectedBank] = useState<string>('GTBANK');

  const [dailyReport, setDailyReport] = useState<DailySummaryReport | null>(null);
  const [discrepancyReport, setDiscrepancyReport] = useState<DiscrepancyReport | null>(null);
  const [settlementReport, setSettlementReport] = useState<SettlementReport | null>(null);
  const [auditReport, setAuditReport] = useState<AuditTrailReport | null>(null);

  useEffect(() => {
    loadBanks();
  }, []);

  const loadBanks = async () => {
    try {
      const bankList = await getSupportedBanks();
      setBanks(bankList);
    } catch (err) {
      console.error('Failed to load banks:', err);
    }
  };

  const generateReport = async () => {
    setLoading(true);
    setError(null);

    try {
      switch (activeTab) {
        case 'daily':
          const daily = await getDailySummary(date);
          setDailyReport(daily);
          break;
        case 'discrepancy':
          const discrepancy = await getDiscrepancyReport(startDate, endDate);
          setDiscrepancyReport(discrepancy);
          break;
        case 'settlement':
          const settlement = await getSettlementReport(date, selectedBank);
          setSettlementReport(settlement);
          break;
        case 'audit':
          const audit = await getAuditTrailReport(startDate, endDate);
          setAuditReport(audit);
          break;
      }
    } catch (err: any) {
      setError(err.message || 'Failed to generate report');
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async (format: 'excel' | 'csv') => {
    setExporting(true);
    setError(null);

    try {
      switch (activeTab) {
        case 'daily':
          await exportDailySummary(format, date);
          break;
        case 'discrepancy':
          await exportDiscrepancyReport(format, startDate, endDate);
          break;
        case 'settlement':
          await exportSettlementReport(format, date, selectedBank);
          break;
        case 'audit':
          await exportAuditTrail(format, startDate, endDate);
          break;
      }
    } catch (err: any) {
      setError(err.message || 'Failed to export report');
    } finally {
      setExporting(false);
    }
  };

  const getPriorityColor = (priority: number): React.CSSProperties => {
    switch (priority) {
      case 1: return { background: 'rgba(239, 68, 68, 0.15)', color: '#F87171' };
      case 2: return { background: 'rgba(245, 158, 11, 0.15)', color: '#FBBF24' };
      default: return { background: 'rgba(34, 197, 94, 0.15)', color: '#4ADE80' };
    }
  };

  const getPriorityLabel = (priority: number): string => {
    switch (priority) {
      case 1: return 'High';
      case 2: return 'Medium';
      default: return 'Low';
    }
  };

  return (
    <div className="container fade-in">
      {/* Header */}
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 600, color: '#FAFAFA', marginBottom: '0.5rem' }}>Reports</h1>
        <p style={{ color: '#71717A', fontSize: '0.875rem' }}>
          Generate and export reconciliation reports
        </p>
      </div>

      {/* Tabs */}
      <div style={{ borderBottom: '1px solid #27272A', marginBottom: '1.5rem' }}>
        <nav style={{ display: 'flex', gap: '2rem' }}>
          {[
            { id: 'daily', name: 'Daily Summary' },
            { id: 'discrepancy', name: 'Discrepancies' },
            { id: 'settlement', name: 'Settlement' },
            { id: 'audit', name: 'Audit Trail' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as ReportTab)}
              style={{
                padding: '1rem 0',
                borderBottom: activeTab === tab.id ? '2px solid #3B82F6' : '2px solid transparent',
                background: 'none',
                border: 'none',
                color: activeTab === tab.id ? '#FAFAFA' : '#71717A',
                fontWeight: activeTab === tab.id ? 500 : 400,
                fontSize: '0.875rem',
                cursor: 'pointer',
                transition: 'all 0.15s ease'
              }}
            >
              {tab.name}
            </button>
          ))}
        </nav>
      </div>

      {/* Form Controls */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', alignItems: 'end' }}>
          {(activeTab === 'daily' || activeTab === 'settlement') && (
            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 500, color: '#A1A1AA', marginBottom: '0.5rem' }}>
                Date
              </label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            </div>
          )}

          {(activeTab === 'discrepancy' || activeTab === 'audit') && (
            <>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 500, color: '#A1A1AA', marginBottom: '0.5rem' }}>
                  Start Date
                </label>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 500, color: '#A1A1AA', marginBottom: '0.5rem' }}>
                  End Date
                </label>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </div>
            </>
          )}

          {activeTab === 'settlement' && (
            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 500, color: '#A1A1AA', marginBottom: '0.5rem' }}>
                Bank
              </label>
              <select
                value={selectedBank}
                onChange={(e) => setSelectedBank(e.target.value)}
              >
                {banks.map((bank) => (
                  <option key={bank} value={bank}>
                    {bank}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              onClick={generateReport}
              disabled={loading}
              className="btn btn-primary"
            >
              {loading ? 'Generating...' : 'Generate Report'}
            </button>

            <button
              onClick={() => handleExport('excel')}
              disabled={exporting}
              className="btn btn-secondary"
              style={{ padding: '0.625rem 0.875rem' }}
            >
              📊 Excel
            </button>
            <button
              onClick={() => handleExport('csv')}
              disabled={exporting}
              className="btn btn-secondary"
              style={{ padding: '0.625rem 0.875rem' }}
            >
              📄 CSV
            </button>
          </div>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div style={{ 
          background: 'rgba(239, 68, 68, 0.1)', 
          borderLeft: '3px solid #EF4444',
          padding: '1rem',
          marginBottom: '1.5rem',
          borderRadius: '6px'
        }}>
          <p style={{ color: '#F87171', fontSize: '0.875rem' }}>{error}</p>
        </div>
      )}

      {/* Daily Summary Report */}
      {activeTab === 'daily' && dailyReport && (
        <DailySummaryView report={dailyReport} />
      )}

      {/* Discrepancy Report */}
      {activeTab === 'discrepancy' && discrepancyReport && (
        <DiscrepancyView 
          report={discrepancyReport} 
          getPriorityColor={getPriorityColor}
          getPriorityLabel={getPriorityLabel}
        />
      )}

      {/* Settlement Report */}
      {activeTab === 'settlement' && settlementReport && (
        <SettlementView report={settlementReport} />
      )}

      {/* Audit Trail Report */}
      {activeTab === 'audit' && auditReport && (
        <AuditTrailView report={auditReport} />
      )}
    </div>
  );
};

// Daily Summary View Component
const DailySummaryView: React.FC<{ report: DailySummaryReport }> = ({ report }) => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
    {/* Summary Cards */}
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
      <StatCard title="Total Transactions" value={report.totalTransactions.toLocaleString()} subtitle={formatCurrency(report.totalAmount)} color="blue" />
      <StatCard title="Matched" value={report.matchedTransactions.toLocaleString()} subtitle={formatPercent(report.matchRate) + ' match rate'} color="green" />
      <StatCard title="Unmatched" value={report.unmatchedTransactions.toLocaleString()} subtitle={formatCurrency(report.unmatchedAmount)} color="yellow" />
      <StatCard title="Disputed" value={report.disputedTransactions.toLocaleString()} subtitle={formatCurrency(report.discrepancyAmount)} color="red" />
    </div>

    {/* Match Rate Breakdown */}
    <div className="card">
      <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', marginBottom: '1rem' }}>Match Rate Breakdown</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem' }}>
        <RateBar label="Overall Match Rate" value={report.matchRate} color="#3B82F6" />
        <RateBar label="Auto Match Rate" value={report.autoMatchRate} color="#22C55E" />
        <RateBar label="Manual Match Rate" value={report.manualMatchRate} color="#F59E0B" />
      </div>
    </div>

    {/* Source Breakdown */}
    {report.sourceBreakdowns && report.sourceBreakdowns.length > 0 && (
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid #27272A' }}>
          <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', margin: 0 }}>Source Breakdown</h3>
        </div>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%' }}>
            <thead>
              <tr>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Source</th>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Count</th>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Amount</th>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Matched</th>
                <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Match Rate</th>
              </tr>
            </thead>
            <tbody>
              {report.sourceBreakdowns.map((source, idx) => (
                <tr key={idx} style={{ borderTop: '1px solid #27272A' }}>
                  <td style={{ padding: '1rem 1.25rem', color: '#FAFAFA', fontWeight: 500 }}>{source.source}</td>
                  <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', textAlign: 'right' }}>{source.transactionCount.toLocaleString()}</td>
                  <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', textAlign: 'right' }}>{formatCurrency(source.totalAmount)}</td>
                  <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', textAlign: 'right' }}>{source.matchedCount.toLocaleString()}</td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'right' }}>
                    <span style={{
                      padding: '0.25rem 0.625rem',
                      borderRadius: '100px',
                      fontSize: '0.75rem',
                      fontWeight: 500,
                      ...(source.matchRate >= 90 
                        ? { background: 'rgba(34, 197, 94, 0.15)', color: '#4ADE80' }
                        : source.matchRate >= 70 
                        ? { background: 'rgba(245, 158, 11, 0.15)', color: '#FBBF24' }
                        : { background: 'rgba(239, 68, 68, 0.15)', color: '#F87171' })
                    }}>
                      {formatPercent(source.matchRate)}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    )}
  </div>
);

// Discrepancy View Component
const DiscrepancyView: React.FC<{
  report: DiscrepancyReport;
  getPriorityColor: (p: number) => React.CSSProperties;
  getPriorityLabel: (p: number) => string;
}> = ({ report, getPriorityColor, getPriorityLabel }) => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
    {/* Summary Cards */}
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
      <StatCard title="Total Discrepancies" value={report.totalDiscrepancies.toLocaleString()} subtitle={formatCurrency(report.totalDiscrepancyAmount)} color="red" />
      <StatCard title="Pending" value={report.pendingDiscrepancies.toLocaleString()} color="yellow" />
      <StatCard title="High Priority" value={report.highPriority.toLocaleString()} color="red" />
      <StatCard title="Missing Transactions" value={report.missingTransactions.toLocaleString()} color="blue" />
    </div>

    {/* Discrepancy Table */}
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid #27272A' }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', margin: 0 }}>Discrepancy Details</h3>
      </div>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', minWidth: '800px' }}>
          <thead>
            <tr>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Reference</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Source</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Expected</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Actual</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Difference</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'center', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Type</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'center', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Priority</th>
            </tr>
          </thead>
          <tbody>
            {report.discrepancies.slice(0, 50).map((item, idx) => (
              <tr key={idx} style={{ borderTop: '1px solid #27272A' }}>
                <td style={{ padding: '1rem 1.25rem', fontFamily: 'monospace', color: '#FAFAFA', fontSize: '0.8125rem' }}>{item.reference}</td>
                <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA' }}>{item.source}</td>
                <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', textAlign: 'right' }}>{formatCurrency(item.expectedAmount)}</td>
                <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', textAlign: 'right' }}>{formatCurrency(item.actualAmount)}</td>
                <td style={{ padding: '1rem 1.25rem', color: '#F87171', textAlign: 'right', fontWeight: 500 }}>{formatCurrency(item.difference)}</td>
                <td style={{ padding: '1rem 1.25rem', textAlign: 'center' }}>
                  <span style={{ padding: '0.25rem 0.625rem', borderRadius: '100px', fontSize: '0.75rem', background: '#27272A', color: '#A1A1AA' }}>
                    {item.discrepancyType}
                  </span>
                </td>
                <td style={{ padding: '1rem 1.25rem', textAlign: 'center' }}>
                  <span style={{ padding: '0.25rem 0.625rem', borderRadius: '100px', fontSize: '0.75rem', fontWeight: 500, ...getPriorityColor(item.priority) }}>
                    {getPriorityLabel(item.priority)}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {report.discrepancies.length > 50 && (
        <div style={{ padding: '1rem', background: '#18181B', textAlign: 'center', fontSize: '0.8125rem', color: '#71717A' }}>
          Showing 50 of {report.discrepancies.length} discrepancies. Export to see all.
        </div>
      )}
    </div>
  </div>
);

// Settlement View Component
const SettlementView: React.FC<{ report: SettlementReport }> = ({ report }) => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
    {/* Summary Cards */}
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
      <StatCard title="Expected Settlement" value={formatCurrency(report.expectedSettlement)} color="blue" />
      <StatCard title="Actual Settlement" value={formatCurrency(report.actualSettlement)} color="green" />
      <StatCard title="Variance" value={formatCurrency(report.variance)} color={report.variance === 0 ? 'green' : 'red'} />
      <StatCard title="Total Fees" value={formatCurrency(report.totalFees)} color="yellow" />
    </div>

    {/* Transaction Comparison */}
    <div className="card">
      <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', marginBottom: '1rem' }}>Transaction Comparison</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: '1rem' }}>
        <ComparisonStat label="Expected" value={report.expectedTransactionCount} />
        <ComparisonStat label="Actual" value={report.actualTransactionCount} />
        <ComparisonStat label="Matched" value={report.matchedCount} color="#4ADE80" />
        <ComparisonStat label="Missing from Bank" value={report.missingFromBank} color="#F87171" />
        <ComparisonStat label="Missing from System" value={report.missingFromSystem} color="#FBBF24" />
      </div>
    </div>

    {/* Line Items Table */}
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid #27272A' }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', margin: 0 }}>Settlement Line Items</h3>
      </div>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', minWidth: '600px' }}>
          <thead>
            <tr>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Reference</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>System</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Bank</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'right', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Fee</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'center', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Status</th>
            </tr>
          </thead>
          <tbody>
            {report.lineItems.slice(0, 50).map((item, idx) => (
              <tr key={idx} style={{ borderTop: '1px solid #27272A' }}>
                <td style={{ padding: '1rem 1.25rem', fontFamily: 'monospace', color: '#FAFAFA', fontSize: '0.8125rem' }}>{item.reference}</td>
                <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', textAlign: 'right' }}>{formatCurrency(item.systemAmount)}</td>
                <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', textAlign: 'right' }}>{formatCurrency(item.bankAmount)}</td>
                <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', textAlign: 'right' }}>{formatCurrency(item.fee)}</td>
                <td style={{ padding: '1rem 1.25rem', textAlign: 'center' }}>
                  <span style={{
                    padding: '0.25rem 0.625rem',
                    borderRadius: '100px',
                    fontSize: '0.75rem',
                    fontWeight: 500,
                    ...(item.status === 'MATCHED' 
                      ? { background: 'rgba(34, 197, 94, 0.15)', color: '#4ADE80' }
                      : item.status === 'AMOUNT_VARIANCE' 
                      ? { background: 'rgba(245, 158, 11, 0.15)', color: '#FBBF24' }
                      : { background: 'rgba(239, 68, 68, 0.15)', color: '#F87171' })
                  }}>
                    {item.status.replace(/_/g, ' ')}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  </div>
);

// Audit Trail View Component
const AuditTrailView: React.FC<{ report: AuditTrailReport }> = ({ report }) => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
    <div className="card">
      <h3 style={{ fontSize: '1rem', fontWeight: 500, color: '#FAFAFA', marginBottom: '0.5rem' }}>
        Audit Trail ({report.entries.length} entries)
      </h3>
      <p style={{ fontSize: '0.8125rem', color: '#71717A' }}>
        {formatDate(report.startDate)} to {formatDate(report.endDate)}
      </p>
    </div>

    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', minWidth: '700px' }}>
          <thead>
            <tr>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Timestamp</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Action</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Reference</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>User</th>
              <th style={{ padding: '0.875rem 1.25rem', textAlign: 'left', fontSize: '0.75rem', color: '#71717A', textTransform: 'uppercase' }}>Details</th>
            </tr>
          </thead>
          <tbody>
            {report.entries.slice(0, 100).map((entry, idx) => (
              <tr key={idx} style={{ borderTop: '1px solid #27272A' }}>
                <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', fontSize: '0.8125rem' }}>
                  {entry.timestamp ? formatDateTime(entry.timestamp) : '-'}
                </td>
                <td style={{ padding: '1rem 1.25rem' }}>
                  <span style={{
                    padding: '0.25rem 0.625rem',
                    borderRadius: '100px',
                    fontSize: '0.75rem',
                    fontWeight: 500,
                    ...(entry.action === 'MATCHED' 
                      ? { background: 'rgba(34, 197, 94, 0.15)', color: '#4ADE80' }
                      : entry.action === 'DISPUTED' 
                      ? { background: 'rgba(239, 68, 68, 0.15)', color: '#F87171' }
                      : entry.action === 'RESOLVED' 
                      ? { background: 'rgba(59, 130, 246, 0.15)', color: '#60A5FA' }
                      : { background: '#27272A', color: '#A1A1AA' })
                  }}>
                    {entry.action}
                  </span>
                </td>
                <td style={{ padding: '1rem 1.25rem', fontFamily: 'monospace', color: '#FAFAFA', fontSize: '0.8125rem' }}>{entry.reference}</td>
                <td style={{ padding: '1rem 1.25rem', color: '#A1A1AA', fontSize: '0.8125rem' }}>{entry.user || 'System'}</td>
                <td style={{ padding: '1rem 1.25rem', color: '#71717A', fontSize: '0.8125rem', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{entry.details}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {report.entries.length > 100 && (
        <div style={{ padding: '1rem', background: '#18181B', textAlign: 'center', fontSize: '0.8125rem', color: '#71717A' }}>
          Showing 100 of {report.entries.length} entries. Export to see all.
        </div>
      )}
    </div>
  </div>
);

// Stat Card Component
const StatCard: React.FC<{
  title: string;
  value: string;
  subtitle?: string;
  color: 'blue' | 'green' | 'yellow' | 'red';
}> = ({ title, value, subtitle, color }) => {
  const borderColors = {
    blue: '#3B82F6',
    green: '#22C55E',
    yellow: '#F59E0B',
    red: '#EF4444',
  };

  return (
    <div className="card" style={{ borderLeft: `3px solid ${borderColors[color]}` }}>
      <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{title}</p>
      <p style={{ fontSize: '1.5rem', fontWeight: 600, color: '#FAFAFA' }}>{value}</p>
      {subtitle && <p style={{ fontSize: '0.75rem', color: '#A1A1AA', marginTop: '0.25rem' }}>{subtitle}</p>}
    </div>
  );
};

// Rate Bar Component
const RateBar: React.FC<{ label: string; value: number; color: string }> = ({ label, value, color }) => (
  <div>
    <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem' }}>{label}</p>
    <p style={{ fontSize: '1.5rem', fontWeight: 600, color: '#FAFAFA', marginBottom: '0.5rem' }}>{formatPercent(value)}</p>
    <div style={{ width: '100%', background: '#27272A', borderRadius: '4px', height: '6px' }}>
      <div style={{ width: `${Math.min(value, 100)}%`, background: color, height: '6px', borderRadius: '4px', transition: 'width 0.3s ease' }} />
    </div>
  </div>
);

// Comparison Stat Component
const ComparisonStat: React.FC<{ label: string; value: number; color?: string }> = ({ label, value, color = '#FAFAFA' }) => (
  <div style={{ textAlign: 'center' }}>
    <p style={{ fontSize: '0.75rem', color: '#71717A', marginBottom: '0.5rem' }}>{label}</p>
    <p style={{ fontSize: '1.5rem', fontWeight: 600, color }}>{value}</p>
  </div>
);

export default ReportsPage;
