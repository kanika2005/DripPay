import { useEffect, useState } from 'react';
import api from '../api/client';

export default function Admin() {
  const [alerts, setAlerts] = useState([]);
  const [recon, setRecon] = useState(null);
  const [explanations, setExplanations] = useState({});

  function load() {
    api.get('/admin/alerts').then((res) => setAlerts(res.data));
    api.get('/admin/reconciliation').then((res) => setRecon(res.data));
  }

  useEffect(() => {
    load();
  }, []);

  async function runRecon() {
    const { data } = await api.post('/admin/reconciliation/run');
    setRecon(data);
  }

  async function explain(alertId) {
    const { data } = await api.get(`/admin/alerts/${alertId}/explain`);
    setExplanations((e) => ({ ...e, [alertId]: data.explanation }));
  }

  return (
    <div>
      <h1 className="text-2xl font-bold">Admin</h1>
      <p className="text-sm text-drip-muted">Reconciliation, FraudWatch alerts, and AI explanations.</p>

      <section className="card mt-6 p-4">
        <div className="flex items-center justify-between gap-4">
          <h2 className="font-semibold">Ledger reconciliation</h2>
          <button type="button" className="btn-ghost text-sm" onClick={runRecon}>
            Run now
          </button>
        </div>
        {recon && (
          <p className="mt-2 text-sm text-drip-muted">
            {recon.balanced ? 'Balanced — no mismatches.' : `${recon.mismatches?.length ?? 0} mismatch(es).`}
            {recon.ranAt && ` Last run: ${new Date(recon.ranAt).toLocaleString()}.`}
          </p>
        )}
      </section>

      <h2 className="mt-8 font-semibold">Alerts</h2>
      <ul className="mt-4 space-y-3">
        {alerts.map((a) => (
          <li key={a.id} className="card p-4 text-sm">
            <p className="font-medium">{a.severity} · {a.reviewStatus}</p>
            <p className="mt-1 text-drip-muted">{a.reason}</p>
            <button type="button" className="btn-ghost mt-2 text-xs" onClick={() => explain(a.id)}>
              Explain (AI)
            </button>
            {explanations[a.id] && <p className="mt-2 text-drip-muted">{explanations[a.id]}</p>}
          </li>
        ))}
      </ul>
    </div>
  );
}
