import { toast } from 'react-toastify';

export const toastService = {
  success: (message: string) => {
    toast.success(message, {
      className:
        'bg-color-status-success-light border-l-4 border-color-status-success-dark',
      progressClassName: 'bg-color-status-success-primary',
    });
  },

  error: (message: string) => {
    toast.error(message, {
      className: 'bg-color-status-error-light text-color-text-secondary',
      progressClassName: 'bg-color-status-error-primary',
    });
  },

  warn: (message: string) => {
    toast.warn(message, {
      className: 'bg-color-status-warning-light text-color-text-secondary',
      progressClassName: 'bg-color-status-warning-primary',
    });
  },
};
