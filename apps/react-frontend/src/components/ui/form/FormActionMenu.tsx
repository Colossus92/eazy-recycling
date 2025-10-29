import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/react';
import { SVGProps } from 'react';
import DotsThreeVertical from '@/assets/icons/DotsThreeVertical.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';
import { MenuItemButton } from '@/components/layouts/MenuItemButton.tsx';

export interface FormAction {
  label: string;
  icon: React.FunctionComponent<SVGProps<SVGSVGElement>>;
  onClick: () => void;
  textColor?: string;
}

interface FormActionMenuProps {
  onDelete: () => void;
  deleteText?: string;
  additionalActions?: FormAction[];
}

export const FormActionMenu = ({
  onDelete,
  deleteText = 'Verwijderen',
  additionalActions,
}: FormActionMenuProps) => {
  return (
    <Menu>
      <MenuButton className="hover:bg-color-surface-secondary focus:outline-none rounded">
        <DotsThreeVertical className="w-5 h-5 text-color-text-secondary" />
      </MenuButton>
      <MenuItems
        anchor="bottom end"
        className="flex flex-col items-start w-60 focus:outline-none mt-2 bg-color-surface-primary border border-color-border-primary rounded-radius-md shadow-lg z-20"
      >
        {/* Additional actions first */}
        {additionalActions?.map((action, index) => (
          <MenuItem key={index}>
            <div
              className={
                'flex flex-col items-start self-stretch p-2 border-b border-solid border-color-border-primary'
              }
            >
              <MenuItemButton
                label={action.label}
                icon={action.icon}
                onClick={action.onClick}
                textColor={action.textColor}
              />
            </div>
          </MenuItem>
        ))}

        {/* Standard delete action */}
        <MenuItem>
          <div className={'flex flex-col items-start self-stretch p-2'}>
            <MenuItemButton
              label={deleteText}
              icon={TrashSimple}
              onClick={onDelete}
              textColor={'text-color-status-error-primary'}
            />
          </div>
        </MenuItem>
      </MenuItems>
    </Menu>
  );
};
