import clsx from 'clsx';
import CheckCircleOutline from '@/assets/icons/CheckCircleOutline.svg?react';

export type SignatureStatus = 'SIGNED' | 'NOT SIGNED' | 'LOADING';

export const SignatureStatusTag = ({ status }: { status: SignatureStatus }) => {
  const baseClasses =
    'flex justify-content items-center rounded-radius-xs py-1 px-2 gap-1 ';
  const statusClasses = {
    SIGNED: 'bg-color-status-success-light text-color-status-success-primary',
    'NOT SIGNED': 'bg-[#EAEDF2] text-color-text-secondary',
    LOADING: 'bg-[#EAEDF2] text-color-text-secondary animate-pulse',
  }[status];

  const text = {
    SIGNED: 'Getekend',
    'NOT SIGNED': 'Niet getekend',
    LOADING: 'Laden...',
  }[status];
  return (
    <div className={clsx(baseClasses, statusClasses)}>
      {status === 'SIGNED' && (
        <CheckCircleOutline className="size-5 text-color-status-success-primary" />
      )}
      <span className="text-caption-1">{text}</span>
    </div>
  );
};
