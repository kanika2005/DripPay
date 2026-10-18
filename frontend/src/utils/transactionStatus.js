/** Status colours aligned with backend state machine (icon + label, not colour alone). */

const STATUS_META = {
  SUCCESS: { label: 'Success', pill: 'status-success', icon: '✓' },
  COMPLETED: { label: 'Success', pill: 'status-success', icon: '✓' },
  PENDING: { label: 'Pending', pill: 'status-warning', icon: '◷' },
  INITIATED: { label: 'Initiated', pill: 'status-muted', icon: '○' },
  FAILED: { label: 'Failed', pill: 'status-error', icon: '✕' },
  REVERSED: { label: 'Reversed', pill: 'status-reversed', icon: '↩' },
};

export function getStatusMeta(status) {
  return STATUS_META[status] || { label: status, pill: 'status-muted', icon: '•' };
}

/** Timeline steps for display (current status highlighted). */
export const TX_TIMELINE = ['INITIATED', 'PENDING', 'SUCCESS'];

export function timelineIndex(status) {
  if (status === 'COMPLETED') return 2;
  if (status === 'REVERSED' || status === 'FAILED') return -1;
  const i = TX_TIMELINE.indexOf(status);
  return i >= 0 ? i : 0;
}

export function isSettledStatus(status) {
  return status === 'SUCCESS' || status === 'COMPLETED';
}
