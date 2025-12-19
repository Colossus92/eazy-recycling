import { ReactNode, useRef, useEffect } from 'react';
import { Dialog, DialogPanel } from '@headlessui/react';
import { useFormDialogEnterSubmit } from '@/hooks/useFormDialogEnterSubmit';

interface DialogProps {
  isOpen: boolean;
  setIsOpen?: (value: boolean) => void;
  children?: ReactNode;
  width?: string;
}

export const FormDialog = ({ isOpen, setIsOpen, children, width = 'w-[488px]' }: DialogProps) => {
  const containerRef = useRef<HTMLDivElement>(null);
  useFormDialogEnterSubmit({ isOpen, containerRef });

  useEffect(() => {
    if (!isOpen || !setIsOpen) return; 

    const handleEscapeKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    document.addEventListener('keydown', handleEscapeKey);
    return () => document.removeEventListener('keydown', handleEscapeKey);
  }, [isOpen, setIsOpen]);

  return (
    <Dialog
      open={isOpen}
      onClose={() => {}}
      className="relative z-50"
    >
      <div className="fixed inset-0 bg-black bg-opacity-30 backdrop-blur-xs flex w-screen items-center justify-center p-4">
        <div ref={containerRef}>
          <DialogPanel className={`flex flex-col items-center justify-center shrink-0 ${width} bg-color-surface-primary rounded-radius-lg`}>
            <div className="dialog-content flex flex-col items-center justify-center shrink-0 self-stretch bg-color-surface-primary rounded-radius-lg h-full">
              {children}
            </div>
          </DialogPanel>
        </div>
      </div>
    </Dialog>
  );
};
