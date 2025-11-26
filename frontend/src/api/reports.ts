import api from './client';
import {
  DailySummaryReport,
  DiscrepancyReport,
  SettlementReport,
  AuditTrailReport,
  ReportType,
  ExportFormat,
} from '../types/reports';

const BASE_URL = '/api/reports';

/**
 * Get daily summary report
 */
export const getDailySummary = async (date?: string): Promise<DailySummaryReport> => {
  const params = date ? { date } : {};
  const response = await api.get<DailySummaryReport>(`${BASE_URL}/daily-summary`, { params });
  return response.data;
};

/**
 * Get discrepancy report
 */
export const getDiscrepancyReport = async (
  startDate: string,
  endDate: string
): Promise<DiscrepancyReport> => {
  const response = await api.get<DiscrepancyReport>(`${BASE_URL}/discrepancies`, {
    params: { startDate, endDate },
  });
  return response.data;
};

/**
 * Get settlement report
 */
export const getSettlementReport = async (
  settlementDate: string,
  bankName: string = 'GTBank'
): Promise<SettlementReport> => {
  const response = await api.get<SettlementReport>(`${BASE_URL}/settlement`, {
    params: { settlementDate, bankName },
  });
  return response.data;
};

/**
 * Get audit trail report
 */
export const getAuditTrailReport = async (
  startDate: string,
  endDate: string
): Promise<AuditTrailReport> => {
  const response = await api.get<AuditTrailReport>(`${BASE_URL}/audit-trail`, {
    params: { startDate, endDate },
  });
  return response.data;
};

/**
 * Export daily summary to file
 */
export const exportDailySummary = async (
  format: 'excel' | 'csv',
  date?: string
): Promise<void> => {
  const params = date ? { date } : {};
  const response = await api.get(`${BASE_URL}/daily-summary/export/${format}`, {
    params,
    responseType: 'blob',
  });
  
  downloadFile(response.data, `daily-summary-${date || 'today'}.${format === 'excel' ? 'xlsx' : 'csv'}`);
};

/**
 * Export discrepancy report to file
 */
export const exportDiscrepancyReport = async (
  format: 'excel' | 'csv',
  startDate: string,
  endDate: string
): Promise<void> => {
  const response = await api.get(`${BASE_URL}/discrepancies/export/${format}`, {
    params: { startDate, endDate },
    responseType: 'blob',
  });
  
  downloadFile(
    response.data, 
    `discrepancy-report-${startDate}-to-${endDate}.${format === 'excel' ? 'xlsx' : 'csv'}`
  );
};

/**
 * Export settlement report to file
 */
export const exportSettlementReport = async (
  format: 'excel' | 'csv',
  settlementDate: string,
  bankName: string = 'GTBank'
): Promise<void> => {
  const response = await api.get(`${BASE_URL}/settlement/export/${format}`, {
    params: { settlementDate, bankName },
    responseType: 'blob',
  });
  
  const bankSlug = bankName.toLowerCase().replace(/\s+/g, '-');
  downloadFile(
    response.data, 
    `settlement-report-${bankSlug}-${settlementDate}.${format === 'excel' ? 'xlsx' : 'csv'}`
  );
};

/**
 * Export audit trail to file
 */
export const exportAuditTrail = async (
  format: 'excel' | 'csv',
  startDate: string,
  endDate: string
): Promise<void> => {
  const response = await api.get(`${BASE_URL}/audit-trail/export/${format}`, {
    params: { startDate, endDate },
    responseType: 'blob',
  });
  
  downloadFile(
    response.data, 
    `audit-trail-${startDate}-to-${endDate}.${format === 'excel' ? 'xlsx' : 'csv'}`
  );
};

/**
 * Get available report types
 */
export const getReportTypes = async (): Promise<ReportType[]> => {
  const response = await api.get<ReportType[]>(`${BASE_URL}/types`);
  return response.data;
};

/**
 * Get available export formats
 */
export const getExportFormats = async (): Promise<ExportFormat[]> => {
  const response = await api.get<ExportFormat[]>(`${BASE_URL}/formats`);
  return response.data;
};

/**
 * Helper function to download a file blob
 */
const downloadFile = (blob: Blob, filename: string): void => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

/**
 * Format currency for display
 */
export const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('en-NG', {
    style: 'currency',
    currency: 'NGN',
    minimumFractionDigits: 2,
  }).format(amount);
};

/**
 * Format percentage for display
 */
export const formatPercent = (value: number): string => {
  return `${value.toFixed(2)}%`;
};

/**
 * Format date for display
 */
export const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-NG', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

/**
 * Format datetime for display
 */
export const formatDateTime = (dateString: string): string => {
  return new Date(dateString).toLocaleString('en-NG', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};
