import { useEffect, useState } from 'react';
import api from '../api/client';

export default function Security() {
  const [alerts, setAlerts] = useState([]);

  useEffect(() => {
    api.get('/security/alerts').then((res) => setAlerts(res.data));
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold">FraudWatch</h1>
      <p className="mt-2 text-sm text-drip-muted">
        Rule-based alerts — not confirmed fraud. Each alert explains which rule triggered it.
      </p>
      <ul className="mt-6 space-y-3">
        {alerts.length === 0 ? (
          <li className="card p-6 text-sm text-drip-muted">No alerts. That&apos;s a good vibe.</li>
        ) : alerts.map((a) => (
          <li key={a.id} className="card p-4">
            <div className="flex items-center justify-between">
              <span className="rounded-full bg-drip-purple/20 px-2 py-0.5 text-xs text-drip-purple">{a.severity}</span>
              <span className="text-xs text-drip-muted">{new Date(a.createdAt).toLocaleString()}</span>
            </div>
            <p className="mt-2 text-sm">{a.reason}</p>
            <p className="mt-1 text-xs text-drip-muted">Status: {a.reviewStatus}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}
