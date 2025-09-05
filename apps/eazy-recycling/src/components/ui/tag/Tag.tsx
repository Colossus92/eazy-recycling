import clsx from 'clsx';

interface TagProps {
  status: 'INVOICED' | 'FINISHED' | 'UNPLANNED' | 'ERROR' | 'PLANNED';
}

export const Tag = ({ status }: TagProps) => {
  const baseClasses =
    'inline-flex py-1 px-2 justify-center items-center gap-1 rounded-radius-xs text-subtitle-2 font-urbanist';
  const statusClasses = {
    INVOICED: 'bg-[#EAEDF2] text-color-text-secondary',
    FINISHED: 'bg-color-status-success-light text-color-status-success-primary',
    UNPLANNED:
      'bg-color-status-warning-light text-color-status-warning-primary',
    ERROR: 'bg-color-status-error-light text-color-status-error-dark',
    PLANNED: 'bg-color-brand-light-hover text-color-status-info-dark',
  }[status];

  const text = {
    INVOICED: 'Gefactureerd',
    FINISHED: 'Afgerond',
    UNPLANNED: 'Ongepland',
    ERROR: 'Foutmelding',
    PLANNED: 'Gepland',
  }[status];
  return <div className={clsx(baseClasses, statusClasses)}>{text}</div>;
};
