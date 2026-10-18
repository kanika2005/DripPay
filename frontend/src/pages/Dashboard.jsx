import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowDownLeft, ArrowUpRight, Plus, Send, Wallet } from 'lucide-react';
import api from '../api/client';
import SignedTransactionAmount from '../components/SignedTransactionAmount';
import StatusPill from '../components/StatusPill';
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
    return (
      <div className="space-y-4">
        <div className="skeleton h-36" />
        <div className="grid gap-4 md:grid-cols-3">
          <div className="skeleton h-24" />
          <div className="skeleton h-24" />
          <div className="skeleton h-24" />
        </div>
        <div className="skeleton h-48" />
      </div>
    );
  }

  const empty = Number(wallet?.balance) === 0 && txs.length === 0;

  return (
    <div className="space-y-6">
      <div className="card overflow-hidden border-drip-primary/20 bg-balance-gradient p-6 shadow-glow">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-drip-muted">Available balance</p>
            <h1 className="mt-1 font-amount text-4xl font-semibold tracking-tight text-drip-foreground">
              {formatINR(wallet?.balance)}
            </h1>
            <p className="mt-2 font-mono text-xs text-drip-muted">
              {wallet?.publicWalletId}
            </p>
          </div>
        </div>
        <div className="mt-6 flex flex-wrap gap-3">
          <Link to="/send" className="btn-primary">
            <Send size={16} /> Send
          </Link>
          <Link to="/wallet" className="btn-ghost">
            <Plus size={16} /> Add demo funds
          </Link>
          <Link to="/wallet" className="btn-ghost">
            <Wallet size={16} /> Wallet
          </Link>
        </div>
      </div>

      {empty && (
        <div className="card border-drip-accent/25 p-6">
          <h2 className="font-semibold">Welcome to DripPay</h2>
          <p className="mt-2 text-sm text-drip-muted">
            Add demo funds from Wallet, then send with a required Idempotency-Key for safe transfers.
          </p>
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-3">
        <Stat icon={ArrowUpRight} label="Sent this month" value={formatINR(summary?.monthlySent)} />
        <Stat icon={ArrowDownLeft} label="Received this month" value={formatINR(summary?.monthlyReceived)} />
        <Stat icon={Send} label="Monthly spending" value={formatINR(summary?.monthlySpending)} />
      </div>

      {summary?.insights?.length > 0 && (
        <div className="card p-6">
          <h2 className="font-semibold text-drip-accent">SpendSense</h2>
          <ul className="mt-3 space-y-2 text-sm text-drip-muted">
            {summary.insights.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="card p-6">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold">Recent transactions</h2>
          <Link to="/transactions" className="text-sm font-medium text-drip-primary hover:underline">
            View all
          </Link>
        </div>
        {txs.length === 0 ? (
          <p className="mt-4 text-sm text-drip-muted">No transactions yet.</p>
        ) : (
          <ul className="mt-4 divide-y divide-drip-border">
            {txs.map((tx) => (
              <li key={tx.id} className="flex flex-wrap items-center justify-between gap-2 py-3 text-sm">
                <div>
                  <p className="font-medium">{tx.type.replace(/_/g, ' ')}</p>
                  <p className="text-drip-muted">{new Date(tx.createdAt).toLocaleString()}</p>
                </div>
                <div className="flex items-center gap-3">
                  <StatusPill status={tx.status} />
                  <Link to={`/transactions/${tx.id}`} className="hover:opacity-90">
                    <SignedTransactionAmount tx={tx} myWalletId={wallet?.publicWalletId} />
                  </Link>
                </div>
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
      <div className="flex items-center gap-2 text-drip-muted">
        <Icon size={16} aria-hidden="true" />
        {label}
      </div>
      <p className="mt-2 font-amount text-xl font-semibold">{value}</p>
    </div>
  );
}
