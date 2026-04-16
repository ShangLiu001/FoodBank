import React, { useState, useEffect } from 'react';
import api from './api';

const StatCard = ({ label, value, color, loading }) => (
  <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
    <p className="text-sm font-medium text-gray-500">{label}</p>
    <p className={`text-3xl font-bold mt-2 ${color}`}>
      {loading ? <span className="text-gray-200 animate-pulse">—</span> : value}
    </p>
  </div>
);

const Dashboard = () => {
  const [inventory, setInventory]     = useState([]);
  const [expiring, setExpiring]       = useState([]);
  const [donations, setDonations]     = useState([]);
  const [volunteers, setVolunteers]   = useState([]);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState('');

  useEffect(() => {
    Promise.all([
      api.get('/api/inventory'),
      api.get('/api/inventory/expiring'),
      api.get('/api/donations'),
      api.get('/api/volunteers'),
    ])
      .then(([inv, exp, don, vol]) => {
        setInventory(inv);
        setExpiring(exp);
        setDonations(don);
        setVolunteers(vol);
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const totalUnits  = inventory.reduce((s, i) => s + (i.quantity || 0), 0);
  const activeVols  = volunteers.filter(v => v.user?.status === 1).length;
  const recent5     = [...donations]
    .sort((a, b) => new Date(b.donationDate) - new Date(a.donationDate))
    .slice(0, 5);

  const stats = [
    { label: 'Total Units in Stock',    value: totalUnits.toLocaleString(), color: 'text-blue-600' },
    { label: 'Active Volunteers',        value: activeVols,                  color: 'text-green-600' },
    { label: 'Expiring Soon (3 days)',   value: expiring.length,             color: 'text-red-600' },
    { label: 'Total Donations',          value: donations.length,            color: 'text-purple-600' },
  ];

  return (
    <div className="p-8">
      <header className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800">System Overview</h1>
        <p className="text-gray-500 text-sm">Live data from primaryfeed_db</p>
      </header>

      {error && (
        <div className="mb-6 bg-red-50 text-red-700 p-3 rounded-lg text-sm border border-red-200">{error}</div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map(s => <StatCard key={s.label} {...s} loading={loading} />)}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Expiring Inventory */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="p-4 border-b bg-red-50 flex justify-between items-center">
            <h3 className="font-bold text-red-700 text-sm uppercase">Expiring Within 3 Days</h3>
            <span className="text-xs bg-red-100 text-red-600 font-bold px-2 py-0.5 rounded-full">
              {expiring.length} item{expiring.length !== 1 ? 's' : ''}
            </span>
          </div>
          <div className="overflow-auto max-h-72">
            {loading ? (
              <div className="p-8 text-center text-gray-400 text-sm">Loading...</div>
            ) : expiring.length === 0 ? (
              <div className="p-8 text-center text-green-600 text-sm">No items expiring soon</div>
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
                  <tr>
                    <th className="px-4 py-3">Item</th>
                    <th className="px-4 py-3">Branch</th>
                    <th className="px-4 py-3 text-right">Qty</th>
                    <th className="px-4 py-3">Expires</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {expiring.map(item => (
                    <tr key={item.inventoryId} className="hover:bg-red-50">
                      <td className="px-4 py-3 font-medium">{item.foodItem?.foodName}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{item.branch?.branchName}</td>
                      <td className="px-4 py-3 text-right font-mono font-bold">{item.quantity}</td>
                      <td className="px-4 py-3 text-red-600 font-mono text-xs">{item.expiryDate?.slice(0, 10)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Recent Donations */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="p-4 border-b bg-gray-50 flex justify-between items-center">
            <h3 className="font-bold text-gray-700 text-sm uppercase">Recent Donations</h3>
            <span className="text-xs text-gray-500">{donations.length} total</span>
          </div>
          <div className="overflow-auto max-h-72">
            {loading ? (
              <div className="p-8 text-center text-gray-400 text-sm">Loading...</div>
            ) : recent5.length === 0 ? (
              <div className="p-8 text-center text-gray-400 text-sm">No donations recorded</div>
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
                  <tr>
                    <th className="px-4 py-3">#</th>
                    <th className="px-4 py-3">Donor</th>
                    <th className="px-4 py-3">Branch</th>
                    <th className="px-4 py-3">Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {recent5.map(d => (
                    <tr key={d.donationId} className="hover:bg-gray-50">
                      <td className="px-4 py-3 text-gray-400 font-mono text-xs">#{d.donationId}</td>
                      <td className="px-4 py-3 font-medium">{d.donor?.donorName}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{d.branch?.branchName}</td>
                      <td className="px-4 py-3 text-gray-400 font-mono text-xs">{d.donationDate?.slice(0, 10)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
