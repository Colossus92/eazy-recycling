import { Checkbox, Field, Label } from '@headlessui/react';
import clsx from 'clsx';
import { ReactNode } from 'react';
import {
  Control,
  Controller,
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';

interface CheckboxFieldProps<TFieldValues extends FieldValues> {
  children?: ReactNode;
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control: Control<TFieldValues>;
  };
}

export const CheckboxField = <T extends FieldValues>({
  children,
  formHook,
}: CheckboxFieldProps<T>) => {
  const baseClasses =
    'block size-4 border rounded-radius-xs flex justify-center items-center p-0.5';
  const neutralClasses =
    'bg-color-surface-primary border border-solid border-color-border-primary';
  const hoverClasses = 'hover:border-color-brand-primary';
  const checkedClasses = 'bg-color-brand-primary border-color-brand-primary';

  return (
    <Field className={'flex items-center gap-2 self-stretch'}>
      <Controller
        control={formHook.control}
        name={formHook.name}
        rules={formHook.rules}
        render={({ field }) => (
          <Checkbox
            checked={!!field.value}
            onChange={(checked) => field.onChange(checked)}
          >
            {({ checked, hover }) => (
              <span
                className={clsx(
                  baseClasses,
                  !checked && neutralClasses,
                  checked && checkedClasses,
                  !checked && hover && hoverClasses
                )}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className={clsx(
                    'stroke-white',
                    checked ? 'opacity-100' : 'opacity-0'
                  )}
                  width="12"
                  height="12"
                  viewBox="0 0 12 12"
                  fill="none"
                >
                  <path
                    d="M10.7653 3.64052L4.76528 9.64052C4.73045 9.67539 4.68909 9.70305 4.64357 9.72192C4.59804 9.74079 4.54925 9.7505 4.49996 9.7505C4.45068 9.7505 4.40189 9.74079 4.35636 9.72192C4.31084 9.70305 4.26948 9.67539 4.23465 9.64052L1.60965 7.01552C1.53929 6.94516 1.49976 6.84972 1.49976 6.75021C1.49976 6.6507 1.53929 6.55526 1.60965 6.4849C1.68002 6.41453 1.77545 6.375 1.87496 6.375C1.97448 6.375 2.06991 6.41453 2.14028 6.4849L4.49996 8.84505L10.2347 3.1099C10.305 3.03953 10.4005 3 10.5 3C10.5995 3 10.6949 3.03953 10.7653 3.1099C10.8356 3.18026 10.8752 3.2757 10.8752 3.37521C10.8752 3.47472 10.8356 3.57016 10.7653 3.64052Z"
                    fill="white"
                  />
                </svg>
              </span>
            )}
          </Checkbox>
        )}
      />
      <Label>{children}</Label>
    </Field>
  );
};
