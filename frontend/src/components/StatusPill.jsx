import { getStatusMeta } from '../utils/transactionStatus';

export default function StatusPill({ status, className = '' }) {
  const { label, pill, icon } = getStatusMeta(status);
  return (
    <span className={`status-pill ${pill} ${className}`.trim()} title={label}>
      <span aria-hidden="true">{icon}</span>
      <span>{label}</span>
    </span>
  );
}
