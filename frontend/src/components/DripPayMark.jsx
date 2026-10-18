/**
 * Brand mark: phone + card + verified check (flat line style).
 */
export default function DripPayMark({ size = 36, className = '' }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {/* Phone */}
      <rect
        x="22"
        y="6"
        width="48"
        height="88"
        rx="7"
        stroke="currentColor"
        strokeWidth="3.5"
      />
      <line x1="36" y1="13" x2="56" y2="13" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
      <line x1="38" y1="86" x2="54" y2="86" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />

      {/* Payment card */}
      <rect
        x="44"
        y="38"
        width="52"
        height="34"
        rx="4"
        stroke="currentColor"
        strokeWidth="3.5"
        fill="var(--drip-surface, #121b2e)"
      />
      <rect x="44" y="38" width="52" height="11" fill="currentColor" />
      <path
        d="M78 58 L84 52 L90 58 L84 64 Z"
        fill="currentColor"
        opacity="0.85"
      />
      <path d="M72 64 L78 58 L84 64 L78 70 Z" fill="currentColor" opacity="0.55" />

      {/* Success badge */}
      <circle cx="30" cy="70" r="15" fill="#5CB85C" />
      <path
        d="M23 70 L28 75 L38 63"
        stroke="white"
        strokeWidth="3.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* Sparkle / notification */}
      <path d="M66 10 L66 20" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
      <path d="M71 8 L76 13" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
      <path d="M76 15 L86 15" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}
