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
      <h1 className="mb-2">Upload Bank Settlement CSV</h1>
      <p style={{ marginBottom: '2rem', color: 'var(--text-secondary)' }}>
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
            border: `2px dashed ${dragActive ? 'var(--primary)' : 'var(--border)'}`,
            borderRadius: 'var(--radius-lg)',
            padding: '3rem',
            textAlign: 'center',
            background: dragActive ? 'rgba(0, 122, 255, 0.05)' : 'var(--bg-secondary)',
            transition: 'all 0.3s ease',
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
              <p style={{ fontWeight: 600, fontSize: '1.125rem', marginBottom: '0.25rem' }}>
                {selectedFile.name}
              </p>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                {(selectedFile.size / 1024).toFixed(2)} KB
              </p>
            </div>
          ) : (
            <div>
              <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📁</div>
              <p style={{ fontWeight: 600, fontSize: '1.125rem', marginBottom: '0.5rem' }}>
                Drag & drop your CSV file here
              </p>
              <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>or</p>
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
            marginTop: '2rem',
            padding: '1rem',
            background: 'var(--bg-secondary)',
            borderRadius: 'var(--radius-md)',
          }}
        >
          <p style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.75rem' }}>
            🏦 Supported Banks & Formats:
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.5rem' }}>
            {SUPPORTED_BANKS.filter(b => b.id !== 'auto' && b.id !== 'Generic').map((bank) => (
              <div
                key={bank.id}
                style={{
                  fontSize: '0.8125rem',
                  color: 'var(--text-secondary)',
                  padding: '0.5rem',
                  background: 'var(--bg-primary)',
                  borderRadius: 'var(--radius-sm)',
                }}
              >
                <span style={{ fontWeight: 500 }}>{bank.name}</span>
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
          background: 'var(--bg-secondary)',
          border: '1px dashed var(--border)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <div>
            <p style={{ fontWeight: 600, marginBottom: '0.25rem' }}>📁 Sample Test Files</p>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              Load sample CSV files for testing different bank formats
            </p>
          </div>
          <button
            onClick={() => setShowSampleSelector(!showSampleSelector)}
            className="btn btn-secondary"
            style={{ padding: '0.5rem 1rem', fontSize: '0.875rem' }}
          >
            {showSampleSelector ? 'Hide Samples' : 'Show Samples'}
          </button>
        </div>

        {showSampleSelector && (
          <div
            className="fade-in"
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '0.75rem',
              marginTop: '1rem',
            }}
          >
            {Object.entries(SAMPLE_CSV_CONTENT).map(([bankId, sample]) => (
              <div
                key={bankId}
                style={{
                  padding: '1rem',
                  background: 'var(--bg-primary)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border)',
                }}
              >
                <p style={{ fontWeight: 600, marginBottom: '0.5rem' }}>
                  🏦 {bankId}
                </p>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
                  {sample.filename}
                </p>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    onClick={() => loadSampleFile(bankId)}
                    className="btn btn-primary"
                    style={{ flex: 1, padding: '0.5rem', fontSize: '0.75rem' }}
                  >
                    Load
                  </button>
                  <button
                    onClick={() => downloadSampleFile(bankId)}
                    className="btn btn-secondary"
                    style={{ padding: '0.5rem', fontSize: '0.75rem' }}
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
