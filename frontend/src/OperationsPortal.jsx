import React, { useState, useEffect, useCallback } from 'react';
import api from './api';

// ─── Shared UI helpers ────────────────────────────────────────────────────────
const Modal = ({ title, onClose, wide, children }) => (
  <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div className={`bg-white rounded-xl shadow-2xl flex flex-col max-h-[90vh] w-full ${wide ? 'max-w-3xl' : 'max-w-2xl'}`}>
      <div className="flex justify-between items-center px-6 py-4 border-b shrink-0">
        <h3 className="font-bold text-gray-800 text-lg">{title}</h3>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-700 text-2xl leading-none">×</button>
      </div>
      <div className="p-6 overflow-y-auto">{children}</div>
    </div>
  </div>
);

const Err = ({ msg }) => msg
  ? <div className="bg-red-50 text-red-700 p-3 rounded-lg text-sm border border-red-200 mb-4">{msg}</div>
  : null;

const Lbl = ({ text, children }) => (
  <div>
    <label className="block text-xs font-bold text-gray-500 uppercase mb-1">{text}</label>
    {children}
  </div>
);
const inp = "w-full border rounded-lg p-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500";

// ─── INVENTORY TAB ────────────────────────────────────────────────────────────
const InventoryTab = ({ inventory, outOfStock, loading, onRefresh }) => {
  const [q, setQ]               = useState('');
  const [showOOS, setShowOOS]   = useState(false);
  const [editing, setEditing]   = useState(null);
  const [editForm, setEditForm] = useState({});
  const [saving, setSaving]     = useState(false);
  const [err, setErr]           = useState('');

  const today = new Date();
  const soon  = new Date(today); soon.setDate(today.getDate() + 7);

  const source = showOOS ? outOfStock : inventory;
  const rows   = source.filter(i => {
    if (!q) return true;
    return (i.foodItem?.foodName || '').toLowerCase().includes(q.toLowerCase())
        || (i.foodItem?.sku || '').toLowerCase().includes(q.toLowerCase())
        || (i.branch?.branchName || '').toLowerCase().includes(q.toLowerCase());
  });

  const openEdit = item => {
    setEditing(item);
    setEditForm({
      quantity:   item.quantity,
      unit:       item.unit || '',
      expiryDate: item.expiryDate ? item.expiryDate.slice(0, 10) : '',
    });
    setErr('');
  };

  const handleSave = async e => {
    e.preventDefault();
    setSaving(true); setErr('');
    try {
      await api.put(`/api/inventory/${editing.inventoryId}`, {
        foodItem:   { sku: editing.foodItem.sku },
        branch:     { branchId: editing.branch.branchId },
        quantity:   parseInt(editForm.quantity),
        unit:       editForm.unit,
        expiryDate: editForm.expiryDate ? `${editForm.expiryDate}T00:00:00` : null,
      });
      setEditing(null);
      onRefresh();
    } catch (e) { setErr(e.message); } finally { setSaving(false); }
  };

  const handleDelete = async id => {
    if (!window.confirm('Delete this inventory record?')) return;
    try { await api.delete(`/api/inventory/${id}`); onRefresh(); }
    catch (e) { alert(e.message); }
  };

  return (
    <div>
      <div className="flex gap-3 mb-4 items-center">
        <input type="text" placeholder="Filter by name, SKU, or branch…"
          className="flex-1 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          value={q} onChange={e => setQ(e.target.value)} />
        <button onClick={() => { setShowOOS(p => !p); setQ(''); }}
          className={`px-4 py-2 rounded-lg text-sm font-medium border transition-colors ${
            showOOS ? 'bg-red-600 text-white border-red-600' : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
          }`}>
          {showOOS ? 'Showing Out-of-Stock' : 'Show Out-of-Stock'}
        </button>
        <span className="text-xs text-gray-400 whitespace-nowrap">{rows.length} records</span>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-auto max-h-[60vh]">
          {loading ? <div className="p-10 text-center text-gray-400">Loading…</div> : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
                <tr>
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">SKU</th>
                  <th className="px-4 py-3">Item Name</th>
                  <th className="px-4 py-3">Branch</th>
                  <th className="px-4 py-3 text-right">Qty</th>
                  <th className="px-4 py-3">Unit</th>
                  <th className="px-4 py-3">Expiry</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map(item => {
                  const exp       = item.expiryDate ? new Date(item.expiryDate) : null;
                  const isExpired = exp && exp < today;
                  const isExpiring= exp && exp <= soon && !isExpired;
                  return (
                    <tr key={item.inventoryId} className={isExpired ? 'bg-red-50' : isExpiring ? 'bg-amber-50' : 'hover:bg-gray-50'}>
                      <td className="px-4 py-3 text-gray-400 font-mono text-xs">{item.inventoryId}</td>
                      <td className="px-4 py-3 font-mono text-blue-600 text-xs">{item.foodItem?.sku}</td>
                      <td className="px-4 py-3 font-medium">{item.foodItem?.foodName}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{item.branch?.branchName}</td>
                      <td className="px-4 py-3 text-right font-mono font-bold">{item.quantity}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{item.unit}</td>
                      <td className="px-4 py-3 font-mono text-xs">{item.expiryDate?.slice(0, 10)}</td>
                      <td className="px-4 py-3">
                        {isExpired    ? <span className="bg-red-100 text-red-700 px-2 py-0.5 rounded-full text-xs font-bold">EXPIRED</span>
                        : isExpiring  ? <span className="bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full text-xs font-bold">EXPIRING</span>
                        : item.quantity === 0 ? <span className="bg-gray-100 text-gray-400 px-2 py-0.5 rounded-full text-xs">OUT</span>
                        : <span className="bg-green-100 text-green-700 px-2 py-0.5 rounded-full text-xs">OK</span>}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex gap-3">
                          <button onClick={() => openEdit(item)} className="text-blue-500 hover:text-blue-700 text-xs font-medium">Edit</button>
                          <button onClick={() => handleDelete(item.inventoryId)} className="text-red-400 hover:text-red-600 text-xs font-medium">Delete</button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {editing && (
        <Modal title={`Edit Inventory #${editing.inventoryId}`} onClose={() => setEditing(null)}>
          <div className="mb-4 text-sm text-gray-600 bg-gray-50 p-3 rounded-lg">
            <span className="font-mono text-blue-600">{editing.foodItem?.sku}</span> · {editing.foodItem?.foodName} · {editing.branch?.branchName}
          </div>
          <form onSubmit={handleSave} className="space-y-4">
            <Err msg={err} />
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="Quantity"><input type="number" required min="0" className={inp}
                value={editForm.quantity} onChange={e => setEditForm(p => ({ ...p, quantity: e.target.value }))} /></Lbl>
              <Lbl text="Unit"><input className={inp} value={editForm.unit}
                onChange={e => setEditForm(p => ({ ...p, unit: e.target.value }))} /></Lbl>
            </div>
            <Lbl text="Expiry Date"><input type="date" className={inp} value={editForm.expiryDate}
              onChange={e => setEditForm(p => ({ ...p, expiryDate: e.target.value }))} /></Lbl>
            <div className="flex gap-3 pt-1">
              <button type="submit" disabled={saving} className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50">
                {saving ? 'Saving…' : 'Save Changes'}
              </button>
              <button type="button" onClick={() => setEditing(null)} className="px-5 py-2.5 border rounded-lg text-gray-600 hover:bg-gray-50">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

// ─── DONATIONS TAB ────────────────────────────────────────────────────────────
const DonationsTab = ({ donations, donors, foodItems, branches, loading, onRefresh }) => {
  const [showModal, setShowModal]       = useState(false);
  const [expandedId, setExpandedId]     = useState(null);
  const [expandedItems, setExpandedItems] = useState([]);
  const [itemsLoading, setItemsLoading] = useState(false);
  const [err, setErr]                   = useState('');
  const [submitting, setSubmitting]     = useState(false);
  const [form, setForm]                 = useState({ donorId: '', branchId: '', donationDate: new Date().toISOString().slice(0, 10) });
  const [items, setItems]               = useState([{ foodSku: '', quantity: '', unit: 'unit', expiryDate: '' }]);

  const userId = parseInt(localStorage.getItem('userId'), 10);

  const toggleExpand = async id => {
    if (expandedId === id) { setExpandedId(null); return; }
    setExpandedId(id); setItemsLoading(true); setExpandedItems([]);
    try { setExpandedItems(await api.get(`/api/donation-items/donation/${id}`)); }
    catch { /* silently fail */ } finally { setItemsLoading(false); }
  };

  const handleDelete = async id => {
    if (!window.confirm('Delete this donation and all its items?')) return;
    try { await api.delete(`/api/donations/${id}`); if (expandedId === id) setExpandedId(null); onRefresh(); }
    catch (e) { alert(e.message); }
  };

  const close = () => { setShowModal(false); setErr(''); };
  const addRow  = () => setItems(p => [...p, { foodSku: '', quantity: '', unit: 'unit', expiryDate: '' }]);
  const dropRow = i  => setItems(p => p.filter((_, idx) => idx !== i));
  const setRow  = (i, f, v) => setItems(p => p.map((r, idx) => idx === i ? { ...r, [f]: v } : r));

  const handleSubmit = async e => {
    e.preventDefault(); setSubmitting(true); setErr('');
    try {
      const donation = await api.post('/api/donations', {
        branch: { branchId: parseInt(form.branchId) },
        donor:  { donorId: parseInt(form.donorId) },
        user:   { userId },
        donationDate: `${form.donationDate}T00:00:00`,
      });
      for (const item of items) {
        if (!item.foodSku || !item.quantity) continue;
        await api.post('/api/donation-items', {
          donation: { donationId: donation.donationId },
          foodItem: { sku: item.foodSku },
          quantity: parseInt(item.quantity),
          unit:     item.unit || 'unit',
          expiryDate: item.expiryDate ? `${item.expiryDate}T00:00:00` : null,
        });
      }
      close();
      setForm({ donorId: '', branchId: '', donationDate: new Date().toISOString().slice(0, 10) });
      setItems([{ foodSku: '', quantity: '', unit: 'unit', expiryDate: '' }]);
      onRefresh();
    } catch (e) { setErr(e.message); } finally { setSubmitting(false); }
  };

  const sorted = [...donations].sort((a, b) => new Date(b.donationDate) - new Date(a.donationDate));

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <span className="text-sm text-gray-500">{donations.length} donations · click a row to see items</span>
        <button onClick={() => setShowModal(true)} className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-green-700">+ New Donation</button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-auto max-h-[60vh]">
          {loading ? <div className="p-10 text-center text-gray-400">Loading…</div> : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
                <tr>
                  <th className="px-4 py-3 w-8"></th>
                  <th className="px-4 py-3">#</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Donor</th>
                  <th className="px-4 py-3">Branch</th>
                  <th className="px-4 py-3">Recorded By</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {sorted.map(d => (
                  <React.Fragment key={d.donationId}>
                    <tr className="border-t border-gray-100 hover:bg-gray-50 cursor-pointer" onClick={() => toggleExpand(d.donationId)}>
                      <td className="px-4 py-3 text-gray-400 text-xs">{expandedId === d.donationId ? '▾' : '▸'}</td>
                      <td className="px-4 py-3 text-gray-400 font-mono text-xs">#{d.donationId}</td>
                      <td className="px-4 py-3 font-mono text-xs">{d.donationDate?.slice(0, 10)}</td>
                      <td className="px-4 py-3 font-medium">{d.donor?.donorName}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{d.branch?.branchName}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{d.user ? `${d.user.firstName} ${d.user.lastName}` : '—'}</td>
                      <td className="px-4 py-3" onClick={e => e.stopPropagation()}>
                        <button onClick={() => handleDelete(d.donationId)} className="text-red-400 hover:text-red-600 text-xs font-medium">Delete</button>
                      </td>
                    </tr>
                    {expandedId === d.donationId && (
                      <tr className="bg-blue-50 border-t border-blue-100">
                        <td colSpan={7} className="px-8 py-3">
                          {itemsLoading ? <div className="text-gray-400 text-xs py-2">Loading items…</div> :
                           expandedItems.length === 0 ? <div className="text-gray-400 text-xs py-2 italic">No items recorded for this donation.</div> : (
                            <table className="w-full text-xs">
                              <thead className="text-gray-500 uppercase">
                                <tr>
                                  <th className="pr-6 py-1 font-semibold">SKU</th>
                                  <th className="pr-6 py-1 font-semibold">Food Name</th>
                                  <th className="pr-6 py-1 font-semibold text-right">Qty</th>
                                  <th className="pr-6 py-1 font-semibold">Unit</th>
                                  <th className="py-1 font-semibold">Expiry</th>
                                </tr>
                              </thead>
                              <tbody>
                                {expandedItems.map(item => (
                                  <tr key={item.donationItemId} className="border-t border-blue-100">
                                    <td className="pr-6 py-1 font-mono text-blue-600">{item.foodItem?.sku}</td>
                                    <td className="pr-6 py-1">{item.foodItem?.foodName}</td>
                                    <td className="pr-6 py-1 text-right font-mono font-bold">{item.quantity}</td>
                                    <td className="pr-6 py-1 text-gray-500">{item.unit}</td>
                                    <td className="py-1 font-mono text-gray-500">{item.expiryDate?.slice(0, 10)}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          )}
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {showModal && (
        <Modal title="New Donation" onClose={close}>
          <form onSubmit={handleSubmit} className="space-y-5">
            <Err msg={err} />
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="Donor">
                <select required className={inp} value={form.donorId} onChange={e => setForm(p => ({ ...p, donorId: e.target.value }))}>
                  <option value="">Select donor…</option>
                  {donors.map(d => <option key={d.donorId} value={d.donorId}>{d.donorName}</option>)}
                </select>
              </Lbl>
              <Lbl text="Branch">
                <select required className={inp} value={form.branchId} onChange={e => setForm(p => ({ ...p, branchId: e.target.value }))}>
                  <option value="">Select branch…</option>
                  {branches.map(b => <option key={b.branchId} value={b.branchId}>{b.branchName}</option>)}
                </select>
              </Lbl>
            </div>
            <Lbl text="Date">
              <input type="date" required className={inp} value={form.donationDate}
                onChange={e => setForm(p => ({ ...p, donationDate: e.target.value }))} />
            </Lbl>
            <div>
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-bold text-gray-500 uppercase">Line Items</span>
                <button type="button" onClick={addRow} className="text-xs text-blue-600 hover:underline">+ add row</button>
              </div>
              <div className="grid grid-cols-12 gap-1 text-xs text-gray-400 px-1 mb-1">
                <span className="col-span-4">Food SKU</span><span className="col-span-2">Qty</span>
                <span className="col-span-2">Unit</span><span className="col-span-3">Expiry</span>
              </div>
              {items.map((item, i) => (
                <div key={i} className="grid grid-cols-12 gap-1 items-center mb-1">
                  <div className="col-span-4">
                    <select required className="w-full border rounded p-1.5 text-xs" value={item.foodSku} onChange={e => setRow(i, 'foodSku', e.target.value)}>
                      <option value="">Select SKU…</option>
                      {foodItems.map(fi => <option key={fi.sku} value={fi.sku}>{fi.sku} — {fi.foodName}</option>)}
                    </select>
                  </div>
                  <div className="col-span-2"><input type="number" required min="1" placeholder="Qty" className="w-full border rounded p-1.5 text-xs" value={item.quantity} onChange={e => setRow(i, 'quantity', e.target.value)} /></div>
                  <div className="col-span-2"><input placeholder="unit" className="w-full border rounded p-1.5 text-xs" value={item.unit} onChange={e => setRow(i, 'unit', e.target.value)} /></div>
                  <div className="col-span-3"><input type="date" className="w-full border rounded p-1.5 text-xs" value={item.expiryDate} onChange={e => setRow(i, 'expiryDate', e.target.value)} /></div>
                  <div className="col-span-1 text-center">{items.length > 1 && <button type="button" onClick={() => dropRow(i)} className="text-red-400 hover:text-red-600 text-lg leading-none">×</button>}</div>
                </div>
              ))}
            </div>
            <div className="flex gap-3 pt-1">
              <button type="submit" disabled={submitting} className="flex-1 bg-green-600 text-white py-2.5 rounded-lg font-medium hover:bg-green-700 disabled:opacity-50">{submitting ? 'Saving…' : 'Record Donation'}</button>
              <button type="button" onClick={close} className="px-5 py-2.5 border rounded-lg text-gray-600 hover:bg-gray-50">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

// ─── DISTRIBUTIONS TAB ────────────────────────────────────────────────────────
const DistributionsTab = ({ distributions, eligibleBeneficiaries, inventory, branches, loading, onRefresh }) => {
  const [showModal, setShowModal]         = useState(false);
  const [expandedId, setExpandedId]       = useState(null);
  const [expandedItems, setExpandedItems] = useState([]);
  const [itemsLoading, setItemsLoading]   = useState(false);
  const [err, setErr]                     = useState('');
  const [submitting, setSubmitting]       = useState(false);
  const [form, setForm]                   = useState({ beneficiaryId: '', branchId: '', distributionDate: new Date().toISOString().slice(0, 10) });
  const [items, setItems]                 = useState([{ inventoryId: '', quantity: '' }]);

  const userId    = parseInt(localStorage.getItem('userId'), 10);
  const available = inventory.filter(i => i.quantity > 0);

  const toggleExpand = async id => {
    if (expandedId === id) { setExpandedId(null); return; }
    setExpandedId(id); setItemsLoading(true); setExpandedItems([]);
    try { setExpandedItems(await api.get(`/api/distribution-items/distribution/${id}`)); }
    catch { /* silently fail */ } finally { setItemsLoading(false); }
  };

  const handleDelete = async id => {
    if (!window.confirm('Delete this distribution and all its items?')) return;
    try { await api.delete(`/api/distributions/${id}`); if (expandedId === id) setExpandedId(null); onRefresh(); }
    catch (e) { alert(e.message); }
  };

  const close   = () => { setShowModal(false); setErr(''); };
  const addRow  = () => setItems(p => [...p, { inventoryId: '', quantity: '' }]);
  const dropRow = i  => setItems(p => p.filter((_, idx) => idx !== i));
  const setRow  = (i, f, v) => setItems(p => p.map((r, idx) => idx === i ? { ...r, [f]: v } : r));

  const handleSubmit = async e => {
    e.preventDefault(); setSubmitting(true); setErr('');
    try {
      const dist = await api.post('/api/distributions', {
        branch:      { branchId: parseInt(form.branchId) },
        beneficiary: { beneficiaryId: parseInt(form.beneficiaryId) },
        user:        { userId },
        distributionDate: `${form.distributionDate}T00:00:00`,
      });
      for (const item of items) {
        if (!item.inventoryId || !item.quantity) continue;
        await api.post('/api/distribution-items', {
          distribution: { distributionId: dist.distributionId },
          inventory:    { inventoryId: parseInt(item.inventoryId) },
          quantity:     parseInt(item.quantity),
        });
      }
      close();
      setForm({ beneficiaryId: '', branchId: '', distributionDate: new Date().toISOString().slice(0, 10) });
      setItems([{ inventoryId: '', quantity: '' }]);
      onRefresh();
    } catch (e) { setErr(e.message); } finally { setSubmitting(false); }
  };

  const sorted = [...distributions].sort((a, b) => new Date(b.distributionDate) - new Date(a.distributionDate));

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <span className="text-sm text-gray-500">{distributions.length} distributions · click a row to see items</span>
        <button onClick={() => setShowModal(true)} className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">+ New Distribution</button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-auto max-h-[60vh]">
          {loading ? <div className="p-10 text-center text-gray-400">Loading…</div> : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
                <tr>
                  <th className="px-4 py-3 w-8"></th>
                  <th className="px-4 py-3">#</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Beneficiary</th>
                  <th className="px-4 py-3">Branch</th>
                  <th className="px-4 py-3">Recorded By</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {sorted.map(d => (
                  <React.Fragment key={d.distributionId}>
                    <tr className="border-t border-gray-100 hover:bg-gray-50 cursor-pointer" onClick={() => toggleExpand(d.distributionId)}>
                      <td className="px-4 py-3 text-gray-400 text-xs">{expandedId === d.distributionId ? '▾' : '▸'}</td>
                      <td className="px-4 py-3 text-gray-400 font-mono text-xs">#{d.distributionId}</td>
                      <td className="px-4 py-3 font-mono text-xs">{d.distributionDate?.slice(0, 10)}</td>
                      <td className="px-4 py-3 font-medium">{d.beneficiary?.beneficiaryFullName}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{d.branch?.branchName}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{d.user ? `${d.user.firstName} ${d.user.lastName}` : '—'}</td>
                      <td className="px-4 py-3" onClick={e => e.stopPropagation()}>
                        <button onClick={() => handleDelete(d.distributionId)} className="text-red-400 hover:text-red-600 text-xs font-medium">Delete</button>
                      </td>
                    </tr>
                    {expandedId === d.distributionId && (
                      <tr className="bg-green-50 border-t border-green-100">
                        <td colSpan={7} className="px-8 py-3">
                          {itemsLoading ? <div className="text-gray-400 text-xs py-2">Loading items…</div> :
                           expandedItems.length === 0 ? <div className="text-gray-400 text-xs py-2 italic">No items recorded for this distribution.</div> : (
                            <table className="w-full text-xs">
                              <thead className="text-gray-500 uppercase">
                                <tr>
                                  <th className="pr-6 py-1 font-semibold">Inv ID</th>
                                  <th className="pr-6 py-1 font-semibold">Food Name</th>
                                  <th className="pr-6 py-1 font-semibold text-right">Qty</th>
                                  <th className="py-1 font-semibold">Branch</th>
                                </tr>
                              </thead>
                              <tbody>
                                {expandedItems.map(item => (
                                  <tr key={item.distributionItemId} className="border-t border-green-100">
                                    <td className="pr-6 py-1 font-mono text-green-700">#{item.inventory?.inventoryId}</td>
                                    <td className="pr-6 py-1">{item.inventory?.foodItem?.foodName}</td>
                                    <td className="pr-6 py-1 text-right font-mono font-bold">{item.quantity}</td>
                                    <td className="py-1 text-gray-500">{item.inventory?.branch?.branchName}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          )}
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {showModal && (
        <Modal title="New Distribution" onClose={close}>
          <form onSubmit={handleSubmit} className="space-y-5">
            <Err msg={err} />
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="Beneficiary (Eligible Only)">
                <select required className={inp} value={form.beneficiaryId} onChange={e => setForm(p => ({ ...p, beneficiaryId: e.target.value }))}>
                  <option value="">Select…</option>
                  {eligibleBeneficiaries.map(b => <option key={b.beneficiaryId} value={b.beneficiaryId}>{b.beneficiaryFullName}</option>)}
                </select>
              </Lbl>
              <Lbl text="Branch">
                <select required className={inp} value={form.branchId} onChange={e => setForm(p => ({ ...p, branchId: e.target.value }))}>
                  <option value="">Select branch…</option>
                  {branches.map(b => <option key={b.branchId} value={b.branchId}>{b.branchName}</option>)}
                </select>
              </Lbl>
            </div>
            <Lbl text="Date"><input type="date" required className={inp} value={form.distributionDate} onChange={e => setForm(p => ({ ...p, distributionDate: e.target.value }))} /></Lbl>
            <div>
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-bold text-gray-500 uppercase">Items from Inventory</span>
                <button type="button" onClick={addRow} className="text-xs text-blue-600 hover:underline">+ add row</button>
              </div>
              {items.map((item, i) => (
                <div key={i} className="grid grid-cols-12 gap-1 items-center mb-1">
                  <div className="col-span-10">
                    <select required className="w-full border rounded p-1.5 text-xs" value={item.inventoryId} onChange={e => setRow(i, 'inventoryId', e.target.value)}>
                      <option value="">Select inventory item (qty &gt; 0)…</option>
                      {available.map(inv => (
                        <option key={inv.inventoryId} value={inv.inventoryId}>
                          #{inv.inventoryId} · {inv.foodItem?.foodName} · {inv.quantity} {inv.unit} · {inv.branch?.branchName} · exp {inv.expiryDate?.slice(0, 10)}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-span-1"><input type="number" required min="1" placeholder="Qty" className="w-full border rounded p-1.5 text-xs" value={item.quantity} onChange={e => setRow(i, 'quantity', e.target.value)} /></div>
                  <div className="col-span-1 text-center">{items.length > 1 && <button type="button" onClick={() => dropRow(i)} className="text-red-400 hover:text-red-600 text-lg leading-none">×</button>}</div>
                </div>
              ))}
            </div>
            <div className="flex gap-3 pt-1">
              <button type="submit" disabled={submitting} className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50">{submitting ? 'Saving…' : 'Record Distribution'}</button>
              <button type="button" onClick={close} className="px-5 py-2.5 border rounded-lg text-gray-600 hover:bg-gray-50">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

// ─── FOOD ITEMS TAB ───────────────────────────────────────────────────────────
const FoodItemsTab = ({ foodItems, loading, onRefresh }) => {
  const [categories, setCategories] = useState([]);
  const [showModal, setShowModal]   = useState(false);
  const [editing, setEditing]       = useState(null);
  const [err, setErr]               = useState('');
  const [submitting, setSubmitting] = useState(false);
  const blank = { sku: '', foodName: '', foodDescription: '', storageCondition: '', categoryId: '' };
  const [form, setForm]             = useState(blank);

  useEffect(() => {
    api.get('/api/inventory/categories').then(setCategories).catch(() => {});
  }, []);

  const set   = (k, v) => setForm(p => ({ ...p, [k]: v }));
  const close = () => { setShowModal(false); setEditing(null); setErr(''); setForm(blank); };

  const openEdit = item => {
    setEditing(item);
    setForm({ sku: item.sku, foodName: item.foodName, foodDescription: item.foodDescription || '',
      storageCondition: item.storageCondition || '', categoryId: item.category?.categoryId || '' });
  };

  const handleSave = async e => {
    e.preventDefault(); setSubmitting(true); setErr('');
    try {
      const body = { sku: form.sku, foodName: form.foodName, foodDescription: form.foodDescription,
        storageCondition: form.storageCondition, category: form.categoryId ? { categoryId: parseInt(form.categoryId) } : null };
      if (editing) { await api.put(`/api/inventory/food-items/${editing.sku}`, body); }
      else         { await api.post('/api/inventory/food-items', body); }
      close(); onRefresh();
    } catch (e) { setErr(e.message); } finally { setSubmitting(false); }
  };

  const handleDelete = async sku => {
    if (!window.confirm(`Delete food item ${sku}? This will fail if inventory records reference it.`)) return;
    try { await api.delete(`/api/inventory/food-items/${sku}`); onRefresh(); }
    catch (e) { alert(e.message); }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <span className="text-sm text-gray-500">{foodItems.length} food items in catalog</span>
        <button onClick={() => setShowModal(true)} className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">+ Add Food Item</button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-auto max-h-[60vh]">
          {loading ? <div className="p-10 text-center text-gray-400">Loading…</div> : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
                <tr>
                  <th className="px-4 py-3">SKU</th>
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3">Storage</th>
                  <th className="px-4 py-3">Description</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {foodItems.map(fi => (
                  <tr key={fi.sku} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-blue-600 text-xs">{fi.sku}</td>
                    <td className="px-4 py-3 font-medium">{fi.foodName}</td>
                    <td className="px-4 py-3">
                      <span className="bg-purple-100 text-purple-700 px-2 py-0.5 rounded-full text-xs">{fi.category?.categoryName || '—'}</span>
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{fi.storageCondition || '—'}</td>
                    <td className="px-4 py-3 text-gray-400 text-xs max-w-xs truncate">{fi.foodDescription || '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-3">
                        <button onClick={() => openEdit(fi)} className="text-blue-500 hover:text-blue-700 text-xs font-medium">Edit</button>
                        <button onClick={() => handleDelete(fi.sku)} className="text-red-400 hover:text-red-600 text-xs font-medium">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {(showModal || editing) && (
        <Modal title={editing ? `Edit ${editing.sku}` : 'Add Food Item'} onClose={close}>
          <form onSubmit={handleSave} className="space-y-4">
            <Err msg={err} />
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="SKU">
                <input required className={inp} value={form.sku}
                  onChange={e => set('sku', e.target.value)} disabled={!!editing}
                  placeholder="e.g. SKU-011" />
              </Lbl>
              <Lbl text="Category">
                <select className={inp} value={form.categoryId} onChange={e => set('categoryId', e.target.value)}>
                  <option value="">No category</option>
                  {categories.map(c => <option key={c.categoryId} value={c.categoryId}>{c.categoryName}</option>)}
                </select>
              </Lbl>
            </div>
            <Lbl text="Food Name"><input required className={inp} value={form.foodName} onChange={e => set('foodName', e.target.value)} /></Lbl>
            <Lbl text="Storage Condition"><input className={inp} placeholder="e.g. refrigerated, dry" value={form.storageCondition} onChange={e => set('storageCondition', e.target.value)} /></Lbl>
            <Lbl text="Description"><textarea rows={2} className={inp} value={form.foodDescription} onChange={e => set('foodDescription', e.target.value)} /></Lbl>
            {editing && (
              <p className="text-xs text-amber-600 bg-amber-50 p-2 rounded-lg border border-amber-200">
                Note: SKU is the primary key and cannot be changed. Category names drive expiry logic — do not rename Produce, Dairy, Meat, Seafood, or Bakery.
              </p>
            )}
            <div className="flex gap-3 pt-1">
              <button type="submit" disabled={submitting} className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50">{submitting ? 'Saving…' : editing ? 'Save Changes' : 'Add Food Item'}</button>
              <button type="button" onClick={close} className="px-5 py-2.5 border rounded-lg text-gray-600 hover:bg-gray-50">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

// ─── MAIN COMPONENT ───────────────────────────────────────────────────────────
const OperationsPortal = () => {
  const [activeTab, setActiveTab]             = useState('inventory');
  const [inventory, setInventory]             = useState([]);
  const [outOfStock, setOutOfStock]           = useState([]);
  const [donations, setDonations]             = useState([]);
  const [distributions, setDistributions]     = useState([]);
  const [donors, setDonors]                   = useState([]);
  const [eligibleBeneficiaries, setEligible]  = useState([]);
  const [foodItems, setFoodItems]             = useState([]);
  const [branches, setBranches]               = useState([]);
  const [loading, setLoading]                 = useState(true);
  const [error, setError]                     = useState('');

  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const [inv, oos, don, dist, drs, bens, foods, br] = await Promise.all([
        api.get('/api/inventory'),
        api.get('/api/inventory/out-of-stock'),
        api.get('/api/donations'),
        api.get('/api/distributions'),
        api.get('/api/donors'),
        api.get('/api/beneficiaries/eligible'),
        api.get('/api/inventory/food-items'),
        api.get('/api/branches'),
      ]);
      setInventory(inv); setOutOfStock(oos); setDonations(don);
      setDistributions(dist); setDonors(drs); setEligible(bens); setFoodItems(foods); setBranches(br);
    } catch (e) { setError(e.message); } finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const tabs = [
    { key: 'inventory',     label: `Inventory (${inventory.length})` },
    { key: 'donations',     label: `Donations (${donations.length})` },
    { key: 'distributions', label: `Distributions (${distributions.length})` },
    { key: 'food-items',    label: `Food Items (${foodItems.length})` },
  ];

  return (
    <div className="p-8">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Operations Portal</h1>
        <p className="text-gray-500 text-sm">Inventory, food catalog, donations, distributions</p>
      </header>
      {error && <div className="mb-4 bg-red-50 text-red-700 p-3 rounded-lg text-sm border border-red-200">{error}</div>}
      <div className="flex space-x-1 mb-6 border-b border-gray-200">
        {tabs.map(tab => (
          <button key={tab.key} onClick={() => setActiveTab(tab.key)}
            className={`pb-2.5 px-5 font-medium text-sm transition-colors ${activeTab === tab.key ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}>
            {tab.label}
          </button>
        ))}
      </div>
      {activeTab === 'inventory'     && <InventoryTab inventory={inventory} outOfStock={outOfStock} loading={loading} onRefresh={fetchAll} />}
      {activeTab === 'donations'     && <DonationsTab donations={donations} donors={donors} foodItems={foodItems} branches={branches} loading={loading} onRefresh={fetchAll} />}
      {activeTab === 'distributions' && <DistributionsTab distributions={distributions} eligibleBeneficiaries={eligibleBeneficiaries} inventory={inventory} branches={branches} loading={loading} onRefresh={fetchAll} />}
      {activeTab === 'food-items'    && <FoodItemsTab foodItems={foodItems} loading={loading} onRefresh={fetchAll} />}
    </div>
  );
};

export default OperationsPortal;
