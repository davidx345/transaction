import api from './client';
import { IngestionResult } from '../types/ingestion';

/**
 * Upload CSV file with specified bank format
 */
export const uploadCsv = async (
  file: File,
  bankName: string = 'auto'
): Promise<IngestionResult> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('bank', bankName);

  const response = await api.post<IngestionResult>('/api/ingest/csv', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data;
};

/**
 * Upload CSV file with auto-detection
 */
export const uploadCsvAutoDetect = async (file: File): Promise<IngestionResult> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await api.post<IngestionResult>('/api/ingest/csv/auto', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data;
};

/**
 * Get list of supported bank formats
 */
export const getSupportedBanks = async (): Promise<string[]> => {
  const response = await api.get<string[]>('/api/ingest/banks');
  return response.data;
};
