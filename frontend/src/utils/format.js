export function formatINR(amount) {
  const n = Number(amount ?? 0);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
  }).format(n);
}

export function newIdempotencyKey() {
  return crypto.randomUUID();
}
