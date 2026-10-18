# DripPay UI theme

**Concept:** “Clear as water, exact as a ledger.” Dark-first, trust-focused fintech UI.

## Palette (CSS variables)

| Role | Dark | Token |
|------|------|--------|
| Background | `#0B1220` | `--drip-bg` |
| Surface | `#121B2E` | `--drip-surface` |
| Primary (teal) | `#14B8A6` | `--drip-primary` |
| Accent (aqua) | `#38BDF8` | `--drip-accent` |
| Credit / success | `#22C55E` | `--drip-credit` |
| Debit / error | `#EF4444` | `--drip-debit` |
| Warning | `#F59E0B` | `--drip-warning` |
| Reversed | `#A78BFA` | `--drip-reversed` |

Light mode: toggle in app header (`ThemeProvider`, class on `<html>`).

## Status colours

Use `StatusPill` + `transactionStatus.js` everywhere: SUCCESS (green), PENDING (amber), FAILED (red), REVERSED (purple). Money in/out uses `SignedTransactionAmount` (+/− and labels).

## Typography

- UI: Plus Jakarta Sans (Inter fallback)
- Amounts & references: JetBrains Mono (`font-amount` / `font-mono`)
- Money: `formatINR()` → ₹1,234.50

## Stack

React + Vite + Tailwind (`tailwind.config.js` maps `drip-*` to variables). Recharts for SpendSense (teal, aqua, violet only).

## Trust UI

- “Verified by ledger” on settled transaction detail
- Transaction timeline component
- “Read-only insights” on Assistant
- Simulated money footer in app shell
