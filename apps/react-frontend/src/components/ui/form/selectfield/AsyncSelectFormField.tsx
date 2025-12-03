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
import AsyncSelect from 'react-select/async';
import clsx from 'clsx';
import { RequiredMarker } from '../RequiredMarker';
import { useCallback, useEffect, useRef, useState } from 'react';

export interface Option {
  value: string;
  label: string;
}

interface AsyncSelectFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder: string;
  loadOptions: (inputValue: string) => Promise<Option[]>;
  value?: FieldPathValue<TFieldValues, FieldPath<TFieldValues>>;
  disabled?: boolean;
  testId?: string;
  debounceMs?: number;
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control?: Control<TFieldValues>;
  };
}

export const AsyncSelectFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  loadOptions,
  value,
  disabled = false,
  testId,
  debounceMs = 300,
  formHook,
}: AsyncSelectFormFieldProps<TFieldValues>) => {
  const { name, rules, errors, control } = formHook;
  const error = errors[name]?.message as string;

  // Track the selected option object to display the label
  const [selectedOption, setSelectedOption] = useState<Option | null>(null);

  // Debounce implementation
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const debouncedLoadOptions = useCallback(
    (inputValue: string, callback: (options: Option[]) => void) => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }

      timeoutRef.current = setTimeout(() => {
        loadOptions(inputValue).then(callback);
      }, debounceMs);
    },
    [loadOptions, debounceMs]
  );

  // Load the initial selected option if form has a value but we don't have the option yet
  useEffect(() => {
    const loadInitialOption = async () => {
      // Get the current form value from the control
      const currentValue = control?._formValues?.[name as string];
      if (currentValue && !selectedOption) {
        // Load options to find the matching one
        const options = await loadOptions('');
        const matchingOption = options.find(
          (opt) => opt.value === currentValue
        );
        if (matchingOption) {
          setSelectedOption(matchingOption);
        }
      }
    };
    loadInitialOption();
  }, [control, name, loadOptions, selectedOption]);

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
          <AsyncSelect
            {...field}
            loadOptions={debouncedLoadOptions}
            defaultOptions={true}
            cacheOptions={true}
            placeholder={placeholder}
            isDisabled={disabled}
            isClearable={true}
            classNamePrefix="react-select"
            noOptionsMessage={() => 'Geen opties beschikbaar'}
            loadingMessage={() => 'Laden...'}
            id={testId || `select-${name}`}
            menuPortalTarget={document.body}
            className={clsx(
              'w-full text-body-1',
              disabled
                ? 'text-color-text-disabled cursor-not-allowed'
                : 'text-color-text-secondary'
            )}
            styles={{
              control: (base, state) => ({
                ...base,
                minHeight: '40px',
                height: '40px',
                borderRadius: '8px',
                borderWidth: '1px',
                borderStyle: 'solid',
                borderColor: disabled
                  ? '#E3E8F3'
                  : error
                    ? '#F04438'
                    : state.isFocused
                      ? '#1E77F8'
                      : '#E3E8F3',
                backgroundColor: '#FFFFFF',
                cursor: disabled ? 'not-allowed' : 'default',
                boxShadow: 'none',
                '&:hover': {
                  borderColor: disabled
                    ? '#E3E8F3'
                    : error
                      ? '#F04438'
                      : '#1E77F8',
                  backgroundColor: disabled ? '#FFFFFF' : '#F3F8FF',
                },
              }),
              menuPortal: (base) => ({
                ...base,
                zIndex: 9999,
              }),
              input: (base) => ({
                ...base,
                'input:focus': {
                  boxShadow: 'none',
                },
              }),
            }}
            classNames={{
              placeholder: () => clsx('text-color-text-disabled', 'italic'),
              option: ({ isSelected, isFocused }) =>
                clsx(
                  'cursor-pointer',
                  isSelected
                    ? 'bg-color-primary text-color-text-secondary'
                    : isFocused
                      ? 'bg-color-surface-secondary text-color-text-primary'
                      : 'bg-color-surface-primary text-color-text-primary'
                ),
              singleValue: () => 'text-color-text-primary',
              valueContainer: () => 'px-3 py-2',
            }}
            value={selectedOption}
            onChange={(newOption) => {
              const option = newOption as Option | null;
              setSelectedOption(option);
              field.onChange(option ? option.value : null);
            }}
          />
        )}
      />
      {error && (
        <span className="text-caption-1 text-color-status-error-dark">
          {error}
        </span>
      )}
    </div>
  );
};
