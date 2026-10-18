import { Link } from 'react-router-dom';
import Logo from '../components/Logo';
import ThemeToggle from '../components/ThemeToggle';

export default function Landing() {
  return (
    <div className="min-h-screen bg-drip-bg">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-6 py-6">
        <Logo />
        <div className="flex items-center gap-2">
          <ThemeToggle />
          <Link to="/login" className="btn-ghost">Log in</Link>
          <Link to="/register" className="btn-primary">Get DripPay</Link>
        </div>
      </header>
      <section className="mx-auto grid max-w-6xl gap-10 px-6 py-16 lg:grid-cols-2 lg:items-center">
        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-drip-primary">
            Trust-focused fintech
          </p>
          <h1 className="mt-2 text-4xl font-bold leading-tight tracking-tight lg:text-5xl">
            Clear as water,{' '}
            <span className="text-drip-primary">exact as a ledger</span>.
          </h1>
          <p className="mt-4 text-lg text-drip-muted">
            DripPay is a portfolio-grade digital wallet with idempotent transfers, double-entry
            ledger, SpendSense analytics, and explainable risk alerts — simulated funds only.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/register" className="btn-primary">Create wallet</Link>
            <Link to="/login" className="btn-ghost">Demo login</Link>
          </div>
        </div>
        <div className="card border-drip-primary/20 bg-balance-gradient p-6 shadow-glow">
          <div className="text-sm text-drip-muted">Balance preview</div>
          <div className="mt-2 font-amount text-3xl font-semibold text-drip-foreground">₹ — — —</div>
          <p className="text-sm text-drip-muted">Sign in to load live balances from the API.</p>
          <ul className="mt-6 space-y-2 text-sm text-drip-muted">
            <li>SafeTransfer with Idempotency-Key</li>
            <li>INITIATED → PENDING → SUCCESS timeline</li>
            <li>Read-only AI assistant</li>
          </ul>
        </div>
      </section>
      <footer className="border-t border-drip-border py-6 text-center text-xs text-drip-muted">
        Simulated money — demo wallet only. Not a licensed payment service.
      </footer>
    </div>
  );
}
