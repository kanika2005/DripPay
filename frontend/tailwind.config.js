/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        drip: {
          bg: '#101114',
          lime: '#DFFF68',
          purple: '#8D7BFF',
          muted: '#9CA3AF',
          card: '#1A1B1F',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        glow: '0 0 40px rgba(223, 255, 104, 0.15)',
      },
    },
  },
  plugins: [],
};
