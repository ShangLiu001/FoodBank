import React, { useState, useEffect, useCallback } from 'react';
import api from './api';

// ─── Shared helpers ───────────────────────────────────────────────────────────
const Modal = ({ title, onClose, children }) => (
  <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg flex flex-col max-h-[90vh]">
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
const StatusBadge = ({ active }) => (
  <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
    {active ? 'Active' : 'Inactive'}
  </span>
);

// ─── VOLUNTEERS TAB ───────────────────────────────────────────────────────────
const VolunteersTab = ({ volunteers, loading, onRefresh }) => {
  const [showAdd, setShowAdd]     = useState(false);
  const [editing, setEditing]     = useState(null);
  const [err, setErr]             = useState('');
  const [saving, setSaving]       = useState(false);
  const blank = { firstName: '', lastName: '', email: '', phone: '', password: '', branchId: '', availability: '', backgroundCheck: 0, status: 1 };
  const [form, setForm]           = useState(blank);

  const set   = (k, v) => setForm(p => ({ ...p, [k]: v }));
  const close = () => { setShowAdd(false); setEditing(null); setErr(''); setForm(blank); };

  const openEdit = v => {
    setEditing(v);
    setForm({ firstName: v.user.firstName, lastName: v.user.lastName, email: v.user.email,
      phone: v.user.phone || '', password: '', branchId: v.user.branch?.branchId || '',
      availability: v.availability || '', backgroundCheck: v.backgroundCheck || 0, status: v.user.status });
  };

  const handleSave = async e => {
    e.preventDefault(); setSaving(true); setErr('');
    try {
      if (editing) {
        const body = { firstName: form.firstName, lastName: form.lastName, phone: form.phone,
          branchId: parseInt(form.branchId), status: parseInt(form.status) };
        if (form.password) body.password = form.password;
        await api.put(`/api/users/${editing.user.userId}`, body);
        await api.put(`/api/volunteers/${editing.volunteerId}`, {
          user: { userId: editing.user.userId },
          availability: form.availability,
          backgroundCheck: parseInt(form.backgroundCheck),
        });
      } else {
        await api.post('/api/users', { ...form, role: 1,
          branchId: parseInt(form.branchId),
          backgroundCheck: parseInt(form.backgroundCheck),
          status: parseInt(form.status),
        });
      }
      close(); onRefresh();
    } catch (e) { setErr(e.message); } finally { setSaving(false); }
  };

  const handleDelete = async userId => {
    if (!window.confirm('Delete this volunteer? Associated donations/distributions will prevent deletion.')) return;
    try { await api.delete(`/api/users/${userId}`); onRefresh(); }
    catch (e) { alert(e.message); }
  };

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button onClick={() => setShowAdd(true)} className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">+ Add Volunteer</button>
      </div>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-auto max-h-[58vh]">
          {loading ? <div className="p-10 text-center text-gray-400">Loading…</div> : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
                <tr>
                  <th className="px-4 py-3">Name</th><th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Branch</th><th className="px-4 py-3">Availability</th>
                  <th className="px-4 py-3">BG Check</th><th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {volunteers.map(v => (
                  <tr key={v.volunteerId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium">{v.user?.firstName} {v.user?.lastName}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{v.user?.email}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{v.user?.branch?.branchName}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{v.availability || '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded-full text-xs ${v.backgroundCheck === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                        {v.backgroundCheck === 1 ? 'Cleared' : 'Pending'}
                      </span>
                    </td>
                    <td className="px-4 py-3"><StatusBadge active={v.user?.status === 1} /></td>
                    <td className="px-4 py-3">
                      <div className="flex gap-3">
                        <button onClick={() => openEdit(v)} className="text-blue-500 hover:text-blue-700 text-xs font-medium">Edit</button>
                        <button onClick={() => handleDelete(v.user?.userId)} className="text-red-400 hover:text-red-600 text-xs font-medium">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
      {(showAdd || editing) && (
        <Modal title={editing ? 'Edit Volunteer' : 'Add Volunteer'} onClose={close}>
          <form onSubmit={handleSave} className="space-y-4">
            <Err msg={err} />
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="First Name"><input required={!editing} className={inp} value={form.firstName} onChange={e => set('firstName', e.target.value)} /></Lbl>
              <Lbl text="Last Name"><input required={!editing} className={inp} value={form.lastName} onChange={e => set('lastName', e.target.value)} /></Lbl>
            </div>
            {!editing && <Lbl text="Email"><input type="email" required className={inp} value={form.email} onChange={e => set('email', e.target.value)} /></Lbl>}
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="Phone"><input className={inp} value={form.phone} onChange={e => set('phone', e.target.value)} /></Lbl>
              <Lbl text="Branch ID"><input type="number" required className={inp} value={form.branchId} onChange={e => set('branchId', e.target.value)} /></Lbl>
            </div>
            <Lbl text={editing ? 'New Password (leave blank to keep)' : 'Password'}>
              <input type="password" required={!editing} className={inp} value={form.password} onChange={e => set('password', e.target.value)} />
            </Lbl>
            <Lbl text="Availability"><input className={inp} placeholder="e.g. Weekends" value={form.availability} onChange={e => set('availability', e.target.value)} /></Lbl>
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="Background Check">
                <select className={inp} value={form.backgroundCheck} onChange={e => set('backgroundCheck', e.target.value)}>
                  <option value={0}>Pending</option><option value={1}>Cleared</option>
                </select>
              </Lbl>
              <Lbl text="Status">
                <select className={inp} value={form.status} onChange={e => set('status', e.target.value)}>
                  <option value={1}>Active</option><option value={0}>Inactive</option>
                </select>
              </Lbl>
            </div>
            <div className="flex gap-3 pt-1">
              <button type="submit" disabled={saving} className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50">{saving ? 'Saving…' : editing ? 'Save Changes' : 'Add Volunteer'}</button>
              <button type="button" onClick={close} className="px-5 py-2.5 border rounded-lg text-gray-600 hover:bg-gray-50">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

// ─── DONORS TAB ───────────────────────────────────────────────────────────────
const DonorsTab = ({ donors, loading, onRefresh }) => {
  const [showAdd, setShowAdd] = useState(false);
  const [editing, setEditing] = useState(null);
  const [err, setErr]         = useState('');
  const [saving, setSaving]   = useState(false);
  const blank = { donorName: '', donorType: 0, email: '', phone: '' };
  const [form, setForm]       = useState(blank);

  const set   = (k, v) => setForm(p => ({ ...p, [k]: v }));
  const close = () => { setShowAdd(false); setEditing(null); setErr(''); setForm(blank); };

  const openEdit = d => { setEditing(d); setForm({ donorName: d.donorName, donorType: d.donorType, email: d.email || '', phone: d.phone || '' }); };

  const handleSave = async e => {
    e.preventDefault(); setSaving(true); setErr('');
    try {
      const body = { ...form, donorType: parseInt(form.donorType) };
      if (editing) await api.put(`/api/donors/${editing.donorId}`, body);
      else         await api.post('/api/donors', body);
      close(); onRefresh();
    } catch (e) { setErr(e.message); } finally { setSaving(false); }
  };

  const handleDelete = async id => {
    if (!window.confirm('Delete this donor?')) return;
    try { await api.delete(`/api/donors/${id}`); onRefresh(); } catch (e) { alert(e.message); }
  };

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button onClick={() => setShowAdd(true)} className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">+ Add Donor</button>
      </div>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-auto max-h-[58vh]">
          {loading ? <div className="p-10 text-center text-gray-400">Loading…</div> : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
                <tr><th className="px-4 py-3">ID</th><th className="px-4 py-3">Name</th><th className="px-4 py-3">Type</th><th className="px-4 py-3">Email</th><th className="px-4 py-3">Phone</th><th className="px-4 py-3"></th></tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {donors.map(d => (
                  <tr key={d.donorId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-gray-400 font-mono text-xs">{d.donorId}</td>
                    <td className="px-4 py-3 font-medium">{d.donorName}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded-full text-xs ${d.donorType === 0 ? 'bg-blue-100 text-blue-700' : 'bg-purple-100 text-purple-700'}`}>
                        {d.donorType === 0 ? 'Individual' : 'Organization'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{d.email || '—'}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{d.phone || '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-3">
                        <button onClick={() => openEdit(d)} className="text-blue-500 hover:text-blue-700 text-xs font-medium">Edit</button>
                        <button onClick={() => handleDelete(d.donorId)} className="text-red-400 hover:text-red-600 text-xs font-medium">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
      {(showAdd || editing) && (
        <Modal title={editing ? 'Edit Donor' : 'Add Donor'} onClose={close}>
          <form onSubmit={handleSave} className="space-y-4">
            <Err msg={err} />
            <Lbl text="Donor Name"><input required className={inp} value={form.donorName} onChange={e => set('donorName', e.target.value)} /></Lbl>
            <Lbl text="Type">
              <select className={inp} value={form.donorType} onChange={e => set('donorType', e.target.value)}>
                <option value={0}>Individual</option><option value={1}>Organization</option>
              </select>
            </Lbl>
            <Lbl text="Email"><input type="email" className={inp} value={form.email} onChange={e => set('email', e.target.value)} /></Lbl>
            <Lbl text="Phone"><input className={inp} value={form.phone} onChange={e => set('phone', e.target.value)} /></Lbl>
            <div className="flex gap-3 pt-1">
              <button type="submit" disabled={saving} className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50">{saving ? 'Saving…' : editing ? 'Save Changes' : 'Add Donor'}</button>
              <button type="button" onClick={close} className="px-5 py-2.5 border rounded-lg text-gray-600 hover:bg-gray-50">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

// ─── BENEFICIARIES TAB ────────────────────────────────────────────────────────
const BeneficiariesTab = ({ beneficiaries, loading, onRefresh }) => {
  const [showAdd, setShowAdd] = useState(false);
  const [editing, setEditing] = useState(null);
  const [err, setErr]         = useState('');
  const [saving, setSaving]   = useState(false);
  const blank = { beneficiaryFullName: '', householdSize: '', phone: '', email: '', eligibilityStatus: 1 };
  const [form, setForm]       = useState(blank);

  const set   = (k, v) => setForm(p => ({ ...p, [k]: v }));
  const close = () => { setShowAdd(false); setEditing(null); setErr(''); setForm(blank); };

  const openEdit = b => { setEditing(b); setForm({ beneficiaryFullName: b.beneficiaryFullName, householdSize: b.householdSize, phone: b.phone || '', email: b.email || '', eligibilityStatus: b.eligibilityStatus }); };

  const handleSave = async e => {
    e.preventDefault(); setSaving(true); setErr('');
    try {
      const body = { ...form, householdSize: parseInt(form.householdSize), eligibilityStatus: parseInt(form.eligibilityStatus) };
      if (editing) await api.put(`/api/beneficiaries/${editing.beneficiaryId}`, body);
      else         await api.post('/api/beneficiaries', body);
      close(); onRefresh();
    } catch (e) { setErr(e.message); } finally { setSaving(false); }
  };

  const handleDelete = async id => {
    if (!window.confirm('Delete this beneficiary?')) return;
    try { await api.delete(`/api/beneficiaries/${id}`); onRefresh(); } catch (e) { alert(e.message); }
  };

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button onClick={() => setShowAdd(true)} className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">+ Add Beneficiary</button>
      </div>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-auto max-h-[58vh]">
          {loading ? <div className="p-10 text-center text-gray-400">Loading…</div> : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
                <tr><th className="px-4 py-3">ID</th><th className="px-4 py-3">Name</th><th className="px-4 py-3">Household</th><th className="px-4 py-3">Phone</th><th className="px-4 py-3">Email</th><th className="px-4 py-3">Eligibility</th><th className="px-4 py-3"></th></tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {beneficiaries.map(b => (
                  <tr key={b.beneficiaryId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-gray-400 font-mono text-xs">{b.beneficiaryId}</td>
                    <td className="px-4 py-3 font-medium">{b.beneficiaryFullName}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{b.householdSize} people</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{b.phone || '—'}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{b.email || '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${b.eligibilityStatus === 1 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'}`}>
                        {b.eligibilityStatus === 1 ? 'Eligible' : 'Ineligible'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-3">
                        <button onClick={() => openEdit(b)} className="text-blue-500 hover:text-blue-700 text-xs font-medium">Edit</button>
                        <button onClick={() => handleDelete(b.beneficiaryId)} className="text-red-400 hover:text-red-600 text-xs font-medium">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
      {(showAdd || editing) && (
        <Modal title={editing ? 'Edit Beneficiary' : 'Add Beneficiary'} onClose={close}>
          <form onSubmit={handleSave} className="space-y-4">
            <Err msg={err} />
            <Lbl text="Full Name"><input required className={inp} value={form.beneficiaryFullName} onChange={e => set('beneficiaryFullName', e.target.value)} /></Lbl>
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="Household Size"><input type="number" required min="1" className={inp} value={form.householdSize} onChange={e => set('householdSize', e.target.value)} /></Lbl>
              <Lbl text="Phone"><input className={inp} value={form.phone} onChange={e => set('phone', e.target.value)} /></Lbl>
            </div>
            <Lbl text="Email"><input type="email" className={inp} value={form.email} onChange={e => set('email', e.target.value)} /></Lbl>
            <Lbl text="Eligibility">
              <select className={inp} value={form.eligibilityStatus} onChange={e => set('eligibilityStatus', e.target.value)}>
                <option value={1}>Eligible</option><option value={0}>Ineligible</option>
              </select>
            </Lbl>
            <div className="flex gap-3 pt-1">
              <button type="submit" disabled={saving} className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50">{saving ? 'Saving…' : editing ? 'Save Changes' : 'Add Beneficiary'}</button>
              <button type="button" onClick={close} className="px-5 py-2.5 border rounded-lg text-gray-600 hover:bg-gray-50">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

// ─── STAFF TAB ────────────────────────────────────────────────────────────────
const StaffTab = ({ staff, loading, onRefresh }) => {
  const [showAdd, setShowAdd] = useState(false);
  const [editing, setEditing] = useState(null);
  const [err, setErr]         = useState('');
  const [saving, setSaving]   = useState(false);
  const blank = { firstName: '', lastName: '', email: '', phone: '', password: '', branchId: '', jobTitle: '', hireDate: new Date().toISOString().slice(0, 10), status: 1 };
  const [form, setForm]       = useState(blank);

  const set   = (k, v) => setForm(p => ({ ...p, [k]: v }));
  const close = () => { setShowAdd(false); setEditing(null); setErr(''); setForm(blank); };

  const openEdit = u => {
    setEditing(u);
    setForm({ firstName: u.firstName, lastName: u.lastName, email: u.email, phone: u.phone || '',
      password: '', branchId: u.branch?.branchId || '', jobTitle: '', hireDate: new Date().toISOString().slice(0, 10), status: u.status });
  };

  const handleSave = async e => {
    e.preventDefault(); setSaving(true); setErr('');
    try {
      if (editing) {
        const body = { firstName: form.firstName, lastName: form.lastName, phone: form.phone,
          branchId: parseInt(form.branchId), status: parseInt(form.status) };
        if (form.password) body.password = form.password;
        await api.put(`/api/users/${editing.userId}`, body);
      } else {
        await api.post('/api/users', { ...form, role: 0, branchId: parseInt(form.branchId),
          status: parseInt(form.status), hireDate: `${form.hireDate}T00:00:00` });
      }
      close(); onRefresh();
    } catch (e) { setErr(e.message); } finally { setSaving(false); }
  };

  const handleDelete = async userId => {
    if (!window.confirm('Delete this staff member?')) return;
    try { await api.delete(`/api/users/${userId}`); onRefresh(); } catch (e) { alert(e.message); }
  };

  return (
    <div>
      <div className="flex justify-end mb-4">
        <button onClick={() => setShowAdd(true)} className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">+ Add Staff</button>
      </div>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-auto max-h-[58vh]">
          {loading ? <div className="p-10 text-center text-gray-400">Loading…</div> : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
                <tr><th className="px-4 py-3">Name</th><th className="px-4 py-3">Email</th><th className="px-4 py-3">Branch</th><th className="px-4 py-3">Status</th><th className="px-4 py-3"></th></tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {staff.map(u => (
                  <tr key={u.userId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium">{u.firstName} {u.lastName}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{u.email}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{u.branch?.branchName}</td>
                    <td className="px-4 py-3"><StatusBadge active={u.status === 1} /></td>
                    <td className="px-4 py-3">
                      <div className="flex gap-3">
                        <button onClick={() => openEdit(u)} className="text-blue-500 hover:text-blue-700 text-xs font-medium">Edit</button>
                        <button onClick={() => handleDelete(u.userId)} className="text-red-400 hover:text-red-600 text-xs font-medium">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
      {(showAdd || editing) && (
        <Modal title={editing ? 'Edit Staff' : 'Add Staff Member'} onClose={close}>
          <form onSubmit={handleSave} className="space-y-4">
            <Err msg={err} />
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="First Name"><input required={!editing} className={inp} value={form.firstName} onChange={e => set('firstName', e.target.value)} /></Lbl>
              <Lbl text="Last Name"><input required={!editing} className={inp} value={form.lastName} onChange={e => set('lastName', e.target.value)} /></Lbl>
            </div>
            {!editing && <Lbl text="Email"><input type="email" required className={inp} value={form.email} onChange={e => set('email', e.target.value)} /></Lbl>}
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="Phone"><input className={inp} value={form.phone} onChange={e => set('phone', e.target.value)} /></Lbl>
              <Lbl text="Branch ID"><input type="number" required className={inp} value={form.branchId} onChange={e => set('branchId', e.target.value)} /></Lbl>
            </div>
            <Lbl text={editing ? 'New Password (leave blank to keep)' : 'Password'}>
              <input type="password" required={!editing} className={inp} value={form.password} onChange={e => set('password', e.target.value)} />
            </Lbl>
            {!editing && (
              <>
                <Lbl text="Job Title"><input className={inp} value={form.jobTitle} onChange={e => set('jobTitle', e.target.value)} /></Lbl>
                <Lbl text="Hire Date"><input type="date" className={inp} value={form.hireDate} onChange={e => set('hireDate', e.target.value)} /></Lbl>
              </>
            )}
            <Lbl text="Status">
              <select className={inp} value={form.status} onChange={e => set('status', e.target.value)}>
                <option value={1}>Active</option><option value={0}>Inactive</option>
              </select>
            </Lbl>
            <div className="flex gap-3 pt-1">
              <button type="submit" disabled={saving} className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50">{saving ? 'Saving…' : editing ? 'Save Changes' : 'Add Staff'}</button>
              <button type="button" onClick={close} className="px-5 py-2.5 border rounded-lg text-gray-600 hover:bg-gray-50">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

// ─── SHIFTS TAB ───────────────────────────────────────────────────────────────
const ShiftsTab = ({ volunteers, loading: volLoading, onRefresh }) => {
  const [shifts, setShifts]       = useState([]);
  const [loading, setLoading]     = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing]     = useState(null);
  const [err, setErr]             = useState('');
  const [saving, setSaving]       = useState(false);
  const blank = { volunteerId: '', branchId: '', shiftDate: new Date().toISOString().slice(0, 10), shiftTimeStart: '09:00', shiftTimeEnd: '13:00', shiftNotes: '' };
  const [form, setForm]           = useState(blank);

  const fetchShifts = useCallback(async () => {
    setLoading(true);
    try { setShifts(await api.get('/api/shifts')); }
    catch { /* handled below */ } finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchShifts(); }, [fetchShifts]);

  const set   = (k, v) => setForm(p => ({ ...p, [k]: v }));
  const close = () => { setShowModal(false); setEditing(null); setErr(''); setForm(blank); };

  const openEdit = s => {
    setEditing(s);
    setForm({
      volunteerId:   s.volunteer?.volunteerId || '',
      branchId:      s.branch?.branchId || '',
      shiftDate:     s.shiftDate || '',
      shiftTimeStart: s.shiftTimeStart ? s.shiftTimeStart.slice(0, 5) : '09:00',
      shiftTimeEnd:   s.shiftTimeEnd   ? s.shiftTimeEnd.slice(0, 5)   : '13:00',
      shiftNotes:    s.shiftNotes || '',
    });
  };

  const handleSave = async e => {
    e.preventDefault(); setSaving(true); setErr('');
    try {
      const body = {
        volunteer:     { volunteerId: parseInt(form.volunteerId) },
        branch:        { branchId: parseInt(form.branchId) },
        shiftDate:     form.shiftDate,
        shiftTimeStart: `${form.shiftTimeStart}:00`,
        shiftTimeEnd:   `${form.shiftTimeEnd}:00`,
        shiftNotes:    form.shiftNotes || null,
      };
      if (editing) await api.put(`/api/shifts/${editing.shiftId}`, body);
      else         await api.post('/api/shifts', body);
      close(); fetchShifts(); onRefresh();
    } catch (e) { setErr(e.message); } finally { setSaving(false); }
  };

  const handleDelete = async id => {
    if (!window.confirm('Delete this shift?')) return;
    try { await api.delete(`/api/shifts/${id}`); fetchShifts(); } catch (e) { alert(e.message); }
  };

  const sorted = [...shifts].sort((a, b) => {
    if (a.shiftDate !== b.shiftDate) return b.shiftDate?.localeCompare(a.shiftDate);
    return a.shiftTimeStart?.localeCompare(b.shiftTimeStart);
  });

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <span className="text-sm text-gray-500">{shifts.length} shifts · DB trigger prevents overlapping shifts per volunteer</span>
        <button onClick={() => setShowModal(true)} className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">+ Schedule Shift</button>
      </div>

      <div className="mb-4 bg-amber-50 border border-amber-200 p-3 rounded-lg text-xs text-amber-700">
        <strong>trg_volunteer_shift_no_overlap_insert / update</strong> — The database will reject any shift that overlaps with an existing block for the same volunteer on the same date across any branch. Back-to-back shifts (end = start) are allowed.
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-auto max-h-[52vh]">
          {loading ? <div className="p-10 text-center text-gray-400">Loading…</div> : shifts.length === 0 ? (
            <div className="p-10 text-center text-gray-400 text-sm">No shifts scheduled.</div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 text-xs uppercase sticky top-0">
                <tr>
                  <th className="px-4 py-3">Volunteer</th><th className="px-4 py-3">Branch</th>
                  <th className="px-4 py-3">Date</th><th className="px-4 py-3">Start</th>
                  <th className="px-4 py-3">End</th><th className="px-4 py-3">Notes</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {sorted.map(s => (
                  <tr key={s.shiftId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium">{s.volunteer?.user?.firstName} {s.volunteer?.user?.lastName}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{s.branch?.branchName}</td>
                    <td className="px-4 py-3 font-mono text-xs">{s.shiftDate}</td>
                    <td className="px-4 py-3 font-mono text-xs">{s.shiftTimeStart?.slice(0, 5)}</td>
                    <td className="px-4 py-3 font-mono text-xs">{s.shiftTimeEnd?.slice(0, 5)}</td>
                    <td className="px-4 py-3 text-gray-400 text-xs">{s.shiftNotes || '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-3">
                        <button onClick={() => openEdit(s)} className="text-blue-500 hover:text-blue-700 text-xs font-medium">Edit</button>
                        <button onClick={() => handleDelete(s.shiftId)} className="text-red-400 hover:text-red-600 text-xs font-medium">Delete</button>
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
        <Modal title={editing ? 'Edit Shift' : 'Schedule Shift'} onClose={close}>
          <form onSubmit={handleSave} className="space-y-4">
            <Err msg={err} />
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="Volunteer">
                <select required className={inp} value={form.volunteerId} onChange={e => set('volunteerId', e.target.value)}>
                  <option value="">Select volunteer…</option>
                  {volunteers.map(v => (
                    <option key={v.volunteerId} value={v.volunteerId}>
                      {v.user?.firstName} {v.user?.lastName}
                    </option>
                  ))}
                </select>
              </Lbl>
              <Lbl text="Branch ID"><input type="number" required className={inp} value={form.branchId} onChange={e => set('branchId', e.target.value)} /></Lbl>
            </div>
            <Lbl text="Shift Date"><input type="date" required className={inp} value={form.shiftDate} onChange={e => set('shiftDate', e.target.value)} /></Lbl>
            <div className="grid grid-cols-2 gap-4">
              <Lbl text="Start Time"><input type="time" required className={inp} value={form.shiftTimeStart} onChange={e => set('shiftTimeStart', e.target.value)} /></Lbl>
              <Lbl text="End Time"><input type="time" required className={inp} value={form.shiftTimeEnd} onChange={e => set('shiftTimeEnd', e.target.value)} /></Lbl>
            </div>
            <Lbl text="Notes (optional)"><input className={inp} value={form.shiftNotes} onChange={e => set('shiftNotes', e.target.value)} placeholder="e.g. Food sorting, loading dock" /></Lbl>
            <div className="flex gap-3 pt-1">
              <button type="submit" disabled={saving} className="flex-1 bg-blue-600 text-white py-2.5 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50">{saving ? 'Saving…' : editing ? 'Save Changes' : 'Schedule Shift'}</button>
              <button type="button" onClick={close} className="px-5 py-2.5 border rounded-lg text-gray-600 hover:bg-gray-50">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

// ─── MAIN COMPONENT ───────────────────────────────────────────────────────────
const CommunityManagement = () => {
  const [activeTab, setActiveTab]         = useState('volunteers');
  const [volunteers, setVolunteers]       = useState([]);
  const [donors, setDonors]               = useState([]);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [staff, setStaff]                 = useState([]);
  const [loading, setLoading]             = useState(true);
  const [error, setError]                 = useState('');

  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const [vols, drs, bens, stf] = await Promise.all([
        api.get('/api/volunteers'),
        api.get('/api/donors'),
        api.get('/api/beneficiaries'),
        api.get('/api/users/role/0'),
      ]);
      setVolunteers(vols); setDonors(drs); setBeneficiaries(bens); setStaff(stf);
    } catch (e) { setError(e.message); } finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const tabs = [
    { key: 'volunteers',    label: `Volunteers (${volunteers.length})` },
    { key: 'donors',        label: `Donors (${donors.length})` },
    { key: 'beneficiaries', label: `Beneficiaries (${beneficiaries.length})` },
    { key: 'staff',         label: `Staff (${staff.length})` },
    { key: 'shifts',        label: 'Shifts' },
  ];

  return (
    <div className="p-8">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Community Management</h1>
        <p className="text-gray-500 text-sm">Volunteers, donors, beneficiaries, staff, and shift scheduling</p>
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
      {activeTab === 'volunteers'    && <VolunteersTab    volunteers={volunteers}       loading={loading} onRefresh={fetchAll} />}
      {activeTab === 'donors'        && <DonorsTab        donors={donors}               loading={loading} onRefresh={fetchAll} />}
      {activeTab === 'beneficiaries' && <BeneficiariesTab beneficiaries={beneficiaries} loading={loading} onRefresh={fetchAll} />}
      {activeTab === 'staff'         && <StaffTab         staff={staff}                 loading={loading} onRefresh={fetchAll} />}
      {activeTab === 'shifts'        && <ShiftsTab        volunteers={volunteers}        loading={loading} onRefresh={fetchAll} />}
    </div>
  );
};

export default CommunityManagement;
