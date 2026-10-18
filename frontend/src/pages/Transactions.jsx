import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import SignedTransactionAmount from '../components/SignedTransactionAmount';
import StatusPill from '../components/StatusPill';
import { useAuth } from '../context/AuthContext';

export default function Transactions() {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  async function downloadStatement(format) {
    const res = await api.get('/statements/export', {
      params: { format },
      responseType: 'blob',
    });
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = format === 'pdf' ? 'drippay-statement.pdf' : 'drippay-statement.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  useEffect(() => {
    setLoading(true);
    api.get('/transactions', { params: { page, size: 15 } }).then((res) => {
      setItems(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
      setLoading(false);
    });
  }, [page]);

  return (
    <div>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Transaction history</h1>
          <p className="mt-1 text-sm text-drip-muted">Ledger-backed entries · newest first</p>
        </div>
        <div className="flex gap-2">
          <button type="button" className="btn-ghost text-sm" onClick={() => downloadStatement('csv')}>Export CSV</button>
          <button type="button" className="btn-ghost text-sm" onClick={() => downloadStatement('pdf')}>Export PDF</button>
        </div>
      </div>
      <div className="card mt-6 max-h-[70vh] overflow-auto">
        {loading ? (
          <div className="space-y-2 p-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="skeleton h-12" />
            ))}
          </div>
        ) : items.length === 0 ? (
          <div className="p-8 text-center text-drip-muted">No transactions yet. Add demo funds or send money.</div>
        ) : (
          <table className="table-sticky w-full text-left text-sm">
            <thead className="border-b border-drip-border text-drip-muted">
              <tr>
                <th className="p-4 font-medium">Reference</th>
                <th className="p-4 font-medium">Type</th>
                <th className="p-4 font-medium">Status</th>
                <th className="p-4 font-medium">Amount</th>
                <th className="p-4 font-medium">Date</th>
              </tr>
            </thead>
            <tbody>
              {items.map((tx) => (
                <tr key={tx.id} className="border-b border-drip-border hover:bg-drip-border/15">
                  <td className="p-4">
                    <Link className="font-mono text-drip-primary hover:underline" to={`/transactions/${tx.id}`}>
                      {tx.referenceId.slice(0, 10)}…
                    </Link>
                  </td>
                  <td className="p-4">{tx.type.replace(/_/g, ' ')}</td>
                  <td className="p-4">
                    <StatusPill status={tx.status} />
                  </td>
                  <td className="p-4">
                    <SignedTransactionAmount tx={tx} myWalletId={user?.publicWalletId} />
                  </td>
                  <td className="p-4 text-drip-muted">{new Date(tx.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <div className="mt-4 flex justify-center gap-2">
        <button type="button" className="btn-ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
          Prev
        </button>
        <span className="px-3 py-2 text-sm text-drip-muted">
          Page {page + 1} / {Math.max(totalPages, 1)}
        </span>
        <button
          type="button"
          className="btn-ghost"
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
