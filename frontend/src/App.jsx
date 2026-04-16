import React from 'react';
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
} from 'react-router-dom';

// 1. Import the Sidebar
import Sidebar from './Sidebar';

// 2. Import the Page components
import LoginPage from './LoginPage';
import Dashboard from './Dashboard';
import OperationsPortal from './OperationsPortal';
import CommunityManagement from './CommunityManagement';

import './App.css';

// A small helper component to keep the Sidebar on every page except Login
const Layout = ({ children }) => (
  <div className="flex h-screen bg-gray-100">
    <Sidebar />
    <div className="flex-1 overflow-auto">{children}</div>
  </div>
);

function App() {
  return (
    <Router>
      <Routes>
        {/* Public Route: No Sidebar here */}
        <Route path="/login" element={<LoginPage />} />

        {/* Private Routes: Wrapped in the Layout to show the Sidebar */}
        <Route
          path="/dashboard"
          element={
            <Layout>
              <Dashboard />
            </Layout>
          }
        />
        <Route
          path="/operations"
          element={
            <Layout>
              <OperationsPortal />
            </Layout>
          }
        />
        <Route
          path="/community"
          element={
            <Layout>
              <CommunityManagement />
            </Layout>
          }
        />

        {/* Default redirect */}
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
