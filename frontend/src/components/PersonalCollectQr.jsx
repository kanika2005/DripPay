import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Download } from 'lucide-react';
import api from '../api/client';

export function collectPayUrls(dripHandle) {
  if (!dripHandle) return { payWebUrl: null, qrImageUrl: null };
  const payWebUrl = `${window.location.origin}/collect/pay?handle=${encodeURIComponent(dripHandle)}`;
  const qrImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=280x280&data=${encodeURIComponent(payWebUrl)}`;
  return { payWebUrl, qrImageUrl };
}

export default function PersonalCollectQr({
  dripHandle: dripHandleProp,
  size = 220,
  showDownload = true,
  showManageLink = false,
  className = '',
}) {
  const [dripHandle, setDripHandle] = useState(dripHandleProp ?? null);
  const [loading, setLoading] = useState(!dripHandleProp);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (dripHandleProp) {
      setDripHandle(dripHandleProp);
      setLoading(false);
      setError(false);
      return;
    }
    setLoading(true);
    api
      .get('/collect/me')
      .then((res) => setDripHandle(res.data.dripHandle))
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [dripHandleProp]);

  const { payWebUrl, qrImageUrl } = useMemo(() => collectPayUrls(dripHandle), [dripHandle]);

  function downloadQr() {
    if (!qrImageUrl || !dripHandle) return;
    const a = document.createElement('a');
    a.href = qrImageUrl;
    a.download = `drippay-${dripHandle.replace('@drippay', '')}-qr.png`;
    a.target = '_blank';
    a.rel = 'noopener';
    a.click();
  }

  if (loading) {
    return <p className="text-sm text-drip-muted">Loading your payment QR…</p>;
  }

  if (error || !dripHandle || !qrImageUrl) {
    return <p className="text-sm text-red-400">Could not load your collect QR. Try again from Collect.</p>;
  }

  return (
    <div className={`flex flex-col items-center gap-3 ${className}`}>
      <img
        src={qrImageUrl}
        alt={`QR code to pay ${dripHandle}`}
        width={size}
        height={size}
        className="rounded-xl border border-drip-border bg-white p-2"
      />
      <p className="font-mono text-base text-drip-primary">{dripHandle}</p>
      <p className="max-w-full break-all text-center text-xs text-drip-muted">{payWebUrl}</p>
      <div className="flex flex-wrap items-center justify-center gap-2">
        {showDownload && (
          <button type="button" className="btn-ghost flex items-center gap-2 !text-sm" onClick={downloadQr}>
            <Download size={16} /> Save QR
          </button>
        )}
        {showManageLink && (
          <Link to="/collect" className="text-sm font-medium text-drip-primary hover:underline">
            Customize handle →
          </Link>
        )}
      </div>
    </div>
  );
}
