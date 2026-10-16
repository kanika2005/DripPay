import { Link } from 'react-router-dom';
import Logo from '../components/Logo';

export default function Landing() {
  return (
    <div className="min-h-screen bg-drip-bg">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-6 py-6">
        <Logo />
        <div className="flex gap-3">
          <Link to="/login" className="btn-ghost">Log in</Link>
          <Link to="/register" className="btn-primary">Get DripPay</Link>
        </div>
      </header>
      <section className="mx-auto grid max-w-6xl gap-10 px-6 py-16 lg:grid-cols-2 lg:items-center">
        <div>
          <h1 className="text-4xl font-bold leading-tight lg:text-5xl">
            Your money moves with <span className="text-drip-lime">your vibe</span>.
          </h1>
          <p className="mt-4 text-lg text-drip-muted">
            DripPay is a portfolio-grade digital wallet with SafeTransfer, SpendSense insights,
            SplitSmart groups, and transparent FraudWatch alerts — simulated funds only.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/register" className="btn-primary">Create wallet</Link>
            <Link to="/login" className="btn-ghost">Demo login</Link>
          </div>
        </div>
        <div className="card p-6 shadow-glow">
          <div className="text-sm text-drip-muted">Live preview</div>
          <div className="mt-2 text-3xl font-bold text-drip-lime opacity-80">₹ — — —</div>
          <p className="text-sm text-drip-muted">Decorative preview only — balances load from API after login</p>
          <ul className="mt-6 space-y-2 text-sm text-drip-muted">
            <li>• Idempotent SafeTransfer</li>
            <li>• Double-entry ledger backend</li>
            <li>• INR formatting & budgets</li>
          </ul>
        </div>
      </section>
    </div>
  );
}
