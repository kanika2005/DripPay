import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Run (backend on 18080, demo user seeded):
 *   k6 run -e BASE_URL=http://localhost:18080 -e TOKEN=<jwt> load/k6/transfers.js
 *
 * Record results in load/RESULTS.md after each run.
 */
export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<800'],
  },
};

const base = __ENV.BASE_URL || 'http://localhost:18080';
const token = __ENV.TOKEN;

export default function () {
  if (!token) {
    throw new Error('Set TOKEN env to a valid JWT');
  }
  const headers = {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
    'Idempotency-Key': `k6-${__VU}-${__ITER}-${Date.now()}`,
  };
  const res = http.get(`${base}/api/v1/wallets/me`, { headers });
  check(res, { 'wallet 200': (r) => r.status === 200 });
  sleep(0.5);
}
