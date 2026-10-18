import { useEffect, useState } from 'react';
import { QrCode } from 'lucide-react';
import api from '../api/client';
import { useToast } from '../context/ToastContext';

export default function Collect() {
  const { show } = useToast();
  const [profile, setProfile] = useState(null);
  const [handleInput, setHandleInput] = useState('');
  const [payHandle, setPayHandle] = useState('');
  const [lookup, setLookup] = useState(null);

  useEffect(() => {
    api.get('/collect/me').then((res) => {
      setProfile(res.data);
      setHandleInput(res.data.dripHandle.replace('@drippay', ''));
    });
  }, []);

  async function saveHandle() {
    const h = handleInput.includes('@') ? handleInput : `${handleInput}@drippay`;
    const { data } = await api.put('/collect/me/handle', { handle: h });
    setProfile(data);
    show('Collect handle updated');
  }

  async function resolvePayee() {
    const { data } = await api.get(`/collect/resolve/${encodeURIComponent(payHandle)}`);
    setLookup(data);
  }

  const qrUrl = profile
    ? `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(profile.qrPayload)}`
    : null;

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div className="flex items-center gap-2">
        <QrCode className="text-drip-primary" size={24} aria-hidden="true" />
        <h1 className="text-2xl font-bold tracking-tight">Collect</h1>
      </div>
      <p className="text-sm text-drip-muted">
        Share your <span className="font-mono text-drip-foreground">name@drippay</span> handle or QR so others can pay you.
      </p>

      <div className="card space-y-4 p-6">
        <h2 className="font-semibold">Your handle</h2>
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
        {profile && (
          <p className="text-sm text-drip-muted">
            Active: <span className="font-mono text-drip-primary">{profile.dripHandle}</span>
          </p>
        )}
        {qrUrl && (
          <div className="flex flex-col items-center gap-2 pt-2">
            <img src={qrUrl} alt="QR code for your DripPay collect link" className="rounded-xl border border-drip-border bg-white p-2" />
            <p className="max-w-full break-all text-center font-mono text-xs text-drip-muted">{profile.qrPayload}</p>
          </div>
        )}
      </div>

      <div className="card space-y-3 p-6">
        <h2 className="font-semibold">Look up a handle</h2>
        <input
          className="input font-mono"
          placeholder="friend@drippay"
          value={payHandle}
          onChange={(e) => setPayHandle(e.target.value)}
        />
        <button type="button" className="btn-ghost" onClick={resolvePayee}>Resolve</button>
        {lookup && (
          <p className="text-sm">
            {lookup.displayName} · wallet <span className="font-mono">{lookup.walletId}</span>
          </p>
        )}
      </div>
    </div>
  );
}
