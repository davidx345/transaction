import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom';

interface DisputeDetail {
  id: string;
  transactionRef: string;
  state: string;
  confidenceScore: number;
  rulesFired: { rule: string; weight: number }[];
}

export const DisputeDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [dispute, setDispute] = useState<DisputeDetail | null>(null);
  const [reason, setReason] = useState('');

  useEffect(() => {
    axios.get(`http://localhost:8080/api/disputes/${id}`)
      .then(res => setDispute(res.data))
      .catch(err => console.error(err));
  }, [id]);

  const handleAction = (action: 'approve' | 'reject') => {
    axios.post(`http://localhost:8080/api/disputes/${id}/${action}`, { reason })
      .then(() => {
        alert(`Dispute ${action}d successfully`);
        navigate('/');
      })
      .catch(err => alert('Error: ' + err.message));
  };

  if (!dispute) return <div>Loading...</div>;

  return (
    <div style={{ padding: '20px' }}>
      <h2>Dispute Details: {dispute.transactionRef}</h2>
      <p><strong>Status:</strong> {dispute.state}</p>
      <p><strong>Confidence Score:</strong> {dispute.confidenceScore}</p>
      
      <h3>Rules Fired:</h3>
      <ul>
        {dispute.rulesFired.map((r, idx) => (
          <li key={idx}>{r.rule} (Weight: {r.weight})</li>
        ))}
      </ul>

      <div style={{ marginTop: '20px' }}>
        <textarea 
          placeholder="Enter reason for decision..." 
          value={reason}
          onChange={e => setReason(e.target.value)}
          rows={4}
          style={{ width: '100%', marginBottom: '10px' }}
        />
        <button onClick={() => handleAction('approve')} style={{ marginRight: '10px', backgroundColor: 'green', color: 'white' }}>
          Approve Refund
        </button>
        <button onClick={() => handleAction('reject')} style={{ backgroundColor: 'red', color: 'white' }}>
          Reject Dispute
        </button>
      </div>
    </div>
  );
};
