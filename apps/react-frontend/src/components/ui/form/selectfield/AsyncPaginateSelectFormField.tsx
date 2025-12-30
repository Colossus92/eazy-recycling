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
import { AsyncPaginate } from 'react-select-async-paginate';
import type { GroupBase } from 'react-select';
import clsx from 'clsx';
import { RequiredMarker } from '../RequiredMarker';
import { useEffect, useState } from 'react';

export interface Option {
  value: string;
  label: string;
}

export interface PaginatedResult {
  options: Option[];
  hasMore: boolean;
}

export interface LoadOptionsParams {
  inputValue: string;
  page: number;
}

interface AsyncPaginateSelectFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder: string;
  /**
   * Load options for a given search query and page number.
   * Should return { options, hasMore } where hasMore indicates if there are more pages.
   */
  loadOptions: (params: LoadOptionsParams) => Promise<PaginatedResult>;
  /**
   * Optional function to load a single option by its value.
   * Used to display the correct label when form loads with existing data.
   */
  loadOptionByValue?: (value: string) => Promise<Option | null>;
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

interface Additional {
  page: number;
}

export const AsyncPaginateSelectFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  loadOptions,
  loadOptionByValue,
  value,
  disabled = false,
  testId,
  debounceMs = 300,
  formHook,
}: AsyncPaginateSelectFormFieldProps<TFieldValues>) => {
  const { name, rules, errors, control } = formHook;
  const error = errors[name]?.message as string;

  // Track the selected option object to display the label
  const [selectedOption, setSelectedOption] = useState<Option | null>(null);

  // Adapter for react-select-async-paginate's loadOptions signature
  const loadOptionsAdapter = async (
    inputValue: string,
    _loadedOptions: readonly (Option | GroupBase<Option>)[],
    additional?: Additional
  ) => {
    const page = additional?.page ?? 0;
    const result = await loadOptions({ inputValue, page });

    return {
      options: result.options,
      hasMore: result.hasMore,
      additional: {
        page: page + 1,
      },
    };
  };

  // Load the initial selected option if form has a value but we don't have the option yet
  useEffect(() => {
    const loadInitialOption = async () => {
      // Get the current form value from the control
      const currentValue = control?._formValues?.[name as string];

      // Clear selected option if form value is empty
      if (!currentValue) {
        setSelectedOption(null);
        return;
      }

      // Skip if we already have the correct option
      if (selectedOption && selectedOption.value === currentValue) {
        return;
      }

      // If we have a dedicated loader for single values, use it
      if (loadOptionByValue) {
        const option = await loadOptionByValue(currentValue);
        if (option) {
          setSelectedOption(option);
          return;
        }
      }
      // Fallback: Load first page to find the matching one
      const result = await loadOptions({ inputValue: '', page: 0 });
      const matchingOption = result.options.find(
        (opt) => opt.value === currentValue
      );
      if (matchingOption) {
        setSelectedOption(matchingOption);
      }
    };
    loadInitialOption();
  }, [
    control?._formValues?.[name as string],
    name,
    loadOptions,
    loadOptionByValue,
  ]);

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
        render={({ field: { ref, ...field } }) => (
          <AsyncPaginate
            {...field}
            loadOptions={loadOptionsAdapter}
            additional={{ page: 0 }}
            debounceTimeout={debounceMs}
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
