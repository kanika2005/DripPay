import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ScanLine, Keyboard } from 'lucide-react';
import api from '../api/client';
import PayeeQuickList from '../components/PayeeQuickList';
import QrScanner from '../components/QrScanner';
import { formatINR, newIdempotencyKey } from '../utils/format';
import { looksLikeHandle, looksLikeWalletId, parseCollectPayload } from '../utils/collectPayload';
import { useToast } from '../context/ToastContext';

export default function SendMoney() {
  const { show } = useToast();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [mode, setMode] = useState('manual');
  const [step, setStep] = useState(1);
  const [recipientInput, setRecipientInput] = useState('');
  const [recipientId, setRecipientId] = useState('');
  const [recipientHandle, setRecipientHandle] = useState('');
  const [recipientName, setRecipientName] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [category, setCategory] = useState('TRANSFER');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const prefillHandle = searchParams.get('handle');
  const prefillWallet = searchParams.get('wallet');
  const [payees, setPayees] = useState({ recentlyPaid: [], mostContacted: [] });

  useEffect(() => {
    api.get('/insights/payees').then((res) => setPayees(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    const handle = prefillHandle;
    const wallet = prefillWallet;
    if (!handle && !wallet) return;
    const value = handle || wallet;
    setRecipientInput(value);
    setMode('manual');
    lookupRecipient(value);
    setSearchParams({}, { replace: true });
  }, [prefillHandle, prefillWallet]);

  async function lookupRecipient(input = recipientInput) {
    setError('');
    const trimmed = input.trim();
    if (!trimmed) {
      setError('Enter a wallet ID or @drippay handle');
      return;
    }

    try {
      if (looksLikeWalletId(trimmed)) {
        const id = trimmed.toUpperCase();
        const { data } = await api.get(`/wallets/lookup/${id}`);
        setRecipientId(id);
        setRecipientHandle('');
        setRecipientName(data.displayName);
        setStep(2);
        return;
      }

      const handle = looksLikeHandle(trimmed)
        ? (trimmed.toLowerCase().includes('@drippay') ? trimmed.toLowerCase() : `${trimmed.toLowerCase()}@drippay`)
        : null;

      if (!handle) {
        setError('Use DRIP… wallet ID or name@drippay');
        return;
      }

      const { data } = await api.get(`/collect/resolve/${encodeURIComponent(handle)}`);
      setRecipientId(data.walletId);
      setRecipientHandle(data.dripHandle);
      setRecipientName(data.displayName);
      setStep(2);
    } catch (err) {
      setError(err.response?.data?.message || 'Recipient not found');
    }
  }

  function onQrScan(decoded) {
    const handle = parseCollectPayload(decoded);
    if (!handle) {
      show('Not a DripPay collect QR — try again');
      return;
    }
    setRecipientInput(handle);
    setMode('manual');
    lookupRecipient(handle);
    show(`Scanned ${handle}`);
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
      <h1 className="text-2xl font-bold">Scan &amp; Pay</h1>
      <p className="text-sm text-drip-muted">
        Scan someone&apos;s DripPay QR or pay via <span className="font-mono">name@drippay</span> / wallet ID.
      </p>

      {step === 1 && (
        <>
          <div className="mt-4 flex gap-2">
            <button
              type="button"
              className={`flex flex-1 items-center justify-center gap-2 rounded-xl border px-3 py-2 text-sm ${
                mode === 'scan'
                  ? 'border-drip-primary bg-drip-primary/15 text-drip-primary'
                  : 'border-drip-border text-drip-muted'
              }`}
              onClick={() => setMode('scan')}
            >
              <ScanLine size={16} /> Scan QR
            </button>
            <button
              type="button"
              className={`flex flex-1 items-center justify-center gap-2 rounded-xl border px-3 py-2 text-sm ${
                mode === 'manual'
                  ? 'border-drip-primary bg-drip-primary/15 text-drip-primary'
                  : 'border-drip-border text-drip-muted'
              }`}
              onClick={() => setMode('manual')}
            >
              <Keyboard size={16} /> Enter manually
            </button>
          </div>

          {mode === 'scan' && (
            <div className="card mt-4 space-y-3 p-4">
              <p className="text-sm text-drip-muted">Point your camera at a DripPay collect QR from the Collect screen.</p>
              <QrScanner onScan={onQrScan} active={step === 1 && mode === 'scan'} />
            </div>
          )}

          {mode === 'manual' && (
            <div className="card mt-4 space-y-4 p-6">
              <label className="text-sm text-drip-muted">Recipient (wallet ID or @drippay)</label>
              <input
                className="input font-mono"
                value={recipientInput}
                onChange={(e) => setRecipientInput(e.target.value)}
                placeholder="friend@drippay or DRIPXXXXXXXX"
              />
              {error && <p className="text-sm text-red-400">{error}</p>}
              <button
                type="button"
                className="btn-primary w-full"
                onClick={() => lookupRecipient()}
                disabled={!recipientInput.trim()}
              >
                Continue
              </button>
            </div>
          )}

          <div className="card mt-4 p-4">
            <h2 className="text-sm font-semibold">Recently paid &amp; most contacted</h2>
            <div className="mt-3">
              <PayeeQuickList
                recentlyPaid={payees.recentlyPaid}
                mostContacted={payees.mostContacted}
              />
            </div>
          </div>
        </>
      )}

      {step === 2 && (
        <div className="card mt-6 space-y-4 p-6">
          <p className="text-sm">
            Sending to <span className="text-drip-foreground">{recipientName}</span>
            {recipientHandle ? (
              <span className="font-mono text-drip-primary"> ({recipientHandle})</span>
            ) : (
              <span className="font-mono text-drip-muted"> ({recipientId})</span>
            )}
          </p>
          <input
            className="input"
            type="number"
            min="0.01"
            step="0.01"
            placeholder="Amount (INR)"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <input className="input" placeholder="Note" value={note} onChange={(e) => setNote(e.target.value)} />
          <select className="input" value={category} onChange={(e) => setCategory(e.target.value)}>
            {['TRANSFER', 'FOOD', 'SHOPPING', 'ENTERTAINMENT', 'OTHER'].map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
          <div className="rounded-xl bg-black/30 p-4 text-sm">
            <p>
              Confirm transfer of <span className="font-semibold text-drip-lime">{formatINR(amount || 0)}</span>
            </p>
          </div>
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button
            type="button"
            className="btn-primary w-full"
            onClick={confirmSend}
            disabled={submitting || !amount}
          >
            {submitting ? 'Processing…' : 'Confirm payment'}
          </button>
          <button
            type="button"
            className="btn-ghost w-full"
            onClick={() => {
              setStep(1);
              setError('');
            }}
          >
            Back
          </button>
        </div>
      )}
    </div>
  );
}
