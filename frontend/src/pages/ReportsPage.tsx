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
  DiscrepancyItem,
} from '../types/reports';

type ReportTab = 'daily' | 'discrepancy' | 'settlement' | 'audit';

const ReportsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ReportTab>('daily');
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [banks, setBanks] = useState<string[]>([]);

  // Form state
  const [date, setDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [startDate, setStartDate] = useState<string>(
    new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );
  const [endDate, setEndDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [selectedBank, setSelectedBank] = useState<string>('GTBANK');

  // Report data
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

  const getPriorityColor = (priority: number): string => {
    switch (priority) {
      case 1: return 'bg-red-100 text-red-800';
      case 2: return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-green-100 text-green-800';
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
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Reports</h1>
          <p className="mt-2 text-gray-600">
            Generate and export reconciliation reports
          </p>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200 mb-6">
          <nav className="-mb-px flex space-x-8">
            {[
              { id: 'daily', name: 'Daily Summary' },
              { id: 'discrepancy', name: 'Discrepancies' },
              { id: 'settlement', name: 'Settlement' },
              { id: 'audit', name: 'Audit Trail' },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as ReportTab)}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {tab.name}
              </button>
            ))}
          </nav>
        </div>

        {/* Form Controls */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            {(activeTab === 'daily' || activeTab === 'settlement') && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Date
                </label>
                <input
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                />
              </div>
            )}

            {(activeTab === 'discrepancy' || activeTab === 'audit') && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Start Date
                  </label>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    End Date
                  </label>
                  <input
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                  />
                </div>
              </>
            )}

            {activeTab === 'settlement' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Bank
                </label>
                <select
                  value={selectedBank}
                  onChange={(e) => setSelectedBank(e.target.value)}
                  className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                >
                  {banks.map((bank) => (
                    <option key={bank} value={bank}>
                      {bank}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="flex items-end gap-2">
              <button
                onClick={generateReport}
                disabled={loading}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
              >
                {loading ? (
                  <>
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    Generating...
                  </>
                ) : (
                  'Generate Report'
                )}
              </button>

              <div className="flex gap-2">
                <button
                  onClick={() => handleExport('excel')}
                  disabled={exporting}
                  className="px-3 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed text-sm"
                >
                  📊 Excel
                </button>
                <button
                  onClick={() => handleExport('csv')}
                  disabled={exporting}
                  className="px-3 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed text-sm"
                >
                  📄 CSV
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Error */}
        {error && (
          <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6">
            <p className="text-red-700">{error}</p>
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
    </div>
  );
};

// Daily Summary View Component
const DailySummaryView: React.FC<{ report: DailySummaryReport }> = ({ report }) => (
  <div className="space-y-6">
    {/* Summary Cards */}
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
      <StatCard
        title="Total Transactions"
        value={report.totalTransactions.toLocaleString()}
        subtitle={formatCurrency(report.totalAmount)}
        color="blue"
      />
      <StatCard
        title="Matched"
        value={report.matchedTransactions.toLocaleString()}
        subtitle={formatPercent(report.matchRate) + ' match rate'}
        color="green"
      />
      <StatCard
        title="Unmatched"
        value={report.unmatchedTransactions.toLocaleString()}
        subtitle={formatCurrency(report.unmatchedAmount)}
        color="yellow"
      />
      <StatCard
        title="Disputed"
        value={report.disputedTransactions.toLocaleString()}
        subtitle={formatCurrency(report.discrepancyAmount)}
        color="red"
      />
    </div>

    {/* Match Rate Breakdown */}
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-medium text-gray-900 mb-4">Match Rate Breakdown</h3>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <p className="text-sm text-gray-500">Overall Match Rate</p>
          <p className="text-2xl font-bold text-gray-900">{formatPercent(report.matchRate)}</p>
          <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
            <div 
              className="bg-blue-600 h-2 rounded-full" 
              style={{ width: `${Math.min(report.matchRate, 100)}%` }}
            />
          </div>
        </div>
        <div>
          <p className="text-sm text-gray-500">Auto Match Rate</p>
          <p className="text-2xl font-bold text-green-600">{formatPercent(report.autoMatchRate)}</p>
          <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
            <div 
              className="bg-green-600 h-2 rounded-full" 
              style={{ width: `${Math.min(report.autoMatchRate, 100)}%` }}
            />
          </div>
        </div>
        <div>
          <p className="text-sm text-gray-500">Manual Match Rate</p>
          <p className="text-2xl font-bold text-yellow-600">{formatPercent(report.manualMatchRate)}</p>
          <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
            <div 
              className="bg-yellow-600 h-2 rounded-full" 
              style={{ width: `${Math.min(report.manualMatchRate, 100)}%` }}
            />
          </div>
        </div>
      </div>
    </div>

    {/* Source Breakdown */}
    {report.sourceBreakdowns && report.sourceBreakdowns.length > 0 && (
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900">Source Breakdown</h3>
        </div>
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Source</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Count</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amount</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Matched</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Match Rate</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {report.sourceBreakdowns.map((source, idx) => (
              <tr key={idx}>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {source.source}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                  {source.transactionCount.toLocaleString()}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                  {formatCurrency(source.totalAmount)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                  {source.matchedCount.toLocaleString()}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-right">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    source.matchRate >= 90 ? 'bg-green-100 text-green-800' :
                    source.matchRate >= 70 ? 'bg-yellow-100 text-yellow-800' :
                    'bg-red-100 text-red-800'
                  }`}>
                    {formatPercent(source.matchRate)}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )}
  </div>
);

// Discrepancy View Component
const DiscrepancyView: React.FC<{
  report: DiscrepancyReport;
  getPriorityColor: (p: number) => string;
  getPriorityLabel: (p: number) => string;
}> = ({ report, getPriorityColor, getPriorityLabel }) => (
  <div className="space-y-6">
    {/* Summary Cards */}
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
      <StatCard
        title="Total Discrepancies"
        value={report.totalDiscrepancies.toLocaleString()}
        subtitle={formatCurrency(report.totalDiscrepancyAmount)}
        color="red"
      />
      <StatCard
        title="Pending"
        value={report.pendingDiscrepancies.toLocaleString()}
        color="yellow"
      />
      <StatCard
        title="High Priority"
        value={report.highPriority.toLocaleString()}
        color="red"
      />
      <StatCard
        title="Missing Transactions"
        value={report.missingTransactions.toLocaleString()}
        color="blue"
      />
    </div>

    {/* Discrepancy Table */}
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-200">
        <h3 className="text-lg font-medium text-gray-900">Discrepancy Details</h3>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Reference</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Source</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Expected</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actual</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Difference</th>
              <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Type</th>
              <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Priority</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {report.discrepancies.slice(0, 50).map((item, idx) => (
              <tr key={idx}>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-900">
                  {item.reference}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {item.source}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                  {formatCurrency(item.expectedAmount)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                  {formatCurrency(item.actualAmount)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-red-600 text-right font-medium">
                  {formatCurrency(item.difference)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-center">
                  <span className="px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                    {item.discrepancyType}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-center">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${getPriorityColor(item.priority)}`}>
                    {getPriorityLabel(item.priority)}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {report.discrepancies.length > 50 && (
        <div className="px-6 py-4 bg-gray-50 text-center text-sm text-gray-500">
          Showing 50 of {report.discrepancies.length} discrepancies. Export to see all.
        </div>
      )}
    </div>
  </div>
);

// Settlement View Component
const SettlementView: React.FC<{ report: SettlementReport }> = ({ report }) => (
  <div className="space-y-6">
    {/* Summary Cards */}
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
      <StatCard
        title="Expected Settlement"
        value={formatCurrency(report.expectedSettlement)}
        color="blue"
      />
      <StatCard
        title="Actual Settlement"
        value={formatCurrency(report.actualSettlement)}
        color="green"
      />
      <StatCard
        title="Variance"
        value={formatCurrency(report.variance)}
        color={report.variance === 0 ? 'green' : 'red'}
      />
      <StatCard
        title="Total Fees"
        value={formatCurrency(report.totalFees)}
        color="yellow"
      />
    </div>

    {/* Transaction Comparison */}
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-medium text-gray-900 mb-4">Transaction Comparison</h3>
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div className="text-center">
          <p className="text-sm text-gray-500">Expected</p>
          <p className="text-2xl font-bold text-gray-900">{report.expectedTransactionCount}</p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Actual</p>
          <p className="text-2xl font-bold text-gray-900">{report.actualTransactionCount}</p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Matched</p>
          <p className="text-2xl font-bold text-green-600">{report.matchedCount}</p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Missing from Bank</p>
          <p className="text-2xl font-bold text-red-600">{report.missingFromBank}</p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Missing from System</p>
          <p className="text-2xl font-bold text-yellow-600">{report.missingFromSystem}</p>
        </div>
      </div>
    </div>

    {/* Line Items Table */}
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-200">
        <h3 className="text-lg font-medium text-gray-900">Settlement Line Items</h3>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Reference</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">System</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Bank</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Fee</th>
              <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Status</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {report.lineItems.slice(0, 50).map((item, idx) => (
              <tr key={idx}>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-900">
                  {item.reference}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                  {formatCurrency(item.systemAmount)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                  {formatCurrency(item.bankAmount)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                  {formatCurrency(item.fee)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-center">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    item.status === 'MATCHED' ? 'bg-green-100 text-green-800' :
                    item.status === 'AMOUNT_VARIANCE' ? 'bg-yellow-100 text-yellow-800' :
                    'bg-red-100 text-red-800'
                  }`}>
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
  <div className="space-y-6">
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-medium text-gray-900 mb-4">
        Audit Trail ({report.entries.length} entries)
      </h3>
      <p className="text-sm text-gray-500">
        {formatDate(report.startDate)} to {formatDate(report.endDate)}
      </p>
    </div>

    <div className="bg-white rounded-lg shadow overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Timestamp</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Reference</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Details</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {report.entries.slice(0, 100).map((entry, idx) => (
              <tr key={idx}>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {entry.timestamp ? formatDateTime(entry.timestamp) : '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    entry.action === 'MATCHED' ? 'bg-green-100 text-green-800' :
                    entry.action === 'DISPUTED' ? 'bg-red-100 text-red-800' :
                    entry.action === 'RESOLVED' ? 'bg-blue-100 text-blue-800' :
                    'bg-gray-100 text-gray-800'
                  }`}>
                    {entry.action}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-900">
                  {entry.reference}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {entry.user || 'System'}
                </td>
                <td className="px-6 py-4 text-sm text-gray-500 max-w-xs truncate">
                  {entry.details}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {report.entries.length > 100 && (
        <div className="px-6 py-4 bg-gray-50 text-center text-sm text-gray-500">
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
  const colors = {
    blue: 'bg-blue-50 border-blue-200',
    green: 'bg-green-50 border-green-200',
    yellow: 'bg-yellow-50 border-yellow-200',
    red: 'bg-red-50 border-red-200',
  };

  return (
    <div className={`${colors[color]} border rounded-lg p-4`}>
      <p className="text-sm text-gray-600">{title}</p>
      <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
      {subtitle && <p className="text-sm text-gray-500 mt-1">{subtitle}</p>}
    </div>
  );
};

export default ReportsPage;
