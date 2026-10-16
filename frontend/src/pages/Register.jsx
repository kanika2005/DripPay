import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Logo from '../components/Logo';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export default function Register() {
  const { register } = useAuth();
  const { show } = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await register(form);
      show('Wallet created — add demo funds to get started');
      navigate('/dashboard');
    } catch (err) {
      const fields = err.response?.data?.fields;
      if (!err.response) {
        setError('Cannot reach API. Is the backend running? Check VITE_DEV_API_PROXY in frontend/.env (default port 18080).');
      } else {
        setError(fields ? Object.values(fields).join(', ') : err.response?.data?.message || 'Registration failed');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-drip-bg px-4">
      <div className="card w-full max-w-md p-8">
        <Logo />
        <form className="mt-8 space-y-4" onSubmit={onSubmit}>
          {['fullName', 'email', 'password'].map((key) => (
            <div key={key}>
              <label className="text-sm capitalize text-drip-muted">{key.replace('fullName', 'Full name')}</label>
              <input
                className="input mt-1"
                type={key === 'password' ? 'password' : key === 'email' ? 'email' : 'text'}
                value={form[key]}
                onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                required
                minLength={key === 'password' ? 8 : undefined}
              />
            </div>
          ))}
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? 'Creating…' : 'Create DripPay account'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-drip-muted">
          Already have an account? <Link className="text-drip-lime" to="/login">Log in</Link>
        </p>
      </div>
    </div>
  );
}
