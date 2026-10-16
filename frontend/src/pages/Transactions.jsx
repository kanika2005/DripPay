import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { formatINR } from '../utils/format';

export default function Transactions() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

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
      <h1 className="text-2xl font-bold">Transaction history</h1>
      <div className="card mt-6 overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-drip-muted">Loading…</div>
        ) : items.length === 0 ? (
          <div className="p-8 text-center text-drip-muted">No transactions yet. Add demo funds or send money.</div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-white/5 text-drip-muted">
              <tr>
                <th className="p-4">Reference</th>
                <th className="p-4">Type</th>
                <th className="p-4">Amount</th>
                <th className="p-4">Date</th>
              </tr>
            </thead>
            <tbody>
              {items.map((tx) => (
                <tr key={tx.id} className="border-b border-white/5 hover:bg-white/5">
                  <td className="p-4"><Link className="text-drip-lime" to={`/transactions/${tx.id}`}>{tx.referenceId.slice(0, 8)}…</Link></td>
                  <td className="p-4">{tx.type}</td>
                  <td className="p-4">{formatINR(tx.amount)}</td>
                  <td className="p-4 text-drip-muted">{new Date(tx.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <div className="mt-4 flex justify-center gap-2">
        <button type="button" className="btn-ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Prev</button>
        <span className="px-3 py-2 text-sm text-drip-muted">Page {page + 1} / {Math.max(totalPages, 1)}</span>
        <button type="button" className="btn-ghost" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>Next</button>
      </div>
    </div>
  );
}
