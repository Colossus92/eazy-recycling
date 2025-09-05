import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/react';
import { SVGProps } from 'react';
import DotsThreeVertical from '@/assets/icons/DotsThreeVertical.svg?react';
import PencilSimple from '@/assets/icons/PencilSimple.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';
import { MenuItemButton } from '@/components/layouts/MenuItemButton.tsx';

export interface AdditionalAction<T> {
  label: string;
  icon: React.FunctionComponent<SVGProps<SVGSVGElement>>;
  onClick: (item: T) => void;
  textColor?: string;
}

interface ActionMenuProps<T> {
  onEdit: (item: T) => void;
  onDelete: (item: T) => void;
  item: T;
  additionalActions?: AdditionalAction<T>[];
}

export const ActionMenu = <T,>({
  onEdit,
  onDelete,
  item,
  additionalActions,
}: ActionMenuProps<T>) => {
  return (
    <Menu>
      <MenuButton className="p-2 hover:bg-color-surface-secondary focus:outline-none rounded">
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
                onClick={() => action.onClick(item)}
                textColor={action.textColor}
              />
            </div>
          </MenuItem>
        ))}

        {/* Standard edit action */}
        <MenuItem>
          <div
            className={
              'flex flex-col items-start self-stretch p-2 border-b border-solid border-color-border-primary'
            }
          >
            <MenuItemButton
              label={'Bewerken'}
              icon={PencilSimple}
              onClick={() => onEdit(item)}
            />
          </div>
        </MenuItem>

        {/* Standard delete action */}
        <MenuItem>
          <div className={'flex flex-col items-start self-stretch p-2'}>
            <MenuItemButton
              label={'Verwijderen'}
              icon={TrashSimple}
              onClick={() => onDelete(item)}
              textColor={'text-color-status-error-primary'}
            />
          </div>
        </MenuItem>
      </MenuItems>
    </Menu>
  );
};
