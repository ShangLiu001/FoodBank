import React, { useState } from 'react';

const CommunityManagement = () => {
  const [activeView, setActiveView] = useState('volunteers');

  return (
    <div className="p-8">
      <header className="mb-6 flex justify-between items-end">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Community Management
          </h1>
          <p className="text-gray-500 text-sm">
            Manage users, donors, and beneficiaries
          </p>
        </div>
        <button className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">
          + Add New {activeView.slice(0, -1)}
        </button>
      </header>

      {/* View Switcher */}
      <div className="flex space-x-6 mb-6 text-sm font-semibold border-b border-gray-200">
        {['volunteers', 'donors', 'beneficiaries', 'staff'].map((view) => (
          <button
            key={view}
            onClick={() => setActiveView(view)}
            className={`pb-3 capitalize transition-all ${
              activeView === view
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            {view}
          </button>
        ))}
      </div>

      {/* Data Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="w-full text-left text-sm">
          <thead className="bg-gray-50 text-gray-600 uppercase text-xs font-bold">
            {activeView === 'volunteers' && (
              <tr>
                <th className="px-6 py-4">Name</th>
                <th className="px-6 py-4">Primary Branch</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4">Next Shift</th>
              </tr>
            )}
            {activeView === 'donors' && (
              <tr>
                <th className="px-6 py-4">Donor Name</th>
                <th className="px-6 py-4">Type</th>
                <th className="px-6 py-4">Email</th>
                <th className="px-6 py-4">Last Donation</th>
              </tr>
            )}
          </thead>
          <tbody className="divide-y divide-gray-100">
            {activeView === 'volunteers' ? (
              <tr>
                <td className="px-6 py-4 font-medium">Carol Lee</td>
                <td className="px-6 py-4 text-gray-600">Downtown Boston</td>
                <td className="px-6 py-4">
                  <span className="bg-green-100 text-green-700 px-2 py-1 rounded-full text-xs">
                    Active
                  </span>
                </td>
                <td className="px-6 py-4 text-gray-500">2026-04-19</td>
              </tr>
            ) : (
              <tr>
                <td className="px-6 py-4 font-medium text-gray-400" colSpan="4">
                  Fetching data from `{activeView}` table...
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Trigger/Constraint Highlight Box */}
      {activeView === 'volunteers' && (
        <div className="mt-8 bg-amber-50 border border-amber-200 p-4 rounded-lg">
          <h4 className="text-amber-800 font-bold text-xs uppercase mb-1">
            Database Logic Check
          </h4>
          <p className="text-amber-700 text-xs leading-relaxed">
            When assigning shifts, the{' '}
            <strong>trg_volunteer_shift_no_overlap_insert</strong> trigger is
            active. The system will reject any shift entry that overlaps with an
            existing time block for the same Volunteer ID.
          </p>
        </div>
      )}
    </div>
  );
};

export default CommunityManagement;
