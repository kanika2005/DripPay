import { TX_TIMELINE, getStatusMeta, timelineIndex } from '../utils/transactionStatus';

export default function TransactionTimeline({ status }) {
  const current = timelineIndex(status);
  const terminal = status === 'FAILED' || status === 'REVERSED';

  if (terminal) {
    const meta = getStatusMeta(status);
    return (
      <div className="rounded-xl border border-drip-border bg-drip-surface/50 p-4">
        <p className="text-xs font-medium uppercase tracking-wide text-drip-muted">Timeline</p>
        <p className="mt-2 text-sm text-drip-foreground">
          Transaction ended in <StatusPillInline status={status} />
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-drip-border bg-drip-surface/50 p-4">
      <p className="text-xs font-medium uppercase tracking-wide text-drip-muted">Timeline</p>
      <ol className="mt-4 flex flex-col gap-0 sm:flex-row sm:items-center sm:gap-2">
        {TX_TIMELINE.map((step, i) => {
          const done = i < current;
          const active = i === current;
          const meta = getStatusMeta(step);
          return (
            <li key={step} className="flex flex-1 items-center gap-2 sm:flex-col sm:text-center">
              <div
                className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                  active ? 'bg-drip-primary text-drip-bg' : done ? 'bg-drip-credit/20 text-drip-credit' : 'bg-drip-border text-drip-muted'
                }`}
                aria-current={active ? 'step' : undefined}
              >
                {done ? '✓' : meta.icon}
              </div>
              <span className={`text-xs ${active ? 'font-semibold text-drip-primary' : 'text-drip-muted'}`}>
                {meta.label}
              </span>
              {i < TX_TIMELINE.length - 1 && (
                <span className="hidden h-px flex-1 bg-drip-border sm:block" aria-hidden="true" />
              )}
            </li>
          );
        })}
      </ol>
    </div>
  );
}

function StatusPillInline({ status }) {
  const { label, pill, icon } = getStatusMeta(status);
  return (
    <span className={`status-pill ${pill} ml-1 inline-flex`}>
      <span aria-hidden="true">{icon}</span>
      <span>{label}</span>
    </span>
  );
}
