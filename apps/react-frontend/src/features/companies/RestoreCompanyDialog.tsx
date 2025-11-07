import { Description, Dialog, DialogPanel } from '@headlessui/react';
import { Button } from '@/components/ui/button/Button';
import X from '@/assets/icons/X.svg?react';
import ArrowCounterClockwise from '@/assets/icons/ArrowCounterClockwise.svg?react';

interface RestoreCompanyDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  conflictMessage: string;
}

export const RestoreCompanyDialog = ({
  isOpen,
  onClose,
  onConfirm,
  conflictMessage,
}: RestoreCompanyDialogProps) => {
  return (
    <Dialog
      open={isOpen}
      onClose={onClose}
      className="relative z-50"
    >
      <div className="fixed inset-0 bg-black bg-opacity-30 backdrop-blur-xs flex w-screen items-center justify-center p-4">
        <DialogPanel className="w-96 flex flex-col items-center justify-center shrink-0 bg-color-surface-primary rounded-radius-lg">
          <div className="flex py-3 px-4 items-center gap-4 self-stretch border-b border-solid border-color-border-primary">
            <div className={'flex-[1_0_0]'}>
              <h4>Bedrijf herstellen?</h4>
            </div>
            <Button
              icon={X}
              showText={false}
              variant="tertiary"
              iconPosition="right"
              onClick={onClose}
            />
          </div>
          <Description className={'flex flex-col items-center self-stretch p-4 gap-4'}>
            <p className="text-sm">
              {conflictMessage}
            </p>
            <p className="text-sm">
              Wilt u het verwijderde bedrijf herstellen met de nieuwe gegevens?
            </p>
          </Description>
          <div className="flex py-3 px-4 justify-end items-center self-stretch gap-4 border-t border-solid border-color-border-primary">
            <Button
              variant="secondary"
              onClick={onClose}
              label="Annuleren"
            />
            <Button
              icon={ArrowCounterClockwise}
              variant="primary"
              onClick={onConfirm}
              label="Herstellen"
              autoFocus
            />
          </div>
        </DialogPanel>
      </div>
    </Dialog>
  );
};
