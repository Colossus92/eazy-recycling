/**
 * Tailwind CSS class compositions for reuse across components
 */

// Form input styling
export const formInputClasses = {
  base: 'rounded-radius-md border border-solid w-full bg-color-surface-primary',
  text: {
    default: 'text-color-text-secondary',
    disabled: 'text-color-text-disabled',
  },
  border: {
    default: 'border-color-border-primary',
    error: 'border-color-status-error-dark',
    disabled: 'border-color-border-primary',
  },
  background: {
    default: '',
    disabled: 'cursor-not-allowed',
    hover:
      'hover:bg-color-brand-light hover:border-color-brand-dark focus:border-color-border-primary',
  },
  padding: {
    withIcon: 'pl-10 pr-10',
    default: 'px-3 py-2',
  },
  container: 'relative flex items-center text-body-1 w-full',
  icon: 'absolute left-3 pointer-events-none flex items-center',
};
