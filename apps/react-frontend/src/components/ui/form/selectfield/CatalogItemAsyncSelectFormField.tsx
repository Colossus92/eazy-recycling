import { CatalogItemResponseItemTypeEnum } from '@/api/client';
import { CatalogItem, catalogService } from '@/api/services/catalogService';
import clsx from 'clsx';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { FieldErrors } from 'react-hook-form';
import AsyncSelect from 'react-select/async';
import { GroupBase } from 'react-select';
import { RequiredMarker } from '../RequiredMarker';

interface CatalogItemOption {
  value: string;
  label: string;
  item: CatalogItem;
}

interface LineFieldValue {
  id: string;
  catalogItemId: string;
  wasteStreamNumber?: string;
  weightValue: string;
  weightUnit: string;
}

interface CatalogItemAsyncSelectFormFieldProps {
  title: string;
  placeholder: string;
  consignorPartyId?: string;
  disabled?: boolean;
  testId?: string;
  debounceMs?: number;
  required?: boolean;
  index: number;
  field: LineFieldValue;
  update: (index: number, value: LineFieldValue, selectedItem?: CatalogItem) => void;
  errors?: FieldErrors;
}

// Type labels for grouping
const typeLabels: Record<string, string> = {
  [CatalogItemResponseItemTypeEnum.WasteStream]: 'Afvalstromen',
  [CatalogItemResponseItemTypeEnum.Material]: 'Materialen',
  [CatalogItemResponseItemTypeEnum.Product]: 'Producten',
};

// Type order for sorting groups
const typeOrder = [
  CatalogItemResponseItemTypeEnum.WasteStream,
  CatalogItemResponseItemTypeEnum.Material,
  CatalogItemResponseItemTypeEnum.Product,
];

export const CatalogItemAsyncSelectFormField = ({
  title,
  placeholder,
  consignorPartyId,
  disabled = false,
  testId,
  debounceMs = 300,
  required = false,
  index,
  field,
  update,
  errors,
}: CatalogItemAsyncSelectFormFieldProps) => {
  // Get error for this field
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const linesErrors = (errors as any)?.lines as Array<Record<string, { message?: string }>> | undefined;
  const error = linesErrors?.[index]?.catalogItemId?.message;

  // Track the selected option - initialize from field value if available
  const [selectedOption, setSelectedOption] = useState<CatalogItemOption | null>(null);
  
  // Generate a unique cache key based on consignorPartyId to prevent cache sharing
  const cacheKey = useMemo(() => 
    `catalog-${consignorPartyId || 'all'}-${index}`, 
    [consignorPartyId, index]
  );

  // Debounce implementation
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Convert catalog items to grouped options
  const catalogItemsToGroupedOptions = useCallback(
    (
      items: CatalogItem[]
    ): GroupBase<CatalogItemOption>[] => {
      // Group items by type
      const grouped = items.reduce(
        (acc, item) => {
          const type = item.itemType;
          if (!acc[type]) {
            acc[type] = [];
          }
          acc[type].push({
            value: String(item.id),
            label: item.wasteStreamNumber
              ? `${item.name} (${item.wasteStreamNumber})`
              : item.name,
            item,
          });
          return acc;
        },
        {} as Record<string, CatalogItemOption[]>
      );

      // Sort groups by type order and create grouped options
      return typeOrder
        .filter((type) => grouped[type]?.length > 0)
        .map((type) => ({
          label: typeLabels[type],
          options: grouped[type],
        }));
    },
    []
  );

  // Load options for async select
  const loadOptions = useCallback(
    async (inputValue: string): Promise<GroupBase<CatalogItemOption>[]> => {
      const items = await catalogService.search(
        inputValue || undefined,
        consignorPartyId || undefined
      );
      return catalogItemsToGroupedOptions(items);
    },
    [consignorPartyId, catalogItemsToGroupedOptions]
  );

  const debouncedLoadOptions = useCallback(
    (
      inputValue: string,
      callback: (options: GroupBase<CatalogItemOption>[]) => void
    ) => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }

      timeoutRef.current = setTimeout(() => {
        loadOptions(inputValue).then(callback);
      }, debounceMs);
    },
    [loadOptions, debounceMs]
  );

  // Track the previous catalogItemId to detect changes
  const prevCatalogItemIdRef = useRef<string | undefined>(undefined);
  
  // Load initial selected option from field value
  useEffect(() => {
    const currentValue = field.catalogItemId;
    const prevValue = prevCatalogItemIdRef.current;
    
    // Update ref for next comparison
    prevCatalogItemIdRef.current = currentValue;
    
    // If field has no value, clear the selection
    if (!currentValue) {
      setSelectedOption(null);
      return;
    }
    
    // If value hasn't changed, skip (prevents unnecessary API calls)
    if (prevValue === currentValue) {
      return;
    }
    
    // Load options to find the matching one
    const loadInitialOption = async () => {
      const groupedOptions = await loadOptions('');
      for (const group of groupedOptions) {
        const matchingOption = group.options.find(
          (opt) => opt.value === currentValue
        );
        if (matchingOption) {
          setSelectedOption(matchingOption);
          return;
        }
      }
      
      // If no matching option found, clear selection
      setSelectedOption(null);
    };
    loadInitialOption();
  }, [field.catalogItemId, loadOptions]);

  return (
    <div className="flex flex-col items-start self-stretch gap-1 w-full">
      <div className="flex items-center self-stretch justify-between">
        <span className="text-caption-2">
          {title}
          <RequiredMarker required={required} />
        </span>
      </div>

      <AsyncSelect<CatalogItemOption, false, GroupBase<CatalogItemOption>>
        key={cacheKey}
        value={selectedOption}
        loadOptions={debouncedLoadOptions}
        defaultOptions
        cacheOptions={false}
        placeholder={placeholder}
        isDisabled={disabled}
        isClearable={true}
        classNamePrefix="react-select"
        noOptionsMessage={() => 'Geen items gevonden'}
        loadingMessage={() => 'Laden...'}
        id={testId || `catalog-item-select-${index}`}
        instanceId={cacheKey}
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
          group: (base) => ({
            ...base,
            paddingTop: 0,
            paddingBottom: 0,
          }),
          groupHeading: (base) => ({
            ...base,
            fontWeight: 600,
            fontSize: '12px',
            color: '#6B7280',
            textTransform: 'uppercase',
            backgroundColor: '#F9FAFB',
            padding: '8px 12px',
            marginBottom: 0,
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
        onChange={(newOption) => {
          const option = newOption as CatalogItemOption | null;
          setSelectedOption(option);

          if (option) {
            // Update the field array item with the new values
            update(index, {
              ...field,
              catalogItemId: option.value,
              wasteStreamNumber: option.item.wasteStreamNumber || undefined,
            }, option.item);
          } else {
            // Clear the values
            update(index, {
              ...field,
              catalogItemId: '',
              wasteStreamNumber: undefined,
            });
          }
        }}
      />
      {error && (
        <span className="text-caption-1 text-color-status-error-dark">
          {error}
        </span>
      )}
    </div>
  );
};
