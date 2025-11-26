/**
 * TypeScript interfaces for Report API
 */

// Report types enum
export type ReportType = 
  | 'DAILY_SUMMARY'
  | 'DISCREPANCY_REPORT'
  | 'AUDIT_TRAIL'
  | 'DISPUTE_REPORT'
  | 'TRANSACTION_DETAIL'
  | 'SETTLEMENT_RECONCILIATION';

// Export formats
export type ExportFormat = 'JSON' | 'CSV' | 'EXCEL' | 'PDF';

// Source breakdown for daily summary
export interface SourceBreakdown {
  source: string;
  transactionCount: number;
  totalAmount: number;
  matchedCount: number;
  unmatchedCount: number;
  matchRate: number;
}

// Discrepancy item
export interface DiscrepancyItem {
  reference: string;
  source: string;
  counterpartySource?: string;
  expectedAmount: number;
  actualAmount: number;
  difference: number;
  discrepancyType: 'AMOUNT_MISMATCH' | 'MISSING' | 'DUPLICATE';
  transactionDate: string;
  status: string;
  priority: 1 | 2 | 3; // 1=HIGH, 2=MEDIUM, 3=LOW
}

// Daily Summary Report
export interface DailySummaryReport {
  reportDate: string;
  generatedAt: string;
  
  // Transaction counts
  totalTransactions: number;
  matchedTransactions: number;
  unmatchedTransactions: number;
  pendingTransactions: number;
  disputedTransactions: number;
  
  // Amounts
  totalAmount: number;
  matchedAmount: number;
  unmatchedAmount: number;
  discrepancyAmount: number;
  
  // Match rates
  matchRate: number;
  autoMatchRate: number;
  manualMatchRate: number;
  
  // Breakdowns
  sourceBreakdowns: SourceBreakdown[];
  topDiscrepancies: DiscrepancyItem[];
  hourlyDistribution: Record<number, number>;
}

// Discrepancy Report
export interface DiscrepancyReport {
  startDate: string;
  endDate: string;
  generatedAt: string;
  
  // Summary
  totalDiscrepancies: number;
  resolvedDiscrepancies: number;
  pendingDiscrepancies: number;
  totalDiscrepancyAmount: number;
  
  // By type
  amountMismatches: number;
  missingTransactions: number;
  duplicateTransactions: number;
  
  // By priority
  highPriority: number;
  mediumPriority: number;
  lowPriority: number;
  
  // Details
  discrepancies: DiscrepancyItem[];
}

// Settlement line item
export interface SettlementLineItem {
  reference: string;
  systemAmount: number;
  bankAmount: number;
  fee: number;
  netAmount: number;
  status: 'MATCHED' | 'MISSING_FROM_BANK' | 'MISSING_FROM_SYSTEM' | 'AMOUNT_VARIANCE';
  transactionDate: string;
  settlementDate: string;
}

// Settlement Report
export interface SettlementReport {
  settlementDate: string;
  generatedAt: string;
  bankName: string;
  
  // Expected vs Actual
  expectedSettlement: number;
  actualSettlement: number;
  variance: number;
  
  // Transaction breakdown
  expectedTransactionCount: number;
  actualTransactionCount: number;
  matchedCount: number;
  missingFromBank: number;
  missingFromSystem: number;
  
  // Fee analysis
  totalFees: number;
  expectedFees: number;
  feeVariance: number;
  
  // Details
  lineItems: SettlementLineItem[];
}

// Audit entry
export interface AuditEntry {
  timestamp: string;
  action: string;
  entityType: string;
  entityId: string;
  reference: string;
  user: string;
  details: string;
  metadata?: Record<string, any>;
}

// Audit Trail Report
export interface AuditTrailReport {
  startDate: string;
  endDate: string;
  generatedAt: string;
  entries: AuditEntry[];
}

// Dispute item
export interface DisputeItem {
  disputeId: string;
  transactionReference: string;
  amount: number;
  reason: string;
  status: string;
  priority: string;
  createdAt: string;
  resolvedAt?: string;
  resolution?: string;
  assignedTo?: string;
}

// Dispute Report
export interface DisputeReport {
  startDate: string;
  endDate: string;
  generatedAt: string;
  
  // Summary
  totalDisputes: number;
  openDisputes: number;
  resolvedDisputes: number;
  escalatedDisputes: number;
  totalDisputedAmount: number;
  
  // Resolution stats
  avgResolutionTimeHours: number;
  autoResolvedCount: number;
  manualResolvedCount: number;
  
  // Details
  disputes: DisputeItem[];
}

// Report request parameters
export interface ReportRequest {
  reportType?: ReportType;
  startDate?: string;
  endDate?: string;
  date?: string;
  settlementDate?: string;
  bankName?: string;
}

// Report stats for dashboard
export interface ReportStats {
  reportTypes: ReportType[];
  exportFormats: ExportFormat[];
}
