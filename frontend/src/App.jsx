import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import LoginPage from './LoginPage';
import Dashboard from './Dashboard';
import OperationsPortal from './OperationsPortal';
import CommunityManagement from './CommunityManagement';
import Reports from './Reports';
import './App.css';

const Layout = ({ children }) => (
  <div className="flex h-screen bg-gray-100">
    <Sidebar />
    <div className="flex-1 overflow-auto">{children}</div>
  </div>
);

const PrivateRoute = ({ children }) => {
  const token = localStorage.getItem('token');
  return token ? children : <Navigate to="/login" replace />;
};

const StaffRoute = ({ children }) => {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');
  if (!token) return <Navigate to="/login" replace />;
  if (role !== 'ROLE_STAFF') return <Navigate to="/operations" replace />;
  return children;
};

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={
          <StaffRoute><Layout><Dashboard /></Layout></StaffRoute>
        } />
        <Route path="/operations" element={
          <PrivateRoute><Layout><OperationsPortal /></Layout></PrivateRoute>
        } />
        <Route path="/community" element={
          <PrivateRoute><Layout><CommunityManagement /></Layout></PrivateRoute>
        } />
        <Route path="/reports" element={
          <StaffRoute><Layout><Reports /></Layout></StaffRoute>
        } />
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
