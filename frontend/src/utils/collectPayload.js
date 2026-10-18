/** Parse QR text or pasted payload into a drip handle (with @drippay). */
export function parseCollectPayload(text) {
  if (!text || typeof text !== 'string') return null;
  const trimmed = text.trim();

  const schemeMatch = trimmed.match(/^drippay:\/\/collect\?handle=([^&\s]+)/i);
  if (schemeMatch) {
    return decodeURIComponent(schemeMatch[1]).toLowerCase();
  }

  try {
    const url = new URL(trimmed);
    if (url.protocol === 'drippay:' && url.host === 'collect') {
      const handle = url.searchParams.get('handle');
      if (handle) return handle.toLowerCase();
    }
    if (url.pathname.endsWith('/collect/pay')) {
      const handle = url.searchParams.get('handle') || url.searchParams.get('h');
      if (handle) return handle.toLowerCase();
    }
  } catch {
    // not a URL
  }

  const lower = trimmed.toLowerCase();
  if (lower.includes('@drippay')) {
    return lower.endsWith('@drippay') ? lower : `${lower.replace(/@drippay.*$/, '')}@drippay`;
  }

  if (/^drip[a-z0-9]+$/i.test(trimmed)) {
    return null;
  }

  return null;
}

export function looksLikeWalletId(value) {
  return /^DRIP[A-Z0-9]+$/i.test((value || '').trim());
}

export function looksLikeHandle(value) {
  const v = (value || '').trim().toLowerCase();
  return v.includes('@drippay') || (v.length >= 2 && !looksLikeWalletId(v) && /^[a-z0-9._-]+$/i.test(v.replace(/@drippay$/, '')));
}
