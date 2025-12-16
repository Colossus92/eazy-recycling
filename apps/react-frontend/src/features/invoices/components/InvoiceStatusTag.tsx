interface InvoiceStatusTagProps {
  status: string;
}

const statusConfig: Record<string, { label: string; className: string }> = {
  DRAFT: {
    label: 'Concept',
    className: 'bg-color-status-warning-light text-color-status-warning-dark',
  },
  FINAL: {
    label: 'Definitief',
    className: 'bg-color-status-success-light text-color-status-success-dark',
  },
};

export const InvoiceStatusTag = ({ status }: InvoiceStatusTagProps) => {
  const config = statusConfig[status] || {
    label: status,
    className: 'bg-color-surface-tertiary text-color-text-secondary',
  };

  return (
    <span
      className={`inline-flex items-center px-2 py-1 rounded-radius-sm text-caption-1 font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
};
