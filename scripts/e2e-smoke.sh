#!/usr/bin/env bash
set -euo pipefail
BASE="${BASE_URL:-http://localhost:18080/api/v1}"
LOG="${1:-reports/logs/e2e-smoke.log}"
mkdir -p "$(dirname "$LOG")"
exec > >(tee "$LOG") 2>&1

echo "=== DripPay E2E smoke $(date -u +%Y-%m-%dT%H:%M:%SZ) ==="
echo "BASE=$BASE"

SUFFIX=$(date +%s)
EMAIL_A="e2e_a_${SUFFIX}@drippay.test"
EMAIL_B="e2e_b_${SUFFIX}@drippay.test"

REG_A=$(curl -s -w "\n%{http_code}" -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL_A\",\"password\":\"Password123!\",\"fullName\":\"E2E A\"}")
HTTP_A=$(echo "$REG_A" | tail -1)
BODY_A=$(echo "$REG_A" | sed '$d')
echo "register A: HTTP $HTTP_A"
TOKEN_A=$(echo "$BODY_A" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
WALLET_B=$(curl -s -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL_B\",\"password\":\"Password123!\",\"fullName\":\"E2E B\"}" | python3 -c "import sys,json; print(json.load(sys.stdin)['user']['publicWalletId'])")

BAL_BEFORE=$(curl -s -H "Authorization: Bearer $TOKEN_A" "$BASE/wallets/me" | python3 -c "import sys,json; print(json.load(sys.stdin)['balance'])")
curl -s -o /dev/null -w "deposit: HTTP %{http_code}\n" -X POST "$BASE/wallets/demo-deposits" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' -d '{"amount":1000}'
BAL_AFTER_DEP=$(curl -s -H "Authorization: Bearer $TOKEN_A" "$BASE/wallets/me" | python3 -c "import sys,json; print(json.load(sys.stdin)['balance'])")
echo "balance before deposit: $BAL_BEFORE after: $BAL_AFTER_DEP"

IDEM_KEY="e2e-key-$SUFFIX"
TX1=$(curl -s -w "\n%{http_code}" -X POST "$BASE/transfers" \
  -H "Authorization: Bearer $TOKEN_A" -H "Idempotency-Key: $IDEM_KEY" -H 'Content-Type: application/json' \
  -d "{\"recipientWalletId\":\"$WALLET_B\",\"amount\":250,\"note\":\"e2e\"}")
echo "transfer 1: HTTP $(echo "$TX1" | tail -1)"
TX2=$(curl -s -w "\n%{http_code}" -X POST "$BASE/transfers" \
  -H "Authorization: Bearer $TOKEN_A" -H "Idempotency-Key: $IDEM_KEY" -H 'Content-Type: application/json' \
  -d "{\"recipientWalletId\":\"$WALLET_B\",\"amount\":250,\"note\":\"e2e\"}")
echo "transfer duplicate: HTTP $(echo "$TX2" | tail -1)"
BAL_FINAL=$(curl -s -H "Authorization: Bearer $TOKEN_A" "$BASE/wallets/me" | python3 -c "import sys,json; print(json.load(sys.stdin)['balance'])")
echo "balance after transfers: $BAL_FINAL (expect 750.0000)"

FAIL_TX=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/transfers" \
  -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d "{\"recipientWalletId\":\"$WALLET_B\",\"amount\":99999}")
echo "insufficient transfer: HTTP $FAIL_TX (expect 400)"
echo "=== E2E smoke complete ==="
