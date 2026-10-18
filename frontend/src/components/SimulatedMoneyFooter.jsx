import { Droplets } from 'lucide-react';

export default function SimulatedMoneyFooter() {
  return (
    <footer className="mt-auto border-t border-drip-border px-4 py-3 text-center text-xs text-drip-muted lg:px-8">
      <p className="inline-flex items-center justify-center gap-1.5">
        <Droplets size={14} className="text-drip-primary" aria-hidden="true" />
        <span>
          Simulated money — demo wallet only. Balances are not real funds.
        </span>
      </p>
    </footer>
  );
}
