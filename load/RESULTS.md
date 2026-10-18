# k6 load test results

Run after backend is up and you have a JWT (`demo@drippay.app` / `Demo1234!`):

```bash
TOKEN=$(curl -s -X POST http://localhost:18080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@drippay.app","password":"Demo1234!"}' | jq -r .accessToken)

k6 run -e BASE_URL=http://localhost:18080 -e TOKEN="$TOKEN" load/k6/transfers.js
```

## Latest run (template — update when you execute)

| Date | VUs | Duration | http_req_failed | p95 latency | Notes |
|------|-----|----------|-----------------|-------------|-------|
| _pending_ | 10 | 30s | — | — | Install [k6](https://k6.io/docs/get-started/installation/) locally |

Target from roadmap: document 50 concurrent users when infrastructure allows; start with 10 VUs smoke load above.
