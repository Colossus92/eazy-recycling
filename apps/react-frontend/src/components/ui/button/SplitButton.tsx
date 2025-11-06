import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/react';
import clsx from 'clsx';
import CaretDown from '@/assets/icons/CaretDown.svg?react';

interface SplitButtonProps {
  primaryLabel: string;
  secondaryLabel: string;
  onPrimaryClick: () => void;
  onSecondaryClick: () => void;
  disabled?: boolean;
  isSubmitting?: boolean;
  fullWidth?: boolean;
  size?: 'small' | 'medium';
}

export const SplitButton = ({
  primaryLabel,
  secondaryLabel,
  onPrimaryClick,
  onSecondaryClick,
  disabled = false,
  isSubmitting = false,
  fullWidth = false,
  size = 'medium',
}: SplitButtonProps) => {
  const isDisabled = disabled || isSubmitting;

  const base = 'inline-flex items-center flex-shrink-0';
  const sizeClasses = {
    small: 'h-8 rounded-radius-sm',
    medium: 'h-10 rounded-radius-md',
  }[size];
  const width = fullWidth ? 'w-full' : '';

  // Primary button classes (follows primary variant from Button.tsx)
  const primaryClasses = clsx(
    'flex-1 flex items-center justify-center h-full gap-2',
    size === 'small' ? 'px-4 py-2' : 'px-4 py-2',
    isDisabled
      ? 'bg-color-surface-disabled text-color-text-disabled cursor-not-allowed'
      : 'bg-color-brand-primary hover:bg-color-brand-dark text-color-text-invert-primary'
  );

  // Dropdown trigger classes (same background as primary but separated with border)
  const dropdownTriggerClasses = clsx(
    'flex items-center justify-center h-full',
    size === 'small' ? 'px-2' : 'px-2',
    'border-l border-white/20',
    isDisabled
      ? 'bg-color-surface-disabled text-color-text-disabled cursor-not-allowed'
      : 'bg-color-brand-primary hover:bg-color-brand-dark text-color-text-invert-primary'
  );

  return (
    <div className={clsx(base, sizeClasses, width, 'border rounded-radius-md overflow-hidden')}>
      {/* Primary action button */}
      <button
        onClick={onPrimaryClick}
        disabled={isDisabled}
        className={primaryClasses}
        data-testid="split-button-primary"
      >
        <span className="text-subtitle-2">{primaryLabel}</span>
      </button>

      {/* Dropdown menu */}
      <Menu>
        <MenuButton
          className={dropdownTriggerClasses}
          disabled={isDisabled}
          data-testid="split-button-dropdown"
        >
          <CaretDown className="w-4 h-4" />
        </MenuButton>

        <MenuItems
          anchor="bottom end"
          className="flex flex-col items-start min-w-48 focus:outline-none mt-2 bg-color-surface-primary border border-color-border-primary rounded-radius-md shadow-lg z-20"
        >
          <MenuItem>
            <button
              onClick={onSecondaryClick}
              disabled={isDisabled}
              className="w-full inline-flex justify-center items-center flex-shrink-0 px-4 py-2 text-subtitle-2 text-color-brand-primary bg-color-surface-primary border-color-border-primary hover:border-color-brand-primary hover:bg-color-brand-light disabled:text-color-text-disabled disabled:cursor-not-allowed"
              data-testid="split-button-secondary"
            >
              {secondaryLabel}
            </button>
          </MenuItem>
        </MenuItems>
      </Menu>
    </div>
  );
};
