import { useEffect, useState } from 'react';
import api from '../api/client';
import { formatINR } from '../utils/format';
import { useToast } from '../context/ToastContext';

export default function WalletPage() {
  const { show } = useToast();
  const [wallet, setWallet] = useState(null);
  const [amount, setAmount] = useState('500');
  const [loading, setLoading] = useState(true);

  async function load() {
    const { data } = await api.get('/wallets/me');
    setWallet(data);
    setLoading(false);
  }

  useEffect(() => { load(); }, []);

  async function deposit() {
    await api.post('/wallets/demo-deposits', { amount: Number(amount), note: 'Demo top-up' });
    show('Demo funds added');
    load();
  }

  async function withdraw() {
    await api.post('/wallets/demo-withdrawals', { amount: Number(amount), note: 'Demo withdrawal' });
    show('Demo withdrawal completed');
    load();
  }

  if (loading) return <div className="h-40 animate-pulse rounded-2xl bg-white/5" />;

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div className="card p-6">
        <h1 className="text-2xl font-bold">Wallet</h1>
        <p className="mt-4 text-3xl font-bold text-drip-lime">{formatINR(wallet.balance)}</p>
        <p className="text-sm text-drip-muted">Public ID: {wallet.publicWalletId}</p>
      </div>
      <div className="card p-6 space-y-4">
        <h2 className="font-semibold">Demo funds (simulated)</h2>
        <input className="input" type="number" min="1" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />
        <div className="flex gap-3">
          <button type="button" className="btn-primary flex-1" onClick={deposit}>Add demo funds</button>
          <button type="button" className="btn-ghost flex-1" onClick={withdraw}>Withdraw demo</button>
        </div>
        <p className="text-xs text-drip-muted">Simulated only — not real money or banking.</p>
      </div>
      <div className="card space-y-4 p-6">
        <h2 className="font-semibold">Razorpay (test mode)</h2>
        <p className="text-sm text-drip-muted">
          Creates a test order and credits your wallet after verify. Live keys use real Razorpay when configured.
        </p>
        <button
          type="button"
          className="btn-primary w-full"
          onClick={async () => {
            const amt = Number(amount) || 500;
            const { data: order } = await api.post('/payments/razorpay/orders', { amount: amt });
            await api.post('/payments/razorpay/verify', {
              orderId: order.orderId,
              paymentId: order.simulated ? `pay_sim_${Date.now()}` : undefined,
              signature: order.simulated ? 'simulated' : undefined,
            });
            show('Razorpay test payment credited');
            load();
          }}
        >
          Pay ₹{amount || '500'} (simulated checkout)
        </button>
      </div>
    </div>
  );
}
