import { useEffect, useState } from 'react';
import api from '../api/client';

export default function Admin() {
  const [alerts, setAlerts] = useState([]);

  useEffect(() => {
    api.get('/admin/alerts').then((res) => setAlerts(res.data));
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold">Admin — alert review</h1>
      <p className="text-sm text-drip-muted">Review simulated FraudWatch alerts across users.</p>
      <ul className="mt-6 space-y-3">
        {alerts.map((a) => (
          <li key={a.id} className="card p-4 text-sm">
            <p className="font-medium">{a.severity} · {a.reviewStatus}</p>
            <p className="mt-1 text-drip-muted">{a.reason}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}
