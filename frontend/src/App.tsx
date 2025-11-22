import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { DisputeList } from './pages/DisputeList';
import { DisputeDetail } from './pages/DisputeDetail';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<DisputeList />} />
        <Route path="/disputes/:id" element={<DisputeDetail />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
