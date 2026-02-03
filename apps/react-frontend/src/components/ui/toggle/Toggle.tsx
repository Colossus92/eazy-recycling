import { Switch } from '@headlessui/react';

interface ToggleProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  disabled?: boolean;
  testId?: string;
}

export const Toggle = ({
  checked,
  onChange,
  label,
  disabled = false,
  testId,
}: ToggleProps) => {
  return (
    <div className="flex items-center justify-start gap-2 whitespace-nowrap">
      <Switch
        checked={checked}
        onChange={onChange}
        disabled={disabled}
        data-testid={testId}
        className={`${
          checked ? 'bg-color-brand-primary' : 'bg-gray-300'
        } ${disabled ? 'opacity-50 cursor-not-allowed' : ''} relative inline-flex h-6 w-11 items-center rounded-full flex-shrink-0`}
      >
        <span className="sr-only">{label ?? 'Toggle'}</span>
        <span
          className={`${
            checked ? 'translate-x-6' : 'translate-x-1'
          } inline-block h-4 w-4 transform rounded-full bg-white transition`}
        />
      </Switch>
      {label && <span className="text-caption-2">{label}</span>}
    </div>
  );
};
