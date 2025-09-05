import { useEffect, RefObject } from 'react';

interface UseFormDialogEnterSubmitProps {
  isOpen: boolean;
  containerRef: RefObject<HTMLElement>;
}

/**
 * Custom hook that handles form submission when Enter key is pressed within a dialog
 *
 * @param isOpen - Whether the dialog is currently open
 * @param containerRef - Ref to the dialog container element
 */
export const useFormDialogEnterSubmit = ({
  isOpen,
  containerRef,
}: UseFormDialogEnterSubmitProps) => {
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Enter' && isOpen) {
        const activeElement = document.activeElement;
        const isWithinDialog = containerRef.current?.contains(
          activeElement as Node
        );

        if (isWithinDialog) {
          if (activeElement && activeElement.tagName !== 'BUTTON') {
            const form = containerRef.current?.querySelector(
              'form'
            ) as HTMLFormElement;
            if (form) {
              e.preventDefault();
              form.requestSubmit();
            }
          }
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, containerRef]);
};
