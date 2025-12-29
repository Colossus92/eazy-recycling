import { FunctionComponent, SVGProps } from 'react';
import { NavLink, To } from 'react-router-dom';
import { NotificationDot } from '@/components/ui/NotificationDot';

interface NavItemProps {
  icon: FunctionComponent<SVGProps<SVGSVGElement>>;
  label: string;
  collapsed: boolean;
  to: To;
  hasNotification?: boolean;
}

export const NavItem = ({
  icon: Icon,
  label,
  collapsed,
  to,
  hasNotification = false,
}: NavItemProps) => {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `relative group flex items-center self-stretch gap-2 h-10 py-2 px-3 rounded-radius-md
        ${
          isActive
            ? 'text-color-text-invert-primary bg-color-brand-primary'
            : 'text-color-text-secondary hover:text-color-brand-dark hover:bg-color-brand-light-hover'
        }`
      }
      data-testid={`nav-item-${label.toLowerCase()}`}
    >
      <Icon className="h-10 w-5" />
      {!collapsed && <span className="text-button">{label}</span>}
      <NotificationDot
        show={hasNotification}
        className="top-0 right-0 -mt-[0.125rem] -mr-[0.125rem]"
      />
    </NavLink>
  );
};
