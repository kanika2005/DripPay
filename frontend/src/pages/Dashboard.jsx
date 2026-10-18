import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowDownLeft, ArrowUpRight, Plus, Send, Wallet } from 'lucide-react';
import api from '../api/client';
import PayeeQuickList from '../components/PayeeQuickList';
import { formatINR } from '../utils/format';

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [payees, setPayees] = useState({ recentlyPaid: [], mostContacted: [] });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [s, w, p] = await Promise.all([
          api.get('/insights/summary'),
          api.get('/wallets/me'),
          api.get('/insights/payees'),
        ]);
        setSummary(s.data);
        setWallet(w.data);
        setPayees(p.data);
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

  const empty = Number(wallet?.balance) === 0
    && payees.recentlyPaid.length === 0
    && payees.mostContacted.length === 0;

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

      <div className="card p-6">
        <div className="flex items-center justify-between gap-2">
          <h2 className="font-semibold">Pay again</h2>
          <Link to="/send" className="text-sm font-medium text-drip-primary hover:underline">
            Scan &amp; Pay
          </Link>
        </div>
        <div className="mt-4">
          <PayeeQuickList
            recentlyPaid={payees.recentlyPaid}
            mostContacted={payees.mostContacted}
          />
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

      <p className="text-center text-sm text-drip-muted">
        Full history in{' '}
        <Link to="/transactions" className="font-medium text-drip-primary hover:underline">
          Transactions
        </Link>
      </p>
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
