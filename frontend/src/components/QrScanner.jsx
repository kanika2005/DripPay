import { useEffect, useId, useRef, useState } from 'react';
import { Html5Qrcode } from 'html5-qrcode';

export default function QrScanner({ onScan, onError, active = true }) {
  const regionId = useId().replace(/:/g, '');
  const scannerRef = useRef(null);
  const onScanRef = useRef(onScan);
  const onErrorRef = useRef(onError);
  const [starting, setStarting] = useState(false);
  const [cameraError, setCameraError] = useState('');

  onScanRef.current = onScan;
  onErrorRef.current = onError;

  useEffect(() => {
    if (!active) return undefined;

    let scanner;
    let cancelled = false;

    async function start() {
      setStarting(true);
      setCameraError('');
      try {
        scanner = new Html5Qrcode(regionId);
        scannerRef.current = scanner;
        await scanner.start(
          { facingMode: 'environment' },
          { fps: 8, qrbox: { width: 240, height: 240 } },
          (decoded) => {
            onScanRef.current?.(decoded);
          },
          () => {},
        );
      } catch (err) {
        if (!cancelled) {
          const msg = err?.message || 'Camera access failed';
          setCameraError(msg);
          onErrorRef.current?.(msg);
        }
      } finally {
        if (!cancelled) setStarting(false);
      }
    }

    start();

    return () => {
      cancelled = true;
      const s = scannerRef.current;
      scannerRef.current = null;
      if (s) {
        s.stop().catch(() => {}).finally(() => {
          try {
            s.clear();
          } catch {
            /* ignore */
          }
        });
      }
    };
  }, [active, regionId]);

  return (
    <div className="space-y-2">
      <div
        id={regionId}
        className="overflow-hidden rounded-xl border border-drip-border bg-black/40 [&_video]:rounded-xl"
        aria-label="QR code scanner"
      />
      {starting && <p className="text-center text-xs text-drip-muted">Starting camera…</p>}
      {cameraError && (
        <p className="text-center text-xs text-amber-400">
          {cameraError}. Allow camera permission or paste the handle below.
        </p>
      )}
    </div>
  );
}
