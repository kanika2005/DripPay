import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import Logo from '../components/Logo';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export default function Login() {
  const { login } = useAuth();
  const { show } = useToast();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [email, setEmail] = useState('demo@drippay.app');
  const [password, setPassword] = useState('Demo1234!');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function onSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await login(email, password);
      show('Welcome back to DripPay');
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-drip-bg px-4">
      <div className="card w-full max-w-md p-8">
        <Logo />
        {params.get('expired') && (
          <p className="mt-4 rounded-lg bg-amber-500/10 px-3 py-2 text-sm text-amber-200">Session expired. Please log in again.</p>
        )}
        <form className="mt-8 space-y-4" onSubmit={onSubmit}>
          <div>
            <label className="text-sm text-drip-muted">Email</label>
            <input className="input mt-1" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div>
            <label className="text-sm text-drip-muted">Password</label>
            <input className="input mt-1" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? 'Signing in…' : 'Log in'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-drip-muted">
          New here? <Link className="text-drip-lime" to="/register">Create account</Link>
        </p>
      </div>
    </div>
  );
}
