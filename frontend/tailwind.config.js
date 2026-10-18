/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  darkMode: ['class', '.dark'],
  theme: {
    extend: {
      colors: {
        drip: {
          bg: 'var(--drip-bg)',
          surface: 'var(--drip-surface)',
          primary: 'var(--drip-primary)',
          accent: 'var(--drip-accent)',
          credit: 'var(--drip-credit)',
          debit: 'var(--drip-debit)',
          warning: 'var(--drip-warning)',
          reversed: 'var(--drip-reversed)',
          // "foreground" not "text" — Tailwind cannot @apply text-drip-text
          foreground: 'var(--drip-text)',
          muted: 'var(--drip-muted)',
          border: 'var(--drip-border)',
          // legacy aliases (lime → teal primary)
          lime: 'var(--drip-primary)',
          purple: 'var(--drip-reversed)',
          card: 'var(--drip-surface)',
        },
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'Inter', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'monospace'],
      },
      borderRadius: {
        card: '14px',
      },
      boxShadow: {
        glow: '0 0 48px color-mix(in srgb, var(--drip-primary) 18%, transparent)',
        card: '0 1px 0 color-mix(in srgb, var(--drip-border) 80%, transparent)',
      },
      backgroundImage: {
        'balance-gradient':
          'linear-gradient(135deg, color-mix(in srgb, var(--drip-primary) 22%, var(--drip-surface)) 0%, var(--drip-surface) 55%, color-mix(in srgb, var(--drip-accent) 8%, var(--drip-surface)) 100%)',
      },
    },
  },
  plugins: [],
};
