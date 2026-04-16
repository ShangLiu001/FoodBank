import React, { useState } from 'react';
import api from './api';

// ─── Query definitions ────────────────────────────────────────────────────────
const QUERIES = [
  { id: 1,  title: 'Items at Branch',               params: [{ key: 'branchId',      label: 'Branch ID',      type: 'number' }] },
  { id: 2,  title: 'Expiring in 3 Days',            params: [] },
  { id: 3,  title: 'Total Stock — All Branches',    params: [] },
  { id: 4,  title: 'Items by Category',             params: [] },
  { id: 5,  title: 'Top Distributing Branch (last month)', params: [] },
  { id: 6,  title: 'Volunteers at Branch',          params: [{ key: 'branchId',      label: 'Branch ID',      type: 'number' }] },
  { id: 7,  title: 'Volunteer Hours (last 30 days)',params: [] },
  { id: 8,  title: 'Volunteers in Time Window',     params: [
    { key: 'branchId',   label: 'Branch ID',   type: 'number' },
    { key: 'date',       label: 'Date',        type: 'date' },
    { key: 'startTime',  label: 'Start Time',  type: 'time' },
    { key: 'endTime',    label: 'End Time',    type: 'time' },
  ]},
  { id: 9,  title: 'Distribution History — Beneficiary', params: [{ key: 'beneficiaryId', label: 'Beneficiary ID', type: 'number' }] },
  { id: 10, title: 'Beneficiaries Served This Week', params: [] },
  { id: 11, title: 'Donations from Donor',          params: [{ key: 'donorId',       label: 'Donor ID',       type: 'number' }] },
  { id: 12, title: 'Stock vs Distributed (Net Surplus)', params: [] },
  { id: 13, title: 'Items Below Quantity Threshold',params: [{ key: 'threshold',     label: 'Threshold',      type: 'number' }] },
  { id: 14, title: 'All Users & Roles',             params: [] },
  { id: 15, title: 'Top Donated/Distributed Categories', params: [] },
  { id: 16, title: 'Daily Distribution Totals (14 days)', params: [] },
  { id: 17, title: 'Volunteer-to-Distribution Ratio', params: [] },
];

const buildUrl = (query, paramValues) => {
  const base   = `/api/reports/${query.id}`;
  const search = new URLSearchParams();
  for (const p of query.params) {
    let v = paramValues[p.key] || '';
    if (p.type === 'time' && v && v.length === 5) v = `${v}:00`; // HH:mm → HH:mm:ss
    if (v) search.set(p.key, v);
  }
  const qs = search.toString();
  return qs ? `${base}?${qs}` : base;
};

// ─── Generic results table ────────────────────────────────────────────────────
const ResultTable = ({ data }) => {
  if (!data || data.length === 0)
    return <div className="p-10 text-center text-gray-400 text-sm">No results found.</div>;
  const cols = Object.keys(data[0]);
  return (
    <div className="overflow-auto max-h-[calc(100vh-300px)]">
      <table className="w-full text-left text-sm">
        <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
          <tr>{cols.map(c => <th key={c} className="px-4 py-3 whitespace-nowrap">{c.replace(/_/g, ' ')}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {data.map((row, i) => (
            <tr key={i} className="hover:bg-gray-50">
              {cols.map(c => (
                <td key={c} className="px-4 py-3 font-mono text-xs text-gray-700">
                  {row[c] !== null && row[c] !== undefined ? String(row[c]) : <span className="text-gray-300">—</span>}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

// ─── Main Component ───────────────────────────────────────────────────────────
const Reports = () => {
  const [selected, setSelected]   = useState(QUERIES[0]);
  const [paramValues, setParams]  = useState({});
  const [results, setResults]     = useState(null);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');

  const selectQuery = q => {
    setSelected(q);
    setParams({});
    setResults(null);
    setError('');
  };

  const runQuery = async e => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setResults(null);
    try {
      const url  = buildUrl(selected, paramValues);
      const data = await api.get(url);
      setResults(Array.isArray(data) ? data : [data]);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex h-full">
      {/* Query list */}
      <aside className="w-64 shrink-0 bg-white border-r border-gray-200 overflow-y-auto">
        <div className="p-4 border-b border-gray-100">
          <h2 className="text-xs font-bold text-gray-500 uppercase tracking-wider">17 Insight Queries</h2>
        </div>
        <nav className="p-2 space-y-0.5">
          {QUERIES.map(q => (
            <button key={q.id} onClick={() => selectQuery(q)}
              className={`w-full text-left px-3 py-2.5 rounded-lg text-sm transition-colors ${
                selected.id === q.id
                  ? 'bg-blue-50 text-blue-700 font-medium'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}>
              <span className="text-xs font-mono text-gray-400 mr-2">Q{q.id}</span>
              {q.title}
            </button>
          ))}
        </nav>
      </aside>

      {/* Query runner */}
      <main className="flex-1 p-8 overflow-auto">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-800">
            <span className="text-blue-500 font-mono text-xl mr-2">Q{selected.id}</span>
            {selected.title}
          </h1>
        </div>

        <form onSubmit={runQuery} className="mb-6">
          {selected.params.length > 0 && (
            <div className="bg-white border border-gray-200 rounded-xl p-5 mb-4">
              <div className="grid grid-cols-2 gap-4">
                {selected.params.map(p => (
                  <div key={p.key}>
                    <label className="block text-xs font-bold text-gray-500 uppercase mb-1">{p.label}</label>
                    <input
                      type={p.type}
                      required
                      className="w-full border rounded-lg p-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={paramValues[p.key] || ''}
                      onChange={e => setParams(prev => ({ ...prev, [p.key]: e.target.value }))}
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          <button type="submit" disabled={loading}
            className="bg-blue-600 text-white px-6 py-2.5 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 text-sm">
            {loading ? 'Running…' : 'Run Query'}
          </button>
        </form>

        {error && (
          <div className="bg-red-50 text-red-700 p-4 rounded-xl text-sm border border-red-200 mb-4">{error}</div>
        )}

        {results !== null && (
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div className="px-4 py-3 border-b bg-gray-50 flex justify-between items-center">
              <span className="text-xs font-bold text-gray-500 uppercase">Results</span>
              <span className="text-xs text-gray-400">{results.length} row{results.length !== 1 ? 's' : ''}</span>
            </div>
            <ResultTable data={results} />
          </div>
        )}

        {results === null && !loading && !error && (
          <div className="border-2 border-dashed border-gray-200 rounded-xl p-12 text-center text-gray-400 text-sm">
            {selected.params.length > 0
              ? 'Fill in the parameters above, then click Run Query.'
              : 'Click Run Query to fetch results.'}
          </div>
        )}
      </main>
    </div>
  );
};

export default Reports;
