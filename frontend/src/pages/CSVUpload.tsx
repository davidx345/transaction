import React, { useState, useRef, useEffect } from 'react';
import { uploadCsv, getSupportedBanks } from '../api/ingestion';
import { IngestionResult, SAMPLE_CSV_CONTENT, SUPPORTED_BANKS } from '../types/ingestion';
import BankSelector from '../components/BankSelector';
import UploadResultCard from '../components/UploadResultCard';

export const CSVUpload = () => {
  const [uploading, setUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedBank, setSelectedBank] = useState('auto');
  const [uploadResult, setUploadResult] = useState<IngestionResult | null>(null);
  const [availableBanks, setAvailableBanks] = useState<string[]>([]);
  const [showSampleSelector, setShowSampleSelector] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Fetch available banks on mount
  useEffect(() => {
    const fetchBanks = async () => {
      try {
        const banks = await getSupportedBanks();
        setAvailableBanks(banks);
      } catch (error) {
        console.error('Failed to fetch supported banks:', error);
      }
    };
    fetchBanks();
  }, []);

  // Load sample file for a specific bank
  const loadSampleFile = (bankId: string) => {
    const sample = SAMPLE_CSV_CONTENT[bankId];
    if (sample) {
      const blob = new Blob([sample.content], { type: 'text/csv' });
      const file = new File([blob], sample.filename, { type: 'text/csv' });
      setSelectedFile(file);
      setSelectedBank(bankId);
      setUploadResult(null);
      setShowSampleSelector(false);
    }
  };

  // Download sample file
  const downloadSampleFile = (bankId: string) => {
    const sample = SAMPLE_CSV_CONTENT[bankId];
    if (sample) {
      const blob = new Blob([sample.content], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = sample.filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const file = e.dataTransfer.files[0];
      if (file.name.endsWith('.csv')) {
        setSelectedFile(file);
        setUploadResult(null);
      } else {
        alert('Please upload a CSV file');
      }
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      setUploadResult(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    setUploading(true);
    setUploadResult(null);

    try {
      const result = await uploadCsv(selectedFile, selectedBank);
      setUploadResult(result);
      
      if (result.success) {
        setSelectedFile(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
      }
    } catch (error: any) {
      setUploadResult({
        success: false,
        fileName: selectedFile.name,
        bankName: selectedBank,
        autoDetected: false,
        message: error.response?.data?.message || 'Upload failed. Please try again.',
        totalRecords: 0,
        successfulRecords: 0,
        failedRecords: 0,
        skippedRecords: 0,
        warnings: [],
        parseErrors: [],
        timestamp: new Date().toISOString(),
      });
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="container fade-in">
      <h1 style={{ fontSize: '1.75rem', fontWeight: 600, color: '#FAFAFA', marginBottom: '0.5rem' }}>Upload Bank Settlement CSV</h1>
      <p style={{ marginBottom: '2rem', color: '#71717A', fontSize: '0.875rem' }}>
        Upload bank settlement files from Nigerian banks or payment providers. 
        The system auto-detects the format or you can specify the bank.
      </p>

      <div className="card" style={{ maxWidth: '800px' }}>
        {/* Bank Selector */}
        <BankSelector
          value={selectedBank}
          onChange={setSelectedBank}
          disabled={uploading}
          showAutoDetect={true}
        />

        {/* Drop Zone */}
        <div
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
          style={{
            border: `2px dashed ${dragActive ? '#3B82F6' : '#27272A'}`,
            borderRadius: '8px',
            padding: '3rem',
            textAlign: 'center',
            background: dragActive ? 'rgba(59, 130, 246, 0.05)' : '#18181B',
            transition: 'all 0.2s ease',
            marginBottom: '1.5rem',
          }}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv"
            onChange={handleFileChange}
            disabled={uploading}
            style={{ display: 'none' }}
            id="file-upload"
          />

          {selectedFile ? (
            <div>
              <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>📄</div>
              <p style={{ fontWeight: 500, fontSize: '1rem', marginBottom: '0.25rem', color: '#FAFAFA' }}>
                {selectedFile.name}
              </p>
              <p style={{ color: '#71717A', fontSize: '0.8125rem' }}>
                {(selectedFile.size / 1024).toFixed(2)} KB
              </p>
            </div>
          ) : (
            <div>
              <div style={{ fontSize: '2.5rem', marginBottom: '1rem' }}>📁</div>
              <p style={{ fontWeight: 500, fontSize: '1rem', marginBottom: '0.5rem', color: '#FAFAFA' }}>
                Drag & drop your CSV file here
              </p>
              <p style={{ color: '#71717A', marginBottom: '1rem', fontSize: '0.875rem' }}>or</p>
              <label htmlFor="file-upload" className="btn btn-secondary" style={{ cursor: 'pointer' }}>
                Browse Files
              </label>
            </div>
          )}
        </div>

        {/* Upload Result */}
        {uploadResult && (
          <UploadResultCard 
            result={uploadResult} 
            onDismiss={() => setUploadResult(null)}
          />
        )}

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
            className="btn btn-primary"
            style={{ flex: 1 }}
          >
            {uploading ? (
              <>
                <span className="spinner" style={{ marginRight: '0.5rem' }}></span>
                Processing...
              </>
            ) : (
              '🚀 Upload & Process'
            )}
          </button>

          {selectedFile && (
            <button
              onClick={() => {
                setSelectedFile(null);
                setUploadResult(null);
                if (fileInputRef.current) fileInputRef.current.value = '';
              }}
              disabled={uploading}
              className="btn btn-secondary"
            >
              Clear
            </button>
          )}
        </div>

        {/* Supported Formats */}
        <div
          style={{
            marginTop: '1.5rem',
            padding: '1rem',
            background: '#18181B',
            borderRadius: '8px',
          }}
        >
          <p style={{ fontSize: '0.8125rem', fontWeight: 500, marginBottom: '0.75rem', color: '#FAFAFA' }}>
            🏦 Supported Banks & Formats:
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.5rem' }}>
            {SUPPORTED_BANKS.filter(b => b.id !== 'auto' && b.id !== 'Generic').map((bank) => (
              <div
                key={bank.id}
                style={{
                  fontSize: '0.75rem',
                  color: '#A1A1AA',
                  padding: '0.5rem',
                  background: '#111113',
                  borderRadius: '6px',
                }}
              >
                <span style={{ fontWeight: 500, color: '#FAFAFA' }}>{bank.name}</span>
                <span style={{ opacity: 0.7 }}> - {bank.description}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Sample Files Section */}
      <div
        className="card"
        style={{
          maxWidth: '800px',
          marginTop: '1.5rem',
          background: '#111113',
          border: '1px dashed #27272A',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <div>
            <p style={{ fontWeight: 500, marginBottom: '0.25rem', color: '#FAFAFA', fontSize: '0.875rem' }}>📁 Sample Test Files</p>
            <p style={{ fontSize: '0.8125rem', color: '#71717A' }}>
              Load sample CSV files for testing different bank formats
            </p>
          </div>
          <button
            onClick={() => setShowSampleSelector(!showSampleSelector)}
            className="btn btn-secondary"
            style={{ padding: '0.5rem 1rem', fontSize: '0.8125rem' }}
          >
            {showSampleSelector ? 'Hide Samples' : 'Show Samples'}
          </button>
        </div>

        {showSampleSelector && (
          <div
            className="fade-in"
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: '0.75rem',
              marginTop: '1rem',
            }}
          >
            {Object.entries(SAMPLE_CSV_CONTENT).map(([bankId, sample]) => (
              <div
                key={bankId}
                style={{
                  padding: '1rem',
                  background: '#18181B',
                  borderRadius: '8px',
                  border: '1px solid #27272A',
                }}
              >
                <p style={{ fontWeight: 500, marginBottom: '0.5rem', color: '#FAFAFA', fontSize: '0.875rem' }}>
                  🏦 {bankId}
                </p>
                <p style={{ fontSize: '0.6875rem', color: '#71717A', marginBottom: '0.75rem' }}>
                  {sample.filename}
                </p>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    onClick={() => loadSampleFile(bankId)}
                    className="btn btn-primary"
                    style={{ flex: 1, padding: '0.375rem', fontSize: '0.75rem' }}
                  >
                    Load
                  </button>
                  <button
                    onClick={() => downloadSampleFile(bankId)}
                    className="btn btn-secondary"
                    style={{ padding: '0.375rem', fontSize: '0.75rem' }}
                  >
                    ⬇️
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
