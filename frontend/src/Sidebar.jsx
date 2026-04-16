import React from 'react';
import { Link, useLocation } from 'react-router-dom';

const Sidebar = () => {
  const location = useLocation();

  const navItems = [
    { name: 'Dashboard', path: '/dashboard' },
    { name: 'Operations', path: '/operations' },
    { name: 'Community', path: '/community' },
  ];

  return (
    <div className="w-64 bg-slate-900 text-white flex flex-col h-screen sticky top-0">
      <div className="p-6 text-2xl font-bold border-b border-slate-800">PrimaryFeed</div>
      <nav className="flex-1 p-4 space-y-2">
        {navItems.map((item) => (
          <Link
            key={item.name}
            to={item.path}
            className={`block p-3 rounded-md transition-colors ${
              location.pathname === item.path ? 'bg-blue-600' : 'hover:bg-slate-800'
            }`}
          >
            {item.name}
          </Link>
        ))}
        <Link to="/login" className="block p-3 hover:bg-slate-800 rounded-md mt-10 text-gray-400">
          Logout
        </Link>
      </nav>
      <div className="p-4 text-[10px] text-slate-500 uppercase tracking-widest border-t border-slate-800">
        Connected to GCP / MySQL
      </div>
    </div>
  );
};

export default Sidebar;