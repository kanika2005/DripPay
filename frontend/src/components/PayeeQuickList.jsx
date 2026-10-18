import { Link } from 'react-router-dom';
import { formatINR } from '../utils/format';

function payeeLink(payee) {
  if (payee.dripHandle) {
    return `/send?handle=${encodeURIComponent(payee.dripHandle)}`;
  }
  return `/send?wallet=${encodeURIComponent(payee.walletId)}`;
}

function initials(name) {
  const parts = (name || '?').trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return (name || '?').slice(0, 2).toUpperCase();
}

function PayeeRow({ payee, meta }) {
  return (
    <Link
      to={payeeLink(payee)}
      className="flex items-center gap-3 rounded-xl px-2 py-2 transition hover:bg-drip-border/20"
    >
      <span
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-drip-primary/20 text-sm font-semibold text-drip-primary"
        aria-hidden="true"
      >
        {initials(payee.displayName)}
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate font-medium text-sm">{payee.displayName}</p>
        <p className="truncate text-xs text-drip-muted">
          {payee.dripHandle || payee.walletId}
        </p>
      </div>
      {meta && <p className="text-xs text-drip-muted">{meta}</p>}
    </Link>
  );
}

export default function PayeeQuickList({ recentlyPaid = [], mostContacted = [], compact = false }) {
  const hasRecent = recentlyPaid.length > 0;
  const hasFrequent = mostContacted.length > 0;

  if (!hasRecent && !hasFrequent) {
    return (
      <p className="text-sm text-drip-muted">
        No payees yet. Send money once and they&apos;ll show up here for quick pay.
      </p>
    );
  }

  if (compact) {
    return (
      <ul className="space-y-0.5">
        {recentlyPaid.map((p) => (
          <li key={`r-${p.walletId}`}>
            <PayeeRow
              payee={p}
              meta={p.lastPaidAt ? formatINR(p.lastAmount) : null}
            />
          </li>
        ))}
      </ul>
    );
  }

  return (
    <div className="grid gap-6 md:grid-cols-2">
      <div>
        <h3 className="text-sm font-semibold text-drip-foreground">Recently paid</h3>
        {hasRecent ? (
          <ul className="mt-2 space-y-0.5">
            {recentlyPaid.map((p) => (
              <li key={`r-${p.walletId}`}>
                <PayeeRow
                  payee={p}
                  meta={p.lastAmount != null ? formatINR(p.lastAmount) : null}
                />
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-sm text-drip-muted">No outgoing payments yet.</p>
        )}
      </div>
      <div>
        <h3 className="text-sm font-semibold text-drip-foreground">Most contacted</h3>
        {hasFrequent ? (
          <ul className="mt-2 space-y-0.5">
            {mostContacted.map((p) => (
              <li key={`f-${p.walletId}`}>
                <PayeeRow payee={p} meta={`${p.transferCount}×`} />
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-sm text-drip-muted">Pay someone to build your frequent list.</p>
        )}
      </div>
    </div>
  );
}
