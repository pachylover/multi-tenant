import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminStoragePage } from './AdminStoragePage';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/admin/storage" replace />} />
      <Route path="/admin/storage" element={<AdminStoragePage />} />
      <Route path="*" element={<div style={{ padding: 24 }}>Not found</div>} />
    </Routes>
  );
}

