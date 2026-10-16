export default function Logo({ compact = false }) {
  return (
    <div className="flex items-center gap-2">
      <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-drip-lime/10 ring-1 ring-drip-lime/30">
        <span className="text-lg font-bold text-drip-lime">D</span>
      </div>
      {!compact && (
        <div>
          <div className="text-lg font-bold leading-tight">DripPay</div>
          <div className="text-xs text-drip-muted">Your Money. Your Vibe.</div>
        </div>
      )}
    </div>
  );
}
