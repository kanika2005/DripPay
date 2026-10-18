import { Droplet } from 'lucide-react';

export default function Logo({ compact = false }) {
  return (
    <div className="flex items-center gap-2.5">
      <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-drip-primary/15 ring-1 ring-drip-primary/35">
        <Droplet size={20} className="text-drip-primary" aria-hidden="true" />
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
