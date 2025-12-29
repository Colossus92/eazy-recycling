import { useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import CalendarDots from '@/assets/icons/CalendarDots.svg?react';
import BuildingOffice from '@/assets/icons/BuildingOffice.svg?react';
import IdentificationCard from '@/assets/icons/IdentificationCard.svg?react';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';
import BxRecycle from '@/assets/icons/BxRecycle.svg?react';
import Scale from '@/assets/icons/Scale.svg?react';
import IcBaselineEuro from '@/assets/icons/IcBaselineEuro.svg?react';
import { NavItem } from '@/components/layouts/sidebar/NavItem.tsx';
import { SidebarHeader } from '@/components/layouts/sidebar/SidebarHeader.tsx';
import { useAuth } from '@/components/auth/useAuthHook.ts';
import { fallbackRender } from '@/utils/fallbackRender';
import { usePendingApprovals } from '@/features/wastestreams/hooks/usePendingApprovals';

export const Sidebar = () => {
  const [collapsed, setCollapsed] = useState<boolean>(false);
  const { hasRole } = useAuth();
  const { hasPendingApprovals } = usePendingApprovals();

  const toggleSidebar = () => {
    setCollapsed(!collapsed);
  };

  // Define all navigation items
  const allNavItems = [
    { icon: CalendarDots, label: 'Planning', to: '/' },
    { icon: BuildingOffice, label: 'Relaties', to: '/crm' },
    { icon: BxRecycle, label: 'Afvalstroombeheer', to: '/waste-streams' },
    { icon: Scale, label: 'Weegbonnen', to: '/weight-tickets' },
    {
      icon: IcBaselineEuro,
      label: 'Financieel',
      to: '/financials',
      requiredRole: 'admin',
    },
    {
      icon: IdentificationCard,
      label: 'Gebruikersbeheer',
      to: '/users',
      requiredRole: 'admin',
    },
    {
      icon: ArchiveBook,
      label: 'Masterdata',
      to: '/masterdata',
      requiredRole: 'admin',
    },
  ];

  // Filter navigation items based on user role
  const navItems = allNavItems.filter(
    (item) => !item.requiredRole || hasRole(item.requiredRole)
  );

  return (
    <div
      className="h-full flex flex-col items-start shrink-0 border border-solid border-primary rounded-radius-lg group transition-all duration-300"
      data-testid="sidebar"
    >
      <ErrorBoundary fallbackRender={fallbackRender}>
        <SidebarHeader collapsed={collapsed} toggleSidebar={toggleSidebar} />
        <div
          className="flex flex-col items-start gap-2 self-stretch p-4"
          data-testid="sidebar-nav"
        >
          {navItems.map((item, key) => (
            <NavItem
              to={item.to}
              icon={item.icon}
              label={item.label}
              collapsed={collapsed}
              hasNotification={
                item.to === '/waste-streams' && hasPendingApprovals
              }
              key={key}
            />
          ))}
        </div>
      </ErrorBoundary>
    </div>
  );
};
