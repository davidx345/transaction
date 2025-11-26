// Types for Transaction Ingestion API

export interface IngestionResult {
  success: boolean;
  fileName: string;
  bankName: string;
  autoDetected: boolean;
  message: string;
  totalRecords: number;
  successfulRecords: number;
  failedRecords: number;
  skippedRecords: number;
  warnings: string[];
  parseErrors: string[];
  timestamp: string;
  successRate?: number;
}

export interface BankFormat {
  name: string;
  displayName: string;
  dateFormat: string;
  referencePrefix: string;
}

export interface ParsedTransaction {
  externalReference: string;
  normalizedReference: string;
  amount: number;
  currency: string;
  timestamp: string;
  status: string;
  narration?: string;
  customerIdentifier?: string;
  transactionType: 'DEBIT' | 'CREDIT' | 'UNKNOWN';
  sourceRow: number;
  extractedReferenceSource: 'column' | 'narration' | 'generated';
  parseConfidence: number;
  parseWarnings: string[];
}

export interface CsvParseResult {
  detectedFormat: BankFormat;
  fileName: string;
  totalRows: number;
  successfulRows: number;
  failedRows: number;
  skippedRows: number;
  transactions: ParsedTransaction[];
  errors: ParseError[];
  warnings: string[];
  metadata: ParsingMetadata;
}

export interface ParseError {
  row: number;
  message: string;
  rawData: string;
}

export interface ParsingMetadata {
  parseTimeMs: number;
  headerRow: number;
  delimiter: string;
  dateFormat: string;
  autoDetected: boolean;
  detectionMethod: 'header' | 'reference' | 'content' | 'manual';
  detectedColumns: string[];
  columnMapping: Record<string, number>;
}

// Bank information for UI
export const SUPPORTED_BANKS = [
  { id: 'auto', name: 'Auto-Detect', description: 'Automatically detect bank format' },
  { id: 'GTBank', name: 'GTBank', description: 'Guaranty Trust Bank' },
  { id: 'AccessBank', name: 'Access Bank', description: 'Access Bank Nigeria' },
  { id: 'ZenithBank', name: 'Zenith Bank', description: 'Zenith Bank Nigeria' },
  { id: 'FirstBank', name: 'First Bank', description: 'First Bank of Nigeria' },
  { id: 'UBA', name: 'UBA', description: 'United Bank for Africa' },
  { id: 'Paystack', name: 'Paystack', description: 'Paystack Payment Gateway' },
  { id: 'Flutterwave', name: 'Flutterwave', description: 'Flutterwave Payment Gateway' },
  { id: 'Generic', name: 'Generic', description: 'Generic CSV format' },
] as const;

export type SupportedBank = typeof SUPPORTED_BANKS[number]['id'];

// Sample CSV content for different banks
export const SAMPLE_CSV_CONTENT: Record<string, { content: string; filename: string }> = {
  GTBank: {
    filename: 'gtbank_settlement.csv',
    content: `PAYMENT_REF,AMOUNT,SETTLEMENT_DATE,STATUS
GTB-PSK_abc123,5000.00,22/11/2024,SUCCESS
GTB-PSK_def456,15000.50,22/11/2024,SUCCESS
GTB-PSK_ghi789,7500.00,23/11/2024,SUCCESS
GTB-ORDER_1001,2500.00,23/11/2024,PENDING
GTB-INV_2024001,50000.00,24/11/2024,SUCCESS`
  },
  AccessBank: {
    filename: 'accessbank_statement.csv',
    content: `POST DATE,VALUE DATE,REFERENCE,DEBIT AMT,CREDIT AMT,BALANCE,NARRATION
22-Nov-2024,22-Nov-2024,ACC-TRF001234,5000.00,,150000.00,NIP TRANSFER TO JOHN DOE REF:PSK_abc123
22-Nov-2024,22-Nov-2024,ACC-POS567890,,15000.50,165000.50,POS PURCHASE SHOPRITE
23-Nov-2024,23-Nov-2024,ACC-WEB789012,7500.00,,157500.50,WEB TRANSFER ORDER_1001 PAYMENT
23-Nov-2024,24-Nov-2024,ACC-MOB345678,,25000.00,182500.50,MOBILE TRANSFER FROM JANE SMITH`
  },
  ZenithBank: {
    filename: 'zenithbank_statement.csv',
    content: `TRANS DATE,VALUE DATE,TRANS ID,DR AMOUNT,CR AMOUNT,BALANCE,REMARKS
22/11/2024,22/11/2024,ZEN-123456789,5000.00,,95000.00,NIP/FLW-abc123/ONLINE PAYMENT
22/11/2024,22/11/2024,ZEN-987654321,,15000.50,110500.50,INWARD TRANSFER/PSK_def456
23/11/2024,23/11/2024,ZEN-456789123,7500.00,,103000.50,BILL PAYMENT/ORDER-1002`
  },
  FirstBank: {
    filename: 'firstbank_statement.csv',
    content: `TRANSACTION DATE,VALUE DATE,REFERENCE,DEBIT,CREDIT,BALANCE,DESCRIPTION
2024-11-22,2024-11-22,FBN-TRF123456,5000.00,,200000.00,TRANSFER TO ACCESS BANK/PSK_abc123
2024-11-22,2024-11-22,FBN-NIP789012,,15000.50,215000.50,NIP CREDIT FROM ZENITH/REF:TXN999
2024-11-23,2024-11-23,FBN-POS345678,7500.00,,207500.50,POS PAYMENT AT MERCHANT`
  },
  UBA: {
    filename: 'uba_statement.csv',
    content: `TRAN DATE,VALUE DATE,TRAN REF,DEBIT,CREDIT,BALANCE,NARRATION
22/11/2024,22/11/2024,UBA-1234567890,5000.00,,85000.00,NIP TRANSFER/PSK_abc123/PAYMENT
22/11/2024,22/11/2024,UBA-0987654321,,15000.50,100500.50,CREDIT FROM GTB/FLW-xyz789
23/11/2024,23/11/2024,UBA-5678901234,7500.00,,93000.50,USSD TRANSFER/ORDER_1003`
  },
  Paystack: {
    filename: 'paystack_transactions.csv',
    content: `ID,REFERENCE,AMOUNT,CURRENCY,STATUS,PAID_AT,CREATED_AT,CUSTOMER_EMAIL,CHANNEL
1234567890,PSK_abc123,5000.00,NGN,success,2024-11-22T10:30:00,2024-11-22T10:29:55,john@example.com,card
1234567891,PSK_def456,15000.50,NGN,success,2024-11-22T14:45:00,2024-11-22T14:44:30,jane@example.com,bank_transfer
1234567892,PSK_ghi789,7500.00,NGN,success,2024-11-23T09:15:00,2024-11-23T09:14:45,bob@example.com,card`
  },
  Flutterwave: {
    filename: 'flutterwave_transactions.csv',
    content: `ID,TX_REF,FLUTTERWAVEREF,AMOUNT,CHARGED_AMOUNT,CURRENCY,STATUS,CREATED_AT,CUSTOMER_EMAIL,PAYMENT_TYPE
9876543210,ORDER_1001,FLW-abc123,5000.00,5075.00,NGN,successful,2024-11-22 10:30:00,john@example.com,card
9876543211,ORDER_1002,FLW-def456,15000.50,15225.75,NGN,successful,2024-11-22 14:45:00,jane@example.com,banktransfer`
  }
};
