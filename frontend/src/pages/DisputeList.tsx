import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';

interface Dispute {
  id: string;
  transactionRef: string;
  state: string;
  confidenceScore: number;
  createdAt: string;
}

export const DisputeList = () => {
  const [disputes, setDisputes] = useState<Dispute[]>([]);

  useEffect(() => {
    axios.get('http://localhost:8080/api/disputes')
      .then(res => setDisputes(res.data))
      .catch(err => console.error(err));
  }, []);

  return (
    <div style={{ padding: '20px' }}>
      <h1>Dispute Triage Dashboard</h1>
      <table border={1} cellPadding={10} style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th>Reference</th>
            <th>State</th>
            <th>Confidence Score</th>
            <th>Created At</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {disputes.map(d => (
            <tr key={d.id}>
              <td>{d.transactionRef}</td>
              <td>{d.state}</td>
              <td style={{ 
                color: d.confidenceScore > 90 ? 'red' : 'black',
                fontWeight: d.confidenceScore > 90 ? 'bold' : 'normal'
              }}>
                {d.confidenceScore}
              </td>
              <td>{new Date(d.createdAt).toLocaleString()}</td>
              <td>
                <Link to={`/disputes/${d.id}`}>View Details</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
