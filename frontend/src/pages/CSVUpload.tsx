import React, { useState, useRef } from 'react';
import api from '../api/client';

export const CSVUpload = () => {
  const [uploading, setUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedBank, setSelectedBank] = useState('GTBank');
  const [uploadResult, setUploadResult] = useState<{ success: boolean; message: string } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const banks = ['GTBank', 'Access', 'Zenith', 'FCMB', 'UBA', 'First Bank'];

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
      } else {
        alert('Please upload a CSV file');
      }
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    setUploading(true);
    setUploadResult(null);

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('bank', selectedBank);

    try {
      await api.post('/api/ingest/csv', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      setUploadResult({ success: true, message: 'CSV uploaded and processed successfully!' });
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (error: any) {
      setUploadResult({ 
        success: false, 
        message: error.response?.data?.message || 'Upload failed. Please try again.' 
      });
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="container fade-in">
      <h1 className="mb-2">Upload Bank Settlement CSV</h1>
      <p style={{ marginBottom: '2rem', color: 'var(--text-secondary)' }}>
        Upload bank settlement files to trigger reconciliation against internal ledger and payment provider data
      </p>

      <div className="card" style={{ maxWidth: '800px' }}>
        <div style={{ marginBottom: '1.5rem' }}>
          <label style={{ 
            display: 'block', 
            marginBottom: '0.5rem', 
            fontSize: '0.875rem',
            fontWeight: 600,
            color: 'var(--text-secondary)'
          }}>
            Select Bank
          </label>
          <select 
            value={selectedBank}
            onChange={(e) => setSelectedBank(e.target.value)}
            disabled={uploading}
            style={{
              width: '100%',
              padding: '0.875rem 1rem',
              border: '1.5px solid var(--border)',
              borderRadius: 'var(--radius-md)',
              fontSize: '1rem',
              fontFamily: 'inherit',
              background: 'var(--bg-primary)',
              color: 'var(--text-primary)',
              cursor: 'pointer'
            }}
          >
            {banks.map(bank => (
              <option key={bank} value={bank}>{bank}</option>
            ))}
          </select>
        </div>

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
            marginBottom: '1.5rem'
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
              <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}></div>
              <p style={{ fontWeight: 600, fontSize: '1.125rem', marginBottom: '0.25rem' }}>
                {selectedFile.name}
              </p>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                {(selectedFile.size / 1024).toFixed(2)} KB
              </p>
            </div>
          ) : (
            <div>
              <div style={{ fontSize: '3rem', marginBottom: '1rem' }}></div>
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

        {uploadResult && (
          <div 
            className="fade-in"
            style={{
              padding: '1rem',
              borderRadius: 'var(--radius-md)',
              marginBottom: '1.5rem',
              background: uploadResult.success ? 'rgba(52, 199, 89, 0.1)' : 'rgba(255, 59, 48, 0.1)',
              color: uploadResult.success ? 'var(--success)' : 'var(--danger)',
              fontWeight: 500
            }}
          >
            {uploadResult.message}
          </div>
        )}

        <div style={{ display: 'flex', gap: '1rem' }}>
          <button
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
            className="btn btn-primary"
            style={{ flex: 1 }}
          >
            {uploading ? 'Uploading...' : 'Upload & Process'}
          </button>
          
          {selectedFile && (
            <button
              onClick={() => {
                setSelectedFile(null);
                if (fileInputRef.current) fileInputRef.current.value = '';
              }}
              disabled={uploading}
              className="btn btn-secondary"
            >
              Clear
            </button>
          )}
        </div>

        <div style={{ 
          marginTop: '2rem', 
          padding: '1rem', 
          background: 'var(--bg-secondary)', 
          borderRadius: 'var(--radius-md)' 
        }}>
          <p style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.5rem' }}>
            Supported CSV Formats:
          </p>
          <ul style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.8, paddingLeft: '1.5rem' }}>
            <li>GTBank: PAYMENT_REF, AMOUNT, SETTLEMENT_DATE, STATUS</li>
            <li>Access: NARRATION, CREDIT_AMOUNT, VALUE_DATE</li>
            <li>Zenith: TXN_REF, AMOUNT, DATE (DD/MM/YYYY format)</li>
            <li>Other banks: Standard settlement format</li>
          </ul>
        </div>
      </div>
    </div>
  );
};
