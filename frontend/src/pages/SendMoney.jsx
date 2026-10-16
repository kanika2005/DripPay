import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { formatINR } from '../utils/format';
import { newIdempotencyKey } from '../utils/format';
import { useToast } from '../context/ToastContext';

export default function SendMoney() {
  const { show } = useToast();
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [recipientId, setRecipientId] = useState('');
  const [recipientName, setRecipientName] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [category, setCategory] = useState('TRANSFER');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function lookup() {
    setError('');
    try {
      const { data } = await api.get(`/wallets/lookup/${recipientId}`);
      setRecipientName(data.displayName);
      setStep(2);
    } catch (err) {
      setError(err.response?.data?.message || 'Wallet not found');
    }
  }

  async function confirmSend() {
    setSubmitting(true);
    setError('');
    const key = newIdempotencyKey();
    try {
      const { data } = await api.post(
        '/transfers',
        { recipientWalletId: recipientId, amount: Number(amount), note, categoryCode: category },
        { headers: { 'Idempotency-Key': key } },
      );
      show('SafeTransfer completed');
      navigate(`/transactions/${data.id}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Transfer failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-lg">
      <h1 className="text-2xl font-bold">SafeTransfer</h1>
      <p className="text-sm text-drip-muted">Idempotent, ledger-backed transfers between DripPay wallets.</p>

      {step === 1 && (
        <div className="card mt-6 space-y-4 p-6">
          <label className="text-sm text-drip-muted">Recipient wallet ID</label>
          <input className="input" value={recipientId} onChange={(e) => setRecipientId(e.target.value.trim().toUpperCase())} placeholder="DRIPXXXXXXXX" />
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button type="button" className="btn-primary w-full" onClick={lookup} disabled={!recipientId}>Continue</button>
        </div>
      )}

      {step === 2 && (
        <div className="card mt-6 space-y-4 p-6">
          <p className="text-sm">Sending to <span className="text-white">{recipientName}</span> ({recipientId})</p>
          <input className="input" type="number" min="0.01" placeholder="Amount (INR)" value={amount} onChange={(e) => setAmount(e.target.value)} />
          <input className="input" placeholder="Note" value={note} onChange={(e) => setNote(e.target.value)} />
          <select className="input" value={category} onChange={(e) => setCategory(e.target.value)}>
            {['TRANSFER', 'FOOD', 'SHOPPING', 'ENTERTAINMENT', 'OTHER'].map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
          <div className="rounded-xl bg-black/30 p-4 text-sm">
            <p>Confirm transfer of <span className="font-semibold text-drip-lime">{formatINR(amount || 0)}</span></p>
          </div>
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button type="button" className="btn-primary w-full" onClick={confirmSend} disabled={submitting || !amount}>
            {submitting ? 'Processing…' : 'Confirm SafeTransfer'}
          </button>
          <button type="button" className="btn-ghost w-full" onClick={() => setStep(1)}>Back</button>
        </div>
      )}
    </div>
  );
}
