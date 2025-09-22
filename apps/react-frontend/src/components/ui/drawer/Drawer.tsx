'use client';

import { Dialog, DialogPanel } from '@headlessui/react';
import { ReactNode } from 'react';
import X from '@/assets/icons/X.svg?react';
import Pencil from '@/assets/icons/PencilSimple.svg?react';
import Trash from '@/assets/icons/TrashSimple.svg?react';
import { Button } from '@/components/ui/button/Button.tsx';

interface DrawerProps {
  title: string;
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  children?: ReactNode;
  onEdit?: () => void;
  onDelete?: () => void;
}

export const Drawer = ({
  title,
  isOpen,
  setIsOpen,
  children,
  onEdit,
  onDelete,
}: DrawerProps) => {
  return (
    <Dialog open={isOpen} onClose={setIsOpen} className="relative z-10">
      <div className="fixed inset-0"/>

      <div className="fixed inset-0 overflow-hidden">
        <div className="absolute inset-0 overflow-hidden">
          <div className="pointer-events-none fixed inset-y-0 right-0 flex max-w-full pl-10">
            <DialogPanel
              transition
              className="pointer-events-auto w-screen max-w-md transform transition duration-500 ease-in-out data-closed:translate-x-full sm:duration-700"
            >
              <div className="flex h-full flex-col overflow-y-scroll bg-white shadow-xl border border-solid border-color-border-primary">
                <div
                  className={
                    'flex items-center self-stretch gap-4 px-4 py-3 border-b border-solid border-color-border-primary'
                  }
                >
                  <div className={'flex-1'}>
                    <span className="text-h4">{title}</span>
                  </div>
                  <div className={'flex items-center gap-2'}>
                    {onEdit && (
                      <Button
                        icon={Pencil}
                        showText={false}
                        variant="icon"
                        iconPosition="right"
                        onClick={onEdit}
                      />
                    )}
                    {onDelete && (
                      <Button
                        icon={Trash}
                        showText={false}
                        variant="icon"
                        iconPosition="right"
                        onClick={onDelete}
                      />
                    )}
                    <Button
                      icon={X}
                      showText={false}
                      variant="icon"
                      iconPosition="right"
                      onClick={() => setIsOpen(false)}
                    />
                  </div>
                </div>
                {children}
              </div>
            </DialogPanel>
          </div>
        </div>
      </div>
    </Dialog>
  );
};
