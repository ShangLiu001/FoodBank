import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

const Sidebar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const role = localStorage.getItem('role');
  const name = localStorage.getItem('name');
  const isStaff = role === 'ROLE_STAFF';

  const navItems = [
    { name: 'Dashboard',  path: '/dashboard',  staffOnly: true },
    { name: 'Operations', path: '/operations',  staffOnly: false },
    { name: 'Community',  path: '/community',   staffOnly: false },
    { name: 'Reports',    path: '/reports',     staffOnly: true },
  ].filter(item => !item.staffOnly || isStaff);

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  return (
    <div className="w-64 bg-slate-900 text-white flex flex-col h-screen sticky top-0 shrink-0">
      <div className="p-6 border-b border-slate-800">
        <div className="text-2xl font-bold tracking-tight">PrimaryFeed</div>
        {name && <div className="text-sm text-slate-300 mt-1 truncate">{name}</div>}
        <div className="text-xs text-blue-400 mt-0.5">{isStaff ? 'Staff' : 'Volunteer'}</div>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {navItems.map(item => (
          <Link
            key={item.name}
            to={item.path}
            className={`block px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
              location.pathname === item.path
                ? 'bg-blue-600 text-white'
                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
            }`}
          >
            {item.name}
          </Link>
        ))}
      </nav>

      <div className="p-4 border-t border-slate-800">
        <button
          onClick={handleLogout}
          className="w-full text-left px-4 py-2.5 text-sm text-slate-400 hover:bg-slate-800 hover:text-white rounded-lg transition-colors"
        >
          Sign Out
        </button>
        <div className="text-[10px] text-slate-600 uppercase tracking-widest mt-3 px-1">
          GCP · MySQL · primaryfeed
        </div>
      </div>
    </div>
  );
};

export default Sidebar;
