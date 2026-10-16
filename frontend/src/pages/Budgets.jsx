import { useEffect, useState } from 'react';
import api from '../api/client';
import { formatINR } from '../utils/format';
import { useToast } from '../context/ToastContext';

export default function Budgets() {
  const { show } = useToast();
  const [budgets, setBudgets] = useState([]);
  const [categoryCode, setCategoryCode] = useState('FOOD');
  const [limit, setLimit] = useState('3000');

  function load() {
    api.get('/budgets').then((res) => setBudgets(res.data));
  }

  useEffect(() => { load(); }, []);

  async function save() {
    await api.post('/budgets', { categoryCode, monthlyLimit: Number(limit) });
    show('Budget saved');
    load();
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <h1 className="text-2xl font-bold">Budgets</h1>
      <div className="card space-y-4 p-6">
        <select className="input" value={categoryCode} onChange={(e) => setCategoryCode(e.target.value)}>
          {['FOOD', 'SHOPPING', 'TRANSPORT', 'ENTERTAINMENT', 'BILLS'].map((c) => <option key={c}>{c}</option>)}
        </select>
        <input className="input" type="number" value={limit} onChange={(e) => setLimit(e.target.value)} />
        <button type="button" className="btn-primary w-full" onClick={save}>Save monthly budget</button>
      </div>
      <ul className="card divide-y divide-white/5">
        {budgets.length === 0 ? (
          <li className="p-6 text-sm text-drip-muted">No budgets set for this month.</li>
        ) : budgets.map((b) => (
          <li key={b.id} className="flex justify-between p-4 text-sm">
            <span>{b.categoryCode} · {b.monthYear}</span>
            <span>{formatINR(b.monthlyLimit)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
