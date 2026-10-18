import { useState } from 'react';
import api from '../api/client';
import { useToast } from '../context/ToastContext';

export default function PaymentDrafts() {
  const { show } = useToast();
  const [draftId, setDraftId] = useState('');
  const [pin, setPin] = useState('');
  const [draft, setDraft] = useState(null);

  async function loadDraft() {
    const { data } = await api.get(`/payment-drafts/${draftId}`);
    setDraft(data);
  }

  async function confirm() {
    await api.post(`/payment-drafts/${draftId}/confirm`, { pin });
    show('Payment sent');
    setDraft(null);
    setPin('');
  }

  return (
    <div className="mx-auto max-w-lg card space-y-4 p-6">
      <h1 className="text-2xl font-bold">Confirm payment draft</h1>
      <p className="text-sm text-drip-muted">
        Drafts from the Assistant expire in 30 minutes. Enter draft ID and your transfer PIN.
      </p>
      <input className="input" placeholder="Draft ID" value={draftId} onChange={(e) => setDraftId(e.target.value)} />
      <button type="button" className="btn-ghost" onClick={loadDraft}>Load draft</button>
      {draft && (
        <div className="rounded-xl border border-drip-border p-3 text-sm">
          <p>To: {draft.recipientHandle}</p>
          <p>Amount: ₹{draft.amount}</p>
          <p>Status: {draft.status}</p>
        </div>
      )}
      <input
        className="input"
        type="password"
        inputMode="numeric"
        maxLength={6}
        placeholder="Transfer PIN"
        value={pin}
        onChange={(e) => setPin(e.target.value)}
      />
      <button type="button" className="btn-primary" onClick={confirm} disabled={!draftId || !pin}>
        Confirm with PIN
      </button>
    </div>
  );
}
