import { useState } from 'react';
import { AxiosError } from 'axios';
import { ErrorDialog } from '@/components/ui/dialog/ErrorDialog.tsx';

/**
 * Custom hook for handling errors in a consistent way across the application
 * Provides error state management and a reusable error dialog
 */
export const useErrorHandling = () => {
  const [isErrorDialogOpen, setIsErrorDialogOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  /**
   * Extract a readable error message from various error types
   */
  const extractErrorMessage = (error: unknown): string => {
    let errorMessage = 'Onbekende fout';

    if (error instanceof AxiosError) {
      errorMessage =
        error.response?.data?.message || error.message || errorMessage;
    } else if (error instanceof Error) {
      errorMessage = error.message;
    }

    return errorMessage;
  };

  /**
   * Handle an error by extracting the message and showing the dialog
   */
  const handleError = (error: unknown) => {
    const message = extractErrorMessage(error);
    setErrorMessage(message);
    setIsErrorDialogOpen(true);
  };

  /**
   * Error dialog component that can be included in your component's JSX
   */
  const ErrorDialogComponent = () => (
    <ErrorDialog
      isOpen={isErrorDialogOpen}
      setIsOpen={setIsErrorDialogOpen}
      errorMessage={errorMessage}
    />
  );

  return {
    isErrorDialogOpen,
    setIsErrorDialogOpen,
    errorMessage,
    setErrorMessage,
    handleError,
    ErrorDialogComponent,
  };
};
