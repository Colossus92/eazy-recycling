import {
  Control,
  Controller,
  FieldErrors,
  FieldPath,
  FieldPathValue,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import {
  Combobox,
  ComboboxButton,
  ComboboxInput,
  ComboboxOption,
  ComboboxOptions,
} from '@headlessui/react';
import { useState } from 'react';
import clsx from 'clsx';
import { RequiredMarker } from '../RequiredMarker.tsx';

export interface ComboboxItem {
  id: string;
  label: string;
  displayValue?: string;
}

interface ComboboxFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder: string;
  items: ComboboxItem[];
  value?: FieldPathValue<TFieldValues, FieldPath<TFieldValues>>;
  disabled?: boolean;
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control?: Control<TFieldValues>;
  };
  getFilteredItems?: (query: string, items: ComboboxItem[]) => ComboboxItem[];
  displayValue?: (id: string, items: ComboboxItem[]) => string;
  testId?: string;
}

export const ComboboxFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  items,
  value,
  disabled = false,
  formHook,
  getFilteredItems,
  displayValue,
  testId,
}: ComboboxFormFieldProps<TFieldValues>) => {
  const { name, rules, errors, control } = formHook;
  const error = errors[name]?.message as string;
  const [query, setQuery] = useState('');

  const defaultFilterItems = (query: string, items: ComboboxItem[]) => {
    return query === ''
      ? items
      : items.filter((item) =>
          item.label.toLowerCase().includes(query.toLowerCase())
        );
  };

  const defaultDisplayValue = (id: string, items: ComboboxItem[]) => {
    const selected = items.find((item) => item.id === id);
    return selected ? selected.displayValue || selected.label : id;
  };

  const filteredItems = (getFilteredItems || defaultFilterItems)(query, items);

  return (
    <div className="flex flex-col items-start self-stretch gap-1 w-full">
      <div className="flex items-center self-stretch justify-between">
        <span className="text-caption-2">
          {title}
          <RequiredMarker required={rules?.required} />
        </span>
      </div>

      <Controller
        control={control}
        name={name}
        rules={rules}
        defaultValue={value}
        render={({ field }) => (
          <div className="w-full">
            <Combobox
              value={field.value || ''}
              onChange={field.onChange}
              disabled={disabled}
            >
              <div className="relative">
                <div className="relative w-full">
                  <ComboboxInput
                    className={clsx(
                      'w-full h-10 rounded-radius-md border border-solid bg-color-surface-primary px-3 py-2 text-body-1 focus:outline-none',
                      disabled
                        ? 'border-color-border-primary text-color-text-disabled cursor-not-allowed'
                        : error
                          ? 'border-color-status-error-dark'
                          : 'border-color-border-primary focus:border-color-primary hover:bg-color-brand-light hover:border-color-brand-dark'
                    )}
                    data-testid={testId}
                    placeholder={placeholder}
                    onChange={(event) => setQuery(event.target.value)}
                    displayValue={(id: string) =>
                      (displayValue || defaultDisplayValue)(id, items)
                    }
                  />
                  <ComboboxButton className="absolute inset-y-0 right-0 flex items-center pr-2">
                    <svg
                      className="h-5 w-5 text-color-text-secondary"
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                      aria-hidden="true"
                    >
                      <path
                        fillRule="evenodd"
                        d="M10 3a.75.75 0 01.55.24l3.25 3.5a.75.75 0 11-1.1 1.02L10 4.852 7.3 7.76a.75.75 0 01-1.1-1.02l3.25-3.5A.75.75 0 0110 3zm-3.76 9.2a.75.75 0 011.06.04l2.7 2.908 2.7-2.908a.75.75 0 111.1 1.02l-3.25 3.5a.75.75 0 01-1.1 0l-3.25-3.5a.75.75 0 01.04-1.06z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </ComboboxButton>
                </div>
                <ComboboxOptions className="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-radius-md bg-color-surface-primary py-1 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
                  {filteredItems.length === 0 && query !== '' ? (
                    <div className="relative cursor-default select-none py-2 px-4 text-color-text-disabled">
                      Geen resultaten gevonden.
                    </div>
                  ) : (
                    filteredItems.map((item) => (
                      <ComboboxOption
                        key={item.id}
                        className={({ active }) =>
                          `relative cursor-pointer select-none py-2 px-4 ${
                            active
                              ? 'bg-color-surface-secondary text-color-text-primary'
                              : 'text-color-text-primary'
                          }`
                        }
                        value={item.id}
                      >
                        {({ selected }) => (
                          <>
                            <span
                              className={`block truncate ${
                                selected ? 'font-medium' : 'font-normal'
                              }`}
                            >
                              {item.label}
                            </span>
                          </>
                        )}
                      </ComboboxOption>
                    ))
                  )}
                </ComboboxOptions>
              </div>
            </Combobox>
            {error && (
              <span className="text-caption-1 text-color-status-error-dark">
                {error}
              </span>
            )}
          </div>
        )}
      />
    </div>
  );
};
