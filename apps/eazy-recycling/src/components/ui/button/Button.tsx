import { ButtonHTMLAttributes, FunctionComponent, SVGProps } from 'react';
import clsx from 'clsx';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'tertiary' | 'destructive' | 'icon';
  size?: 'small' | 'medium';
  label?: string;
  icon?: FunctionComponent<SVGProps<SVGSVGElement>>;
  iconPosition?: 'left' | 'right';
  disabled?: boolean;
  showText?: boolean;
  fullWidth?: boolean;
}

const paddingMap: Record<
  'small' | 'medium',
  {
    none: string;
    left: string;
    right: string;
  }
> = {
  small: {
    none: 'px-4 py-2',
    left: 'py-1.5 pl-1 pr-2',
    right: 'py-1.5 pl-2 pr-1',
  },
  medium: {
    none: 'px-4 py-2',
    left: 'py-2 pl-2 pr-4',
    right: 'py-2 pl-4 pr-2',
  },
};

export const Button = ({
  variant = 'primary',
  size = 'medium',
  label,
  icon: Icon,
  iconPosition = 'left',
  showText = true,
  fullWidth = false,
  ...props
}: ButtonProps) => {
  const disabled = props.disabled ?? false;
  const base = 'inline-flex justify-center items-center flex-shrink-0';
  const sizeClasses = {
    small: 'h-8 gap-1 rounded-radius-sm',
    medium: 'h-10 gap-2 rounded-radius-md',
  }[size];
  const variantClasses = {
    primary: clsx(
      disabled
        ? 'bg-color-surface-disabled text-color-text-disabled border cursor-not-allowed'
        : 'bg-color-brand-primary hover:bg-color-brand-dark text-color-text-invert-primary border'
    ),
    secondary: clsx(
      disabled
        ? 'border-color-secondary-disabled text-color-text-disabled border cursor-not-allowed'
        : 'bg-color-surface-primary hover:bg-color-brand-light border border-color-border-primary hover:border-color-brand-primary text-color-brand-primary'
    ),
    tertiary: clsx(
      disabled
        ? 'text-color-text-disabled cursor-not-allowed'
        : 'hover:bg-color-brand-light text-color-brand-primary'
    ),
    destructive: clsx(
      disabled
        ? 'border border-color-border-primary text-color-text-disabled cursor-not-allowed'
        : 'hover:bg-color-status-error-light border border-color-border-primary hover:border-transparent text-color-status-error-dark'
    ),
    icon: clsx(
      disabled
        ? 'text-color-text-disabled cursor-not-allowed'
        : 'hover:bg-color-brand-light text-color-text-secondary'
    ),
  }[variant];
  const width = fullWidth ? 'w-full' : '';
  const iconMode = Icon ? (iconPosition === 'left' ? 'left' : 'right') : 'none';

  const paddingClasses = showText ? paddingMap[size][iconMode] : 'p-1.5';
  return (
    <button
      className={clsx(
        base,
        sizeClasses,
        paddingClasses,
        variantClasses,
        width,
        Icon && iconPosition === 'right' ? 'flex-row-reverse' : ''
      )}
      {...props}
    >
      {Icon && (
        <span className="flex-shrink-0">
          <Icon />
        </span>
      )}
      {showText && <span className="text-subtitle-2">{label}</span>}
    </button>
  );
};
