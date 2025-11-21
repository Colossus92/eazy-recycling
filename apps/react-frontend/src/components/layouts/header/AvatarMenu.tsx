import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/react';
import Avatar from 'react-avatar';
import { useNavigate } from 'react-router-dom';
import { MenuItemButton } from '@/components/layouts/MenuItemButton.tsx';
import { useAuth } from '@/components/auth/useAuthHook.ts';
import IcBaselineLogout from '@/assets/icons/IcBaselineLogout.svg?react';
import IcBaselinePersonOutline from '@/assets/icons/IcBaselinePersonOutline.svg?react';
import IconoirSettings from '@/assets/icons/IconoirSettings.svg?react';

export const AvatarMenu = ({
  hideProfileItem = false,
}: {
  hideProfileItem?: boolean;
}) => {
  const { user, signOut, hasRole } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="relative">
      <Menu>
        <MenuButton className="hover:bg-color-surface-secondary rounded focus:outline-none">
          <Avatar
            name={`${user?.firstName} ${user?.lastName}`}
            size="40px"
            round={true}
          />
        </MenuButton>
        <MenuItems
          anchor="bottom end"
          className="flex flex-col items-start w-60 mt-2 bg-color-surface-primary border border-color-border-primary rounded-radius-md shadow-lg z-20 focus:outline-none"
        >
          {!hideProfileItem && (
            <MenuItem as="div" className="w-full focus:outline-none">
              <div
                className={
                  'flex flex-col items-start self-stretch p-2 border-b border-solid border-color-border-primary'
                }
              >
                <MenuItemButton
                  icon={IcBaselinePersonOutline}
                  label={'Profiel'}
                  onClick={() => navigate('/profile')}
                />
              </div>
            </MenuItem>
          )}
          {hasRole('admin') && (
            <MenuItem as="div" className="w-full focus:outline-none">
              <div
                className={
                  'flex flex-col items-start self-stretch p-2 border-b border-solid border-color-border-primary'
                }
              >
                <MenuItemButton
                  icon={IconoirSettings}
                  label={'Instellingen'}
                  onClick={() => navigate('/settings')}
                />
              </div>
            </MenuItem>
          )}
          <MenuItem as="div" className="w-full focus:outline-none">
            <div className={'flex flex-col items-start self-stretch p-2'}>
              <MenuItemButton
                icon={IcBaselineLogout}
                label={'Uitloggen'}
                onClick={signOut}
              />
            </div>
          </MenuItem>
        </MenuItems>
      </Menu>
    </div>
  );
};
