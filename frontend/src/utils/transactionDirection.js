/** @returns {true | false | null} true = credit (in), false = debit (out) */
export function isTransactionCredit(tx, myWalletId) {
  if (!tx || !myWalletId) return null;

  const mine = myWalletId.toUpperCase();
  const sender = tx.senderWalletId?.toUpperCase();
  const recipient = tx.recipientWalletId?.toUpperCase();

  const iReceive = recipient === mine;
  const iSend = sender === mine;

  if (iReceive && !iSend) return true;
  if (iSend && !iReceive) return false;

  if (tx.type === 'DEMO_DEPOSIT') return true;
  if (tx.type === 'DEMO_WITHDRAWAL') return false;

  return null;
}
