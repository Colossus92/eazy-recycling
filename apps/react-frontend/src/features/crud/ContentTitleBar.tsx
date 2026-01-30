import { ReactNode } from 'react';
import { Button } from '@/components/ui/button/Button.tsx';
import MagnifyingGlass from '@/assets/icons/MagnifyingGlass.svg?react';
import Funnel from '@/assets/icons/Funnel.svg?react';
import { TextInput } from '@/components/ui/form/TextInput.tsx';

interface ContentTitleBarProps {
  setQuery: (value: string) => void;
  children?: ReactNode;
  leftActions?: ReactNode;
  setIsFilterOpen?: (value: boolean) => void;
  hideSearchBar?: boolean;
}

export const ContentTitleBar = ({
  setQuery,
  children,
  leftActions,
  setIsFilterOpen,
  hideSearchBar = false,
}: ContentTitleBarProps) => {
  return (
    <div className="flex justify-between items-center self-stretch px-4">
      <div className="flex items-center gap-4">
        {!hideSearchBar && (
          <TextInput
            icon={MagnifyingGlass}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Zoek..."
          />
        )}
        {setIsFilterOpen && (
          <Button
            variant={'secondary'}
            icon={Funnel}
            label={'Filter'}
            onClick={() => setIsFilterOpen(true)}
          />
        )}
        {leftActions}
      </div>
      <div className={'flex justify-end items-center gap-4'}>{children}</div>
    </div>
  );
};
