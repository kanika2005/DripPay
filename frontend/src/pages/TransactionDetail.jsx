import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { BadgeCheck } from 'lucide-react';
import api from '../api/client';
import StatusPill from '../components/StatusPill';
import TransactionTimeline from '../components/TransactionTimeline';
import SignedTransactionAmount from '../components/SignedTransactionAmount';
import { useAuth } from '../context/AuthContext';
import { formatINR } from '../utils/format';
import { isSettledStatus } from '../utils/transactionStatus';

export default function TransactionDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const [tx, setTx] = useState(null);

  useEffect(() => {
    api.get(`/transactions/${id}`).then((res) => setTx(res.data));
  }, [id]);

  if (!tx) return <div className="skeleton mx-auto h-64 max-w-lg" />;

  const settled = isSettledStatus(tx.status);

  return (
    <div className="mx-auto max-w-lg space-y-4">
      <div className="card p-8">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <h1 className="text-xl font-bold">Transaction</h1>
          <StatusPill status={tx.status} />
        </div>
        <p className="mt-2 text-sm text-drip-muted">Reference</p>
        <p className="font-mono text-sm text-drip-primary">{tx.referenceId}</p>

        <div className="mt-6 font-amount text-3xl font-semibold">
          <SignedTransactionAmount tx={tx} myWalletId={user?.publicWalletId} />
        </div>

        {settled && (
          <div
            className="mt-4 flex items-start gap-2 rounded-xl border border-drip-credit/30 bg-drip-credit/10 p-3 text-sm text-drip-foreground"
            role="status"
          >
            <BadgeCheck className="mt-0.5 shrink-0 text-drip-credit" size={18} aria-hidden="true" />
            <div>
              <p className="font-medium text-drip-credit">Verified by ledger</p>
              <p className="mt-1 text-drip-muted">
                This transfer posted a matching debit and credit in the double-entry ledger.
              </p>
            </div>
          </div>
        )}

        <dl className="mt-6 space-y-3 text-sm">
          <Row label="Type" value={tx.type.replace(/_/g, ' ')} />
          <Row label="From" value={tx.senderWalletId || '—'} mono />
          <Row label="To" value={tx.recipientWalletId || '—'} mono />
          <Row label="Category" value={tx.categoryCode || '—'} />
          <Row label="Note" value={tx.description || '—'} />
          <Row label="Date" value={new Date(tx.createdAt).toLocaleString()} />
          <Row label="Amount (raw)" value={formatINR(tx.amount)} mono />
        </dl>
      </div>

      <TransactionTimeline status={tx.status} />
    </div>
  );
}

function Row({ label, value, mono = false }) {
  return (
    <div className="flex justify-between gap-4 border-b border-drip-border pb-2">
      <dt className="text-drip-muted">{label}</dt>
      <dd className={`text-right ${mono ? 'font-mono text-xs' : ''}`}>{value}</dd>
    </div>
  );
}
