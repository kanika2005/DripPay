import { useEffect, useState } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import api from '../api/client';
import { formatINR } from '../utils/format';

const COLORS = ['#DFFF68', '#8D7BFF', '#60A5FA', '#F472B6', '#FBBF24', '#34D399'];

export default function Analytics() {
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    api.get('/insights/categories').then((res) => setCategories(res.data));
  }, []);

  const chartData = categories.map((c) => ({ name: c.label, value: Number(c.amount) }));

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">SpendSense</h1>
      <p className="text-sm text-drip-muted">Category breakdown from your outgoing completed transactions this month.</p>
      {chartData.length === 0 ? (
        <div className="card p-8 text-center text-drip-muted">No spending data yet — send or categorize a transfer.</div>
      ) : (
        <>
          <div className="card h-80 p-4">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={chartData} dataKey="value" nameKey="name" innerRadius={60} outerRadius={100} paddingAngle={4}>
                  {chartData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip formatter={(v) => formatINR(v)} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <ul className="card divide-y divide-white/5">
            {categories.map((c) => (
              <li key={c.categoryCode} className="flex justify-between p-4 text-sm">
                <span>{c.label}</span>
                <span className="font-medium">{formatINR(c.amount)}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
