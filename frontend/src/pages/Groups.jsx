import { useEffect, useState } from 'react';
import { Plus, X } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import api from '../api/client';
import { formatINR } from '../utils/format';
import { useToast } from '../context/ToastContext';

export default function Groups() {
  const { show } = useToast();
  const [groups, setGroups] = useState([]);
  const [name, setName] = useState('');
  const [selected, setSelected] = useState(null);
  const [members, setMembers] = useState([]);
  const [balances, setBalances] = useState({});
  const [inviteRows, setInviteRows] = useState(['']);
  const [memberError, setMemberError] = useState('');
  const [expenseTitle, setExpenseTitle] = useState('');
  const [expenseAmount, setExpenseAmount] = useState('');
  const [expenseDetail, setExpenseDetail] = useState(null);
  const [searchParams, setSearchParams] = useSearchParams();

  function load() {
    api.get('/groups').then((res) => setGroups(res.data));
  }

  useEffect(() => { load(); }, []);

  async function openGroup(g) {
    setSelected(g);
    setMemberError('');
    setInviteRows(['']);
    try {
      const balRes = await api.get(`/groups/${g.id}/balances`);
      setBalances(balRes.data);
    } catch {
      setBalances({});
    }
    try {
      const memRes = await api.get(`/groups/${g.id}/members`);
      setMembers(memRes.data);
    } catch {
      setMembers([]);
      setMemberError('Could not load members. Restart the backend (Flyway V4) and try again.');
    }
  }

  useEffect(() => {
    const groupId = searchParams.get('group');
    const expenseId = searchParams.get('expense');
    if (!groupId || groups.length === 0) return;
    const g = groups.find((x) => String(x.id) === groupId);
    if (g) {
      openGroup(g);
    }
    if (expenseId && groupId) {
      api.get(`/groups/${groupId}/expenses/${expenseId}`)
        .then((res) => setExpenseDetail(res.data))
        .catch(() => setExpenseDetail(null));
    }
  }, [groups, searchParams]);

  async function createGroup() {
    await api.post('/groups', { name, memberUserIds: [] });
    setName('');
    show('Group created');
    load();
  }

  function updateInviteRow(index, value) {
    setInviteRows((rows) => rows.map((r, i) => (i === index ? value : r)));
    setMemberError('');
  }

  function addInviteRow() {
    setInviteRows((rows) => [...rows, '']);
  }

  function removeInviteRow(index) {
    setInviteRows((rows) => (rows.length <= 1 ? [''] : rows.filter((_, i) => i !== index)));
  }

  async function addMembers() {
    if (!selected) return;
    setMemberError('');
    const invites = inviteRows.map((s) => s.trim()).filter(Boolean);
    if (invites.length === 0) return;

    let added = 0;
    const failures = [];
    for (const invite of invites) {
      try {
        await api.post(`/groups/${selected.id}/members`, { invite });
        added += 1;
      } catch (err) {
        const msg = err.response?.data?.message || 'Could not add';
        failures.push(`${invite}: ${msg}`);
      }
    }

    if (added > 0) {
      show(added === 1 ? 'Member added' : `Added ${added} members`);
      setInviteRows(['']);
      load();
      openGroup(selected);
    }
    if (failures.length > 0) {
      setMemberError(failures.join(' · '));
    }
  }

  async function addExpense() {
    await api.post(`/groups/${selected.id}/expenses`, {
      title: expenseTitle,
      totalAmount: Number(expenseAmount),
    });
    show('Expense recorded (no auto wallet movement)');
    openGroup(selected);
    setExpenseTitle('');
    setExpenseAmount('');
  }

  function memberLabel(userId) {
    const m = members.find((x) => x.userId === Number(userId));
    return m ? m.fullName : `User #${userId}`;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">SplitSmart</h1>
      <p className="text-sm text-drip-muted">
        Create a group, add as many friends as you like (email or wallet ID), then split expenses equally.
      </p>
      <div className="card flex flex-wrap gap-3 p-4">
        <input className="input max-w-xs" placeholder="Group name" value={name} onChange={(e) => setName(e.target.value)} />
        <button type="button" className="btn-primary" onClick={createGroup} disabled={!name}>Create group</button>
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <ul className="card divide-y divide-white/5">
          {groups.map((g) => (
            <li key={g.id}>
              <button type="button" className="w-full p-4 text-left hover:bg-white/5" onClick={() => openGroup(g)}>
                <span className="font-medium">{g.name}</span>
                <span className="ml-2 text-xs text-drip-muted">
                  {g.memberCount != null ? `${g.memberCount} member${g.memberCount === 1 ? '' : 's'}` : ''}
                </span>
              </button>
            </li>
          ))}
          {groups.length === 0 && <li className="p-6 text-sm text-drip-muted">No groups yet.</li>}
        </ul>
        {selected && (
          <div className="card space-y-4 p-6">
            <h2 className="font-semibold">{selected.name}</h2>

            <div>
              <p className="text-sm font-medium text-drip-muted">Members ({members.length})</p>
              <ul className="mt-2 max-h-40 space-y-1 overflow-y-auto text-sm">
                {members.map((m) => (
                  <li key={m.userId} className="flex justify-between gap-2 rounded-lg bg-white/5 px-2 py-1">
                    <span>{m.fullName}</span>
                    <span className="truncate text-drip-muted">{m.email}</span>
                  </li>
                ))}
              </ul>
              <p className="mt-4 text-xs text-drip-muted">Invite by email or wallet ID (DRIP…)</p>
              <ul className="mt-2 space-y-2">
                {inviteRows.map((value, index) => (
                  <li key={index} className="flex gap-2">
                    <input
                      className="input min-w-0 flex-1"
                      placeholder="e.g. demo@drippay.app"
                      value={value}
                      onChange={(e) => updateInviteRow(index, e.target.value)}
                    />
                    {inviteRows.length > 1 && (
                      <button
                        type="button"
                        className="btn-ghost shrink-0 px-3"
                        aria-label="Remove row"
                        onClick={() => removeInviteRow(index)}
                      >
                        <X size={18} />
                      </button>
                    )}
                    {index === inviteRows.length - 1 && (
                      <button
                        type="button"
                        className="btn-ghost shrink-0 px-3 text-drip-lime"
                        aria-label="Add another person"
                        onClick={addInviteRow}
                      >
                        <Plus size={20} />
                      </button>
                    )}
                  </li>
                ))}
              </ul>
              <button
                type="button"
                className="btn-primary mt-3 w-full"
                onClick={addMembers}
                disabled={!inviteRows.some((r) => r.trim())}
              >
                Add to group
              </button>
              {memberError && <p className="mt-2 text-sm text-red-400">{memberError}</p>}
            </div>

            <p className="text-sm text-drip-muted">Net balances (positive = others owe them)</p>
            <ul className="text-sm">
              {Object.entries(balances).map(([userId, amt]) => (
                <li key={userId} className="flex justify-between py-1">
                  <span>{memberLabel(userId)}</span>
                  <span>{formatINR(amt)}</span>
                </li>
              ))}
            </ul>
            <input className="input" placeholder="Expense title" value={expenseTitle} onChange={(e) => setExpenseTitle(e.target.value)} />
            <input className="input" type="number" placeholder="Total amount" value={expenseAmount} onChange={(e) => setExpenseAmount(e.target.value)} />
            <button type="button" className="btn-primary w-full" onClick={addExpense} disabled={!expenseTitle || !expenseAmount}>
              Add shared expense (equal split among {members.length} member{members.length === 1 ? '' : 's'})
            </button>
            {members.length < 2 && (
              <p className="text-xs text-drip-muted">Tip: add more members below to split bills between friends.</p>
            )}
          </div>
        )}
      </div>

      {expenseDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="card max-w-md w-full space-y-4 p-6">
            <h2 className="text-xl font-bold">Expense details</h2>
            <p className="text-lg font-medium">{expenseDetail.title}</p>
            <p className="text-sm text-drip-muted">
              Paid by {expenseDetail.paidByName} · {new Date(expenseDetail.createdAt).toLocaleString()}
            </p>
            <p className="text-2xl font-bold text-drip-lime">{formatINR(expenseDetail.totalAmount)}</p>
            <ul className="divide-y divide-white/5 text-sm">
              {expenseDetail.shares?.map((s) => (
                <li key={s.memberName} className="flex justify-between py-2">
                  <span>{s.memberName}</span>
                  <span>{formatINR(s.shareAmount)}</span>
                </li>
              ))}
            </ul>
            <button
              type="button"
              className="btn-primary w-full"
              onClick={() => {
                setExpenseDetail(null);
                setSearchParams({});
              }}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
