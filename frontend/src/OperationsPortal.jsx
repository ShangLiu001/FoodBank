import React, { useState } from 'react';

const OperationsPortal = () => {
  const [activeTab, setActiveTab] = useState('donation');

  return (
    <div className="p-8">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Operations Portal</h1>
        <p className="text-gray-500 text-sm">
          Execute Stored Procedures & Manage Stock
        </p>
      </header>

      {/* Tab Switcher */}
      <div className="flex space-x-4 mb-6 border-b border-gray-200">
        <button
          onClick={() => setActiveTab('donation')}
          className={`pb-2 px-4 font-medium transition-colors ${
            activeTab === 'donation'
              ? 'border-b-2 border-blue-600 text-blue-600'
              : 'text-gray-500'
          }`}
        >
          Record Donation
        </button>
        <button
          onClick={() => setActiveTab('distribution')}
          className={`pb-2 px-4 font-medium transition-colors ${
            activeTab === 'distribution'
              ? 'border-b-2 border-blue-600 text-blue-600'
              : 'text-gray-500'
          }`}
        >
          Record Distribution
        </button>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
        {/* Form Section */}
        <div className="xl:col-span-1 bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          {activeTab === 'donation' ? <DonationForm /> : <DistributionForm />}
        </div>

        {/* Live Inventory Section */}
        <div className="xl:col-span-2 bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="p-4 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
            <h3 className="font-bold text-gray-700 text-sm uppercase">
              Current Inventory (Branch #1)
            </h3>
            <button className="text-xs bg-white border border-gray-300 px-2 py-1 rounded hover:bg-gray-50">
              Refresh View
            </button>
          </div>
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-600">
              <tr>
                <th className="px-4 py-3 font-semibold">SKU</th>
                <th className="px-4 py-3 font-semibold">Item Name</th>
                <th className="px-4 py-3 font-semibold text-right">Quantity</th>
                <th className="px-4 py-3 font-semibold">Expiry Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              <tr>
                <td className="px-4 py-3 font-mono text-blue-600">SKU-001</td>
                <td className="px-4 py-3">Canned Chickpeas</td>
                <td className="px-4 py-3 text-right">142</td>
                <td className="px-4 py-3">2026-12-31</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

// ... DonationForm and DistributionForm remain the same as you had them ...
const DonationForm = () => (
  <form className="space-y-4">
    <h4 className="text-lg font-semibold text-gray-700 mb-2">New Donation</h4>
    <div>
      <label className="block text-xs font-bold text-gray-500 uppercase">
        Donor ID
      </label>
      <input
        type="number"
        className="w-full mt-1 border rounded-md p-2 text-sm"
        placeholder="e.g. 1"
      />
    </div>
    <div className="grid grid-cols-2 gap-4">
      <div>
        <label className="block text-xs font-bold text-gray-500 uppercase">
          Food SKU
        </label>
        <input
          type="text"
          className="w-full mt-1 border rounded-md p-2 text-sm"
          placeholder="SK-001"
        />
      </div>
      <div>
        <label className="block text-xs font-bold text-gray-500 uppercase">
          Quantity
        </label>
        <input
          type="number"
          className="w-full mt-1 border rounded-md p-2 text-sm"
          placeholder="50"
        />
      </div>
    </div>
    <div>
      <label className="block text-xs font-bold text-gray-500 uppercase">
        Expiry Date
      </label>
      <input
        type="date"
        className="w-full mt-1 border rounded-md p-2 text-sm"
      />
    </div>
    <button
      type="button"
      className="w-full bg-green-600 text-white py-2 rounded-md font-medium hover:bg-green-700 transition-colors"
    >
      Execute `record_donation`
    </button>
  </form>
);

const DistributionForm = () => (
  <form className="space-y-4">
    <h4 className="text-lg font-semibold text-gray-700 mb-2">
      New Distribution
    </h4>
    <div>
      <label className="block text-xs font-bold text-gray-500 uppercase">
        Beneficiary ID
      </label>
      <input
        type="number"
        className="w-full mt-1 border rounded-md p-2 text-sm"
        placeholder="e.g. 5"
      />
    </div>
    <div className="grid grid-cols-2 gap-4">
      <div>
        <label className="block text-xs font-bold text-gray-500 uppercase">
          Food SKU
        </label>
        <input
          type="text"
          className="w-full mt-1 border rounded-md p-2 text-sm"
          placeholder="SK-001"
        />
      </div>
      <div>
        <label className="block text-xs font-bold text-gray-500 uppercase">
          Quantity
        </label>
        <input
          type="number"
          className="w-full mt-1 border rounded-md p-2 text-sm"
          placeholder="10"
        />
      </div>
    </div>
    <button
      type="button"
      className="w-full bg-blue-600 text-white py-2 rounded-md font-medium hover:bg-blue-700 transition-colors"
    >
      Execute `record_distribution`
    </button>
    <p className="text-[10px] text-gray-400 italic">
      Uses FIFO logic to pull from earliest expiry batch.
    </p>
  </form>
);

export default OperationsPortal;
