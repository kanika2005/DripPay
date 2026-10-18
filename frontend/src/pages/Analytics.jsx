import { useEffect, useState } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import api from '../api/client';
import { formatINR } from '../utils/format';

const CHART_COLORS = ['#14B8A6', '#38BDF8', '#A78BFA', '#14B8A6', '#38BDF8'];

export default function Analytics() {
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    api.get('/insights/categories').then((res) => setCategories(res.data));
  }, []);

  const chartData = categories.map((c) => ({ name: c.label, value: Number(c.amount) }));

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">SpendSense</h1>
      <p className="text-sm text-drip-muted">
        Category breakdown from settled outgoing transfers this month.
      </p>
      {chartData.length === 0 ? (
        <div className="card p-8 text-center text-drip-muted">
          No spending data yet — send or categorize a transfer.
        </div>
      ) : (
        <>
          <div className="card h-80 p-4">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={chartData}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={60}
                  outerRadius={100}
                  paddingAngle={4}
                >
                  {chartData.map((_, i) => (
                    <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(v) => formatINR(v)}
                  contentStyle={{
                    background: 'var(--drip-surface)',
                    border: '1px solid var(--drip-border)',
                    borderRadius: '12px',
                    color: 'var(--drip-text)',
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <ul className="card divide-y divide-drip-border">
            {categories.map((c) => (
              <li key={c.categoryCode} className="flex justify-between p-4 text-sm">
                <span>{c.label}</span>
                <span className="font-amount font-medium">{formatINR(c.amount)}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
