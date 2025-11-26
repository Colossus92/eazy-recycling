type SyncStatus = 'OK' | 'FAILED' | 'CONFLICT' | 'PENDING_REVIEW';

interface SyncStatusTagProps {
  status: string;
}

const statusConfig: Record<
  SyncStatus,
  { label: string; bgColor: string; textColor: string }
> = {
  OK: {
    label: 'Gesynchroniseerd',
    bgColor: 'bg-color-success-light',
    textColor: 'text-color-success',
  },
  FAILED: {
    label: 'Mislukt',
    bgColor: 'bg-color-error-light',
    textColor: 'text-color-error',
  },
  CONFLICT: {
    label: 'Conflict',
    bgColor: 'bg-color-error-light',
    textColor: 'text-color-error',
  },
  PENDING_REVIEW: {
    label: 'Handmatige review',
    bgColor: 'bg-color-warning-light',
    textColor: 'text-color-warning',
  },
};

export const SyncStatusTag = ({ status }: SyncStatusTagProps) => {
  const config = statusConfig[status as SyncStatus] || statusConfig.FAILED;

  return (
    <span
      className={`inline-flex items-center px-2 py-1 rounded-radius-sm text-caption ${config.bgColor} ${config.textColor}`}
    >
      {config.label}
    </span>
  );
};
