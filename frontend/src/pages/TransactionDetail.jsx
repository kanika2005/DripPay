import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import api from '../api/client';
import { formatINR } from '../utils/format';

export default function TransactionDetail() {
  const { id } = useParams();
  const [tx, setTx] = useState(null);

  useEffect(() => {
    api.get(`/transactions/${id}`).then((res) => setTx(res.data));
  }, [id]);

  if (!tx) return <div className="h-40 animate-pulse rounded-2xl bg-white/5" />;

  return (
    <div className="mx-auto max-w-lg card p-8">
      <h1 className="text-xl font-bold">Receipt</h1>
      <p className="mt-2 text-sm text-drip-muted">SafeTransfer reference</p>
      <p className="font-mono text-drip-lime">{tx.referenceId}</p>
      <dl className="mt-6 space-y-3 text-sm">
        <Row label="Status" value={tx.status} />
        <Row label="Type" value={tx.type} />
        <Row label="Amount" value={formatINR(tx.amount)} />
        <Row label="From" value={tx.senderWalletId || '—'} />
        <Row label="To" value={tx.recipientWalletId || '—'} />
        <Row label="Category" value={tx.categoryCode || '—'} />
        <Row label="Note" value={tx.description || '—'} />
        <Row label="Date" value={new Date(tx.createdAt).toLocaleString()} />
      </dl>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between gap-4 border-b border-white/5 pb-2">
      <dt className="text-drip-muted">{label}</dt>
      <dd className="text-right">{value}</dd>
    </div>
  );
}
