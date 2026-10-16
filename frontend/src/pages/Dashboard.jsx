import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowDownLeft, ArrowUpRight, Plus, Send } from 'lucide-react';
import api from '../api/client';
import { formatINR } from '../utils/format';

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [txs, setTxs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [s, w, t] = await Promise.all([
          api.get('/insights/summary'),
          api.get('/wallets/me'),
          api.get('/transactions', { params: { page: 0, size: 5 } }),
        ]);
        setSummary(s.data);
        setWallet(w.data);
        setTxs(t.data.content || []);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) {
    return <div className="animate-pulse space-y-4"><div className="h-32 rounded-2xl bg-white/5" /><div className="h-48 rounded-2xl bg-white/5" /></div>;
  }

  const empty = Number(wallet?.balance) === 0 && txs.length === 0;

  return (
    <div className="space-y-6">
      <div className="card p-6 shadow-glow">
        <p className="text-sm text-drip-muted">Available balance</p>
        <h1 className="mt-1 text-4xl font-bold text-drip-lime">{formatINR(wallet?.balance)}</h1>
        <p className="mt-2 text-sm text-drip-muted">Wallet ID: {wallet?.publicWalletId}</p>
        <div className="mt-6 flex flex-wrap gap-3">
          <Link to="/send" className="btn-primary"><Send size={16} /> Send</Link>
          <Link to="/wallet" className="btn-ghost"><Plus size={16} /> Add demo funds</Link>
          <Link to="/transactions" className="btn-ghost">Transactions</Link>
        </div>
      </div>

      {empty && (
        <div className="card border-drip-purple/30 p-6">
          <h2 className="font-semibold">Welcome to DripPay 👋</h2>
          <p className="mt-2 text-sm text-drip-muted">Add demo funds from your wallet page, then try SafeTransfer to another user.</p>
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-3">
        <Stat icon={ArrowUpRight} label="Sent this month" value={formatINR(summary?.monthlySent)} />
        <Stat icon={ArrowDownLeft} label="Received this month" value={formatINR(summary?.monthlyReceived)} />
        <Stat icon={Send} label="Monthly spending" value={formatINR(summary?.monthlySpending)} />
      </div>

      {summary?.insights?.length > 0 && (
        <div className="card p-6">
          <h2 className="font-semibold text-drip-purple">SpendSense</h2>
          <ul className="mt-3 space-y-2 text-sm text-drip-muted">
            {summary.insights.map((line) => <li key={line}>• {line}</li>)}
          </ul>
        </div>
      )}

      <div className="card p-6">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold">Recent transactions</h2>
          <Link to="/transactions" className="text-sm text-drip-lime">View all</Link>
        </div>
        {txs.length === 0 ? (
          <p className="mt-4 text-sm text-drip-muted">No transactions yet.</p>
        ) : (
          <ul className="mt-4 divide-y divide-white/5">
            {txs.map((tx) => (
              <li key={tx.id} className="flex items-center justify-between py-3 text-sm">
                <div>
                  <p className="font-medium">{tx.type.replace('_', ' ')}</p>
                  <p className="text-drip-muted">{new Date(tx.createdAt).toLocaleString()}</p>
                </div>
                <Link to={`/transactions/${tx.id}`} className="text-drip-lime">{formatINR(tx.amount)}</Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function Stat({ icon: Icon, label, value }) {
  return (
    <div className="card p-4">
      <div className="flex items-center gap-2 text-drip-muted"><Icon size={16} /> {label}</div>
      <p className="mt-2 text-xl font-semibold">{value}</p>
    </div>
  );
}
