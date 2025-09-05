import {
  forwardRef,
  ButtonHTMLAttributes,
  FunctionComponent,
  SVGProps,
} from 'react';

interface NavItemProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon?: FunctionComponent<SVGProps<SVGSVGElement>>;
  label: string;
  textColor?: string;
}

export const MenuItemButton = forwardRef<HTMLButtonElement, NavItemProps>(
  ({ icon: Icon, label, textColor, ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={`group flex items-center self-stretch gap-2 h-10 py-2 px-3 rounded-radius-md focus:outline-none ${textColor ? textColor : 'text-color-text-secondary'} hover:text-color-brand-dark hover:bg-color-brand-light-hover`}
        {...props}
      >
        {Icon && <Icon className="h-10 w-5" />}
        <span className="text-button">{label}</span>
      </button>
    );
  }
);
