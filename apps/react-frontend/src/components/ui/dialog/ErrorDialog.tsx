import { Dialog, DialogPanel } from '@headlessui/react';
import { Button } from '../button/Button';
import X from '@/assets/icons/X.svg?react';
import ErrorTriangle from '@/assets/icons/ErrorTriangle.svg?react';

interface ErrorDialogProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  errorMessage: string;
}

export const ErrorDialog = ({
  isOpen,
  setIsOpen,
  errorMessage,
}: ErrorDialogProps) => {
  return (
    <Dialog
      open={isOpen}
      onClose={() => setIsOpen(false)}
      className="relative z-50"
    >
      <div className="fixed inset-0 bg-black bg-opacity-30 backdrop-blur-xs flex w-screen items-center justify-center p-4">
        <DialogPanel className="w-[488px] flex flex-col items-center justify-center shrink-0 bg-color-surface-primary rounded-radius-lg">
          <div className="flex py-3 px-4 items-center justify-end gap-4 self-stretch ">
            <Button
              icon={X}
              showText={false}
              variant="icon"
              iconPosition="right"
              size={"small"}
              onClick={() => setIsOpen(false)}
            />
          </div>
          <div className="flex flex-col items-center self-stretch px-4 pb-6 gap-4">
            <ErrorTriangle className="text-color-status-error-primary h-16 w-16 mb-4" />
            <div className='flex flex-col items-center justify-center'>
              <h4>
                Er is iets mis gegaan
              </h4>
              {errorMessage}
            </div>
          </div>

          <div className="flex py-3 px-4 justify-end items-center self-stretch gap-4 border-t border-solid border-color-primary">
            <Button
              variant="destructive"
              onClick={() => setIsOpen(false)}
              label="OK"
            />
          </div>
        </DialogPanel>
      </div>
    </Dialog>
  );
};
