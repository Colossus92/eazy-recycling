import LogoIcon from '@/assets/MediumLogo.svg';
import ArrowLineRight from '@/assets/icons/ArrowLineRight.svg';
import ArrowLineLeft from '@/assets/icons/ArrowLineLeft.svg';
import { Logo } from '@/components/ui/Logo';

interface SidebarHeaderProps {
  collapsed: boolean;
  toggleSidebar: () => void;
}

export const SidebarHeader = ({
  collapsed,
  toggleSidebar,
}: SidebarHeaderProps) => {
  return (
    <div className="relative flex p-4 justify-between items-center self-stretch border-b border-solid border-b-primary">
      <div className="flex items-center justify-center w-full gap-2 relative">
        {collapsed && (
          <>
            {/* Logo icon (shown when not hovering) */}
            <img
              src={LogoIcon}
              alt="Logo"
              className="w-8 h-8 opacity-100 group-hover:opacity-0 transition-opacity duration-200"
            />
            {/* Arrow icon (shown on hover when collapsed) */}
            <button
              onClick={toggleSidebar}
              className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200"
            >
              <img
                src={ArrowLineRight}
                alt="Expand sidebar"
                className="w-4 h-4"
              />
            </button>
          </>
        )}

        {/* Full logo and text (when not collapsed) */}
        {!collapsed && (
          <>
            <Logo />
            <button onClick={toggleSidebar}>
              <img src={ArrowLineLeft} alt="Collapse" className="w-4 h-4" />
            </button>
          </>
        )}
      </div>
    </div>
  );
};
