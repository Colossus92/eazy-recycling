import clsx from 'clsx';
import { ReactNode } from 'react';

interface PaginationButtonProps {
  value: number | ReactNode;
  isActive?: boolean;
  isDisabled?: boolean;
  onClick: () => void;
}

export const PaginationButton = ({
  value,
  isActive = false,
  isDisabled = false,
  onClick,
}: PaginationButtonProps) => {
  const baseClasses =
    'flex w-8 p-1.5 flex-col justify-center items-center gap-2 border border-solid rounded-radius-xs button-font';
  const hoverClasses =
    'hover:bg-color-brand-light hover:border-color-hover hover:text-color-text-primary';
  const formattingClasses = isActive
    ? 'bg-color-brand-primary border-color-brand-primary text-color-text-invert-primary'
    : 'border-color-border-primary text-color-text-secondary';
  const disabledClasses = 'cursor-not-allowed text-color-text-disabled';

  return (
    <button
      className={clsx(
        baseClasses,
        !isDisabled ? hoverClasses : '',
        formattingClasses,
        isDisabled ? disabledClasses : ''
      )}
      onClick={isDisabled ? undefined : onClick}
    >
      {value}
    </button>
  );
};
