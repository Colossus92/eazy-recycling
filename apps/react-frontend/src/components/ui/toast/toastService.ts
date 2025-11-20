import { toast, ToastContent } from 'react-toastify';
import { ReactNode } from 'react';

const position = 'bottom-right';

export const toastService = {
  success: (message: string | ReactNode) => {
    toast.success(message as ToastContent, {
      className:
        'bg-color-status-success-light border-l-4 border-color-status-success-dark',
      progressClassName: 'bg-color-status-success-primary',
      position,
    });
  },

  error: (message: string | ReactNode) => {
    toast.error(message as ToastContent, {
      className: 'bg-color-status-error-light text-color-text-secondary',
      progressClassName: 'bg-color-status-error-primary',
      position,
    });
  },

  warn: (message: string | ReactNode) => {
    toast.warn(message as ToastContent, {
      className: 'bg-color-status-warning-light text-color-text-secondary',
      progressClassName: 'bg-color-status-warning-primary',
      position,
    });
  },
};
