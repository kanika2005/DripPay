import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { QrCode } from 'lucide-react';
import api from '../api/client';
import PersonalCollectQr from '../components/PersonalCollectQr';
import { useToast } from '../context/ToastContext';

export default function Collect() {
  const { show } = useToast();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [handleInput, setHandleInput] = useState('');
  const [payHandle, setPayHandle] = useState('');
  const [lookup, setLookup] = useState(null);
  useEffect(() => {
    api
      .get('/collect/me')
      .then((res) => {
        setProfile(res.data);
        setHandleInput(res.data.dripHandle.replace('@drippay', ''));
      })
      .catch(() => show('Could not load your collect profile'));
  }, [show]);

  async function saveHandle() {
    const h = handleInput.includes('@') ? handleInput : `${handleInput}@drippay`;
    const { data } = await api.put('/collect/me/handle', { handle: h });
    setProfile(data);
    show('Collect handle updated');
  }

  async function resolvePayee() {
    setLookup(null);
    try {
      const { data } = await api.get(`/collect/resolve/${encodeURIComponent(payHandle)}`);
      setLookup(data);
    } catch (err) {
      show(err.response?.data?.message || 'Handle not found');
    }
  }

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div className="flex items-center gap-2">
        <QrCode className="text-drip-primary" size={24} aria-hidden="true" />
        <h1 className="text-2xl font-bold tracking-tight">Collect</h1>
      </div>
      <p className="text-sm text-drip-muted">
        Every user gets a personal QR and <span className="font-mono text-drip-foreground">name@drippay</span> handle.
        Others scan or tap to pay you via Scan &amp; Pay.
      </p>

      <div className="card space-y-4 p-6">
        <h2 className="font-semibold">Your payment QR</h2>
        <PersonalCollectQr dripHandle={profile?.dripHandle} size={260} showManageLink={false} />
        <Link to="/send" className="btn-primary w-full text-center">
          Scan someone else&apos;s QR
        </Link>
      </div>

      <div className="card space-y-4 p-6">
        <h2 className="font-semibold">Customize handle</h2>
        <div className="flex gap-2">
          <input
            className="input flex-1 font-mono"
            value={handleInput}
            onChange={(e) => setHandleInput(e.target.value)}
            aria-label="Drip handle"
          />
          <span className="self-center text-sm text-drip-muted">@drippay</span>
        </div>
        <button type="button" className="btn-primary" onClick={saveHandle}>Save handle</button>
        <p className="text-xs text-drip-muted">Changing your handle updates your QR automatically.</p>
      </div>

      <div className="card space-y-3 p-6">
        <h2 className="font-semibold">Pay a handle</h2>
        <input
          className="input font-mono"
          placeholder="friend@drippay"
          value={payHandle}
          onChange={(e) => setPayHandle(e.target.value)}
        />
        <button type="button" className="btn-ghost" onClick={resolvePayee}>Look up</button>
        {lookup && (
          <div className="space-y-3 rounded-xl border border-drip-border p-3 text-sm">
            <p>
              {lookup.displayName} · <span className="font-mono">{lookup.dripHandle}</span>
            </p>
            <button
              type="button"
              className="btn-primary w-full"
              onClick={() => navigate(`/send?handle=${encodeURIComponent(lookup.dripHandle)}`)}
            >
              Pay {lookup.displayName}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
