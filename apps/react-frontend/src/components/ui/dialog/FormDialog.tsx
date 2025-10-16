import { ReactNode, useRef } from 'react';
import { Dialog, DialogPanel } from '@headlessui/react';
import { useFormDialogEnterSubmit } from '@/hooks/useFormDialogEnterSubmit';

interface DialogProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  children?: ReactNode;
  width?: string;
}

export const FormDialog = ({ isOpen, setIsOpen, children, width = 'w-[488px]' }: DialogProps) => {
  const containerRef = useRef<HTMLDivElement>(null);
  useFormDialogEnterSubmit({ isOpen, containerRef });

  return (
    <Dialog
      open={isOpen}
      onClose={() => setIsOpen(false)}
      className="relative z-50"
    >
      <div className="fixed inset-0 bg-black bg-opacity-30 backdrop-blur-xs flex w-screen items-center justify-center p-4">
        <div ref={containerRef}>
          <DialogPanel className={`flex flex-col items-center justify-center shrink-0 ${width} bg-color-surface-primary rounded-radius-lg`}>
            <div className="dialog-content flex flex-col items-center justify-center shrink-0 self-stretch bg-color-surface-primary rounded-radius-lg">
              {children}
            </div>
          </DialogPanel>
        </div>
      </div>
    </Dialog>
  );
};
