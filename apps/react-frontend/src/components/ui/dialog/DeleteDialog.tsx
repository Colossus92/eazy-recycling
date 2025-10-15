import { Description, Dialog, DialogPanel } from '@headlessui/react';
import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button/Button.tsx';
import { ErrorDialog } from './ErrorDialog';
import X from '@/assets/icons/X.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';

interface DeleteDialogProps<T> {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  onDelete: () => void | Promise<void>;
  title: string;
  description: string;
  item?: T;
}

export const DeleteDialog = <T,>({
  isOpen,
  setIsOpen,
  onDelete,
  title,
  description,
}: DeleteDialogProps<T>) => {
  const [errorMessage, setErrorMessage] = useState<string | undefined>(undefined);

  useEffect(() => {
    if (isOpen) {
      const timeoutId = setTimeout(() => {
        const deleteButton = document.querySelector(
          '[data-delete-button="true"]'
        ) as HTMLButtonElement;
        if (deleteButton) {
          deleteButton.focus();
        }
      }, 50);

      return () => clearTimeout(timeoutId);
    }
  }, [isOpen]);

  const handleDelete = async () => {
    try {
      await onDelete();
    } catch (error: any) {
      const apiErrorMessage = error?.response?.data?.message || error?.message || 'Er is een onbekende fout opgetreden';
      setErrorMessage(apiErrorMessage);
    }
  };

  return (
    <Dialog
      open={isOpen}
      onClose={() => setIsOpen(false)}
      className="relative z-50"
    >
      <div className="fixed inset-0  bg-black bg-opacity-30 backdrop-blur-xs flex w-screen items-center justify-center p-4">
        <DialogPanel className="w-96 flex flex-col items-center justify-center shrink-0 bg-color-surface-primary rounded-radius-lg">
          <div className="flex py-3 px-4 items-center gap-4 self-stretch border-b border-solid border-color-border-primary">
            <div className={'flex-[1_0_0]'}>
              <h4>{title}</h4>
            </div>
            <Button
              icon={X}
              showText={false}
              variant="tertiary"
              iconPosition="right"
              onClick={() => setIsOpen(false)}
            />
          </div>
          <Description
            className={'flex flex-col items-center self-stretch p-4 gap-4'}
          >
            {description}
          </Description>
          <div
            className="flex py-3 px-4 justify-end items-center self-stretch gap-4 border-t border-solid
            border-color-border-primary"
          >
            <Button
              variant={'secondary'}
              onClick={() => setIsOpen(false)}
              label={'Annuleren'}
            />
            <Button
              icon={TrashSimple}
              variant={'destructive'}
              onClick={handleDelete}
              label={'Verwijderen'}
              autoFocus
              data-delete-button="true"
            />
          </div>
        </DialogPanel>
      </div>
      <ErrorDialog
        isOpen={Boolean(errorMessage)}
        setIsOpen={() => setErrorMessage(undefined)}
        errorMessage={errorMessage || ''}
      />
    </Dialog>
  );
};
