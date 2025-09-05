import { Description, Dialog, DialogPanel } from '@headlessui/react';
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
        <DialogPanel className="w-96 flex flex-col items-center justify-center shrink-0 bg-color-surface-primary rounded-radius-lg">
          <div className="flex py-3 px-4 items-center gap-4 self-stretch border-b border-solid border-color-status-error-primary">
            <div className="flex-[1_0_0] flex items-center gap-2">
              <ErrorTriangle className="text-color-status-error-dark h-8 w-8" />
              <h4 className="text-color-text-primary">Er is iets misgegaan</h4>
            </div>
            <Button
              icon={X}
              showText={false}
              variant="destructive"
              iconPosition="right"
              onClick={() => setIsOpen(false)}
            />
          </div>

          <Description className="flex flex-col items-start self-stretch p-4 gap-4 text-body-1 text-color-text-secondary text-left">
            {errorMessage}
          </Description>

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
