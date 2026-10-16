import { useEffect, useState } from 'react';
import api from '../api/client';
import { formatINR } from '../utils/format';
import { useToast } from '../context/ToastContext';

export default function Groups() {
  const { show } = useToast();
  const [groups, setGroups] = useState([]);
  const [name, setName] = useState('');
  const [selected, setSelected] = useState(null);
  const [balances, setBalances] = useState({});
  const [expenseTitle, setExpenseTitle] = useState('');
  const [expenseAmount, setExpenseAmount] = useState('');

  function load() {
    api.get('/groups').then((res) => setGroups(res.data));
  }

  useEffect(() => { load(); }, []);

  async function createGroup() {
    await api.post('/groups', { name, memberUserIds: [] });
    setName('');
    show('Group created');
    load();
  }

  async function openGroup(g) {
    setSelected(g);
    const { data } = await api.get(`/groups/${g.id}/balances`);
    setBalances(data);
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

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">SplitSmart</h1>
      <div className="card flex flex-wrap gap-3 p-4">
        <input className="input max-w-xs" placeholder="Group name" value={name} onChange={(e) => setName(e.target.value)} />
        <button type="button" className="btn-primary" onClick={createGroup} disabled={!name}>Create group</button>
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <ul className="card divide-y divide-white/5">
          {groups.map((g) => (
            <li key={g.id}>
              <button type="button" className="w-full p-4 text-left hover:bg-white/5" onClick={() => openGroup(g)}>
                {g.name}
              </button>
            </li>
          ))}
          {groups.length === 0 && <li className="p-6 text-sm text-drip-muted">No groups yet.</li>}
        </ul>
        {selected && (
          <div className="card space-y-4 p-6">
            <h2 className="font-semibold">{selected.name}</h2>
            <p className="text-sm text-drip-muted">Net balances (positive = others owe user)</p>
            <ul className="text-sm">
              {Object.entries(balances).map(([userId, amt]) => (
                <li key={userId} className="flex justify-between py-1">
                  <span>User #{userId}</span>
                  <span>{formatINR(amt)}</span>
                </li>
              ))}
            </ul>
            <input className="input" placeholder="Expense title" value={expenseTitle} onChange={(e) => setExpenseTitle(e.target.value)} />
            <input className="input" type="number" placeholder="Total amount" value={expenseAmount} onChange={(e) => setExpenseAmount(e.target.value)} />
            <button type="button" className="btn-primary w-full" onClick={addExpense}>Add shared expense (equal split)</button>
          </div>
        )}
      </div>
    </div>
  );
}
