import { useEffect } from 'react';
import { toastService } from '@/components/ui/toast/toastService';
import axios from 'axios';

/**
 * Global error handler component that catches uncaught errors and promise rejections
 * and displays them as toast notifications.
 */
export const GlobalErrorHandler = () => {
  useEffect(() => {
    // Handle uncaught promise rejections
    const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
      console.error('Unhandled promise rejection:', event.reason);
      
      let errorMessage = 'Er is een onverwachte fout opgetreden';
      
      if (axios.isAxiosError(event.reason)) {
        // Handle Axios errors
        const response = event.reason.response;
        if (response?.data?.message) {
          errorMessage = response.data.message;
        } else if (response?.data?.error) {
          errorMessage = response.data.error;
        } else if (response?.statusText) {
          errorMessage = `${response.status}: ${response.statusText}`;
        } else {
          errorMessage = event.reason.message || errorMessage;
        }
      } else if (event.reason instanceof Error) {
        errorMessage = event.reason.message;
      } else if (typeof event.reason === 'string') {
        errorMessage = event.reason;
      }
      
      toastService.error(errorMessage);
      
      // Prevent the default browser error handling
      event.preventDefault();
    };

    // Handle uncaught errors
    const handleError = (event: ErrorEvent) => {
      console.error('Uncaught error:', event.error);
      
      let errorMessage = 'Er is een onverwachte fout opgetreden';
      
      if (event.error instanceof Error) {
        errorMessage = event.error.message;
      } else if (typeof event.error === 'string') {
        errorMessage = event.error;
      } else if (event.message) {
        errorMessage = event.message;
      }
      
      toastService.error(errorMessage);
      
      // Prevent the default browser error handling
      event.preventDefault();
    };

    // Add event listeners
    window.addEventListener('unhandledrejection', handleUnhandledRejection);
    window.addEventListener('error', handleError);

    // Cleanup
    return () => {
      window.removeEventListener('unhandledrejection', handleUnhandledRejection);
      window.removeEventListener('error', handleError);
    };
  }, []);

  // This component doesn't render anything
  return null;
};
