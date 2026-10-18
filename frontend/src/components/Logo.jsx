import DripPayMark from './DripPayMark';

export default function Logo({ compact = false }) {
  return (
    <div className="flex items-center gap-2.5">
      <div
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-drip-surface ring-1 ring-drip-border text-drip-foreground"
        aria-hidden="true"
      >
        <DripPayMark size={28} />
      </div>
      {!compact && (
        <div>
          <div className="text-lg font-bold leading-tight tracking-tight">DripPay</div>
          <div className="text-[11px] text-drip-muted">Clear as water, exact as a ledger</div>
        </div>
      )}
    </div>
  );
}
