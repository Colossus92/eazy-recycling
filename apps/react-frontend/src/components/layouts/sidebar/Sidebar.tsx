import { useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import CalendarDots from '@/assets/icons/CalendarDots.svg?react';
import ShippingContainer from '@/assets/icons/ShippingContainer.svg?react';
import BuildingOffice from '@/assets/icons/BuildingOffice.svg?react';
import TruckTrailer from '@/assets/icons/TruckTrailer.svg?react';
import IdentificationCard from '@/assets/icons/IdentificationCard.svg?react';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';
import BxRecycle from '@/assets/icons/BxRecycle.svg?react';
import { NavItem } from '@/components/layouts/sidebar/NavItem.tsx';
import { SidebarHeader } from '@/components/layouts/sidebar/SidebarHeader.tsx';
import { useAuth } from '@/components/auth/useAuthHook.ts';
import { fallbackRender } from '@/utils/fallbackRender';

export const Sidebar = () => {
  const [collapsed, setCollapsed] = useState<boolean>(false);
  const { hasRole } = useAuth();

  const toggleSidebar = () => {
    setCollapsed(!collapsed);
  };

  // Define all navigation items
  const allNavItems = [
    { icon: CalendarDots, label: 'Planning', to: '/' },
    { icon: ShippingContainer, label: 'Containerbeheer', to: '/containers' },
    { icon: BuildingOffice, label: 'CRM', to: '/crm' },
    { icon: TruckTrailer, label: 'Vrachtwagenbeheer', to: '/trucks' },
    { icon: BxRecycle, label: 'Afvalstroombeheer', to: '/waste-streams' },
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
        <div className="flex flex-col items-start gap-2 self-stretch p-4" data-testid="sidebar-nav">
          {navItems.map((item, key) => (
            <NavItem
              to={item.to}
              icon={item.icon}
              label={item.label}
              collapsed={collapsed}
              key={key}
            />
          ))}
        </div>
      </ErrorBoundary>
    </div>
  );
};
