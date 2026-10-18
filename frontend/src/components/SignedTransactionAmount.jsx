import { formatINR } from '../utils/format';
import { isTransactionCredit } from '../utils/transactionDirection';

/**
 * Money in: green +. Money out: red −. Icons/signs for accessibility, not colour alone.
 */
export default function SignedTransactionAmount({ tx, myWalletId, className = '' }) {
  const credit = isTransactionCredit(tx, myWalletId);
  const sign = credit === true ? '+' : credit === false ? '−' : '';
  const color =
    credit === true ? 'text-drip-credit' : credit === false ? 'text-drip-debit' : 'text-drip-muted';

  return (
    <span className={`font-amount font-medium ${color} ${className}`.trim()} aria-label={credit ? 'Money in' : credit === false ? 'Money out' : 'Amount'}>
      <span aria-hidden="true">{sign}</span>
      {formatINR(tx.amount)}
    </span>
  );
}
