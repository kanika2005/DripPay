import { useEffect, useState } from 'react';
import api from '../api/client';
import { formatINR } from '../utils/format';
import { useToast } from '../context/ToastContext';

function budgetOverBalanceMessage(limitAmount, balance) {
  return `Budget amount (${formatINR(limitAmount)}) is more than your wallet balance (${formatINR(balance)}). Lower the budget or add demo funds.`;
}

export default function Budgets() {
  const { show } = useToast();
  const [budgets, setBudgets] = useState([]);
  const [balance, setBalance] = useState(null);
  const [categoryCode, setCategoryCode] = useState('FOOD');
  const [limit, setLimit] = useState('3000');
  const [error, setError] = useState('');

  function load() {
    api.get('/budgets').then((res) => setBudgets(res.data));
    api.get('/wallets/me').then((res) => setBalance(Number(res.data.balance)));
  }

  useEffect(() => { load(); }, []);

  async function save() {
    setError('');
    const amount = Number(limit);
    if (balance != null && amount > balance) {
      setError(budgetOverBalanceMessage(amount, balance));
      return;
    }
    try {
      await api.post('/budgets', { categoryCode, monthlyLimit: amount });
      show('Budget saved');
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not save budget');
    }
  }

  const limitNum = Number(limit);
  const overBalance = balance != null && limitNum > balance;

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <h1 className="text-2xl font-bold">Budgets</h1>
      <p className="text-sm text-drip-muted">
        Monthly spending cap per category. Limit cannot exceed your current wallet balance
        {balance != null && <> ({formatINR(balance)} available)</>}.
      </p>
      <div className="card space-y-4 p-6">
        <select className="input" value={categoryCode} onChange={(e) => setCategoryCode(e.target.value)}>
          {['FOOD', 'SHOPPING', 'TRANSPORT', 'ENTERTAINMENT', 'BILLS'].map((c) => <option key={c}>{c}</option>)}
        </select>
        <input
          className={`input ${overBalance ? 'border-red-400/60' : ''}`}
          type="number"
          min="1"
          step="0.01"
          value={limit}
          onChange={(e) => {
            setLimit(e.target.value);
            setError('');
          }}
        />
        {(error || overBalance) && (
          <p className="rounded-lg bg-red-500/10 px-3 py-2 text-sm text-red-400" role="alert">
            {error || budgetOverBalanceMessage(limitNum, balance)}
          </p>
        )}
        <button type="button" className="btn-primary w-full" onClick={save} disabled={overBalance || !limit}>
          Save monthly budget
        </button>
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
