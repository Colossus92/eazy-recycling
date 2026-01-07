import { CatalogItemResponseItemTypeEnum } from '@/api/client';
import { useCatalogItems, filterCatalogItems } from '@/api/hooks/useCatalogItems';
import { CatalogItem, CatalogItemType } from '@/api/services/catalogService';
import clsx from 'clsx';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { FieldErrors } from 'react-hook-form';
import Select, { GroupBase, StylesConfig } from 'react-select';
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

export type CatalogItemTypeFilter = 'MATERIAL' | 'PRODUCT' | 'WASTE_STREAM' | 'MATERIAL_OR_WASTE_STREAM' | 'ALL';

interface CatalogItemAsyncSelectFormFieldProps {
  title: string;
  placeholder: string;
  consignorPartyId?: string;
  disabled?: boolean;
  testId?: string;
  required?: boolean;
  index: number;
  field: LineFieldValue;
  update: (
    index: number,
    value: LineFieldValue,
    selectedItem?: CatalogItem
  ) => void;
  errors?: FieldErrors;
  typeFilter?: CatalogItemTypeFilter;
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

/**
 * Maps typeFilter to API type parameter for server-side filtering
 */
const getApiTypeFilter = (typeFilter: CatalogItemTypeFilter): CatalogItemType | undefined => {
  switch (typeFilter) {
    case 'MATERIAL':
      return CatalogItemResponseItemTypeEnum.Material;
    case 'PRODUCT':
      return CatalogItemResponseItemTypeEnum.Product;
    case 'WASTE_STREAM':
      return CatalogItemResponseItemTypeEnum.WasteStream;
    case 'MATERIAL_OR_WASTE_STREAM':
    case 'ALL':
    default:
      return undefined; // Fetch all and filter client-side if needed
  }
};

/**
 * Client-side filter for MATERIAL_OR_WASTE_STREAM type
 */
const filterByTypeClient = (items: CatalogItem[], typeFilter: CatalogItemTypeFilter): CatalogItem[] => {
  if (typeFilter === 'MATERIAL_OR_WASTE_STREAM') {
    return items.filter(
      (item) =>
        item.itemType === CatalogItemResponseItemTypeEnum.Material ||
        item.itemType === CatalogItemResponseItemTypeEnum.WasteStream
    );
  }
  return items;
};

export const CatalogItemAsyncSelectFormField = ({
  title,
  placeholder,
  consignorPartyId,
  disabled = false,
  testId,
  required = false,
  index,
  field,
  update,
  errors,
  typeFilter = 'ALL',
}: CatalogItemAsyncSelectFormFieldProps) => {
  // Get error for this field
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const linesErrors = (errors as any)?.lines as
    | Array<Record<string, { message?: string }>>
    | undefined;
  const error = linesErrors?.[index]?.catalogItemId?.message;

  // Track the selected option
  const [selectedOption, setSelectedOption] = useState<CatalogItemOption | null>(null);
  const [inputValue, setInputValue] = useState('');

  // Generate a unique instance key
  const instanceKey = useMemo(
    () => `catalog-${consignorPartyId || 'all'}-${typeFilter}-${index}`,
    [consignorPartyId, typeFilter, index]
  );

  // Use React Query to fetch and cache catalog items
  const apiTypeFilter = getApiTypeFilter(typeFilter);
  const { data: catalogItems = [], isLoading } = useCatalogItems({
    consignorPartyId,
    type: apiTypeFilter,
    enabled: !!consignorPartyId,
  });

  // Apply client-side filtering if needed (for MATERIAL_OR_WASTE_STREAM)
  const filteredItems = useMemo(() => {
    return filterByTypeClient(catalogItems, typeFilter);
  }, [catalogItems, typeFilter]);

  // Convert catalog items to grouped options
  const catalogItemsToGroupedOptions = useCallback(
    (items: CatalogItem[]): GroupBase<CatalogItemOption>[] => {
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

      return typeOrder
        .filter((type) => grouped[type]?.length > 0)
        .map((type) => ({
          label: typeLabels[type],
          options: grouped[type],
        }));
    },
    []
  );

  // Generate options based on current input value (client-side filtering)
  const options = useMemo(() => {
    const searchFiltered = filterCatalogItems(filteredItems, inputValue);
    return catalogItemsToGroupedOptions(searchFiltered);
  }, [filteredItems, inputValue, catalogItemsToGroupedOptions]);

  // Track the previous field values to detect changes
  const prevFieldRef = useRef<{
    catalogItemId?: string;
    wasteStreamNumber?: string;
  }>({});

  // Update selected option when field value changes or data loads
  useEffect(() => {
    const currentCatalogItemId = field.catalogItemId;
    const currentWasteStreamNumber = field.wasteStreamNumber;
    const prev = prevFieldRef.current;

    prevFieldRef.current = {
      catalogItemId: currentCatalogItemId,
      wasteStreamNumber: currentWasteStreamNumber,
    };

    if (!currentCatalogItemId) {
      setSelectedOption(null);
      return;
    }

    // Skip if values haven't changed and we already have a selection
    if (
      prev.catalogItemId === currentCatalogItemId &&
      prev.wasteStreamNumber === currentWasteStreamNumber &&
      selectedOption
    ) {
      return;
    }

    // Find matching option in loaded data
    for (const item of filteredItems) {
      const isMatch = currentWasteStreamNumber && item.wasteStreamNumber
        ? item.wasteStreamNumber === currentWasteStreamNumber
        : item.catalogItemId === currentCatalogItemId;
      
      if (isMatch) {
        setSelectedOption({
          value: String(item.id),
          label: item.wasteStreamNumber
            ? `${item.name} (${item.wasteStreamNumber})`
            : item.name,
          item,
        });
        return;
      }
    }

    // If no match found and items are loaded, clear selection
    if (filteredItems.length > 0) {
      setSelectedOption(null);
    }
  }, [field.catalogItemId, field.wasteStreamNumber, filteredItems, selectedOption]);

  const selectStyles: StylesConfig<CatalogItemOption, false, GroupBase<CatalogItemOption>> = {
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
        borderColor: disabled ? '#E3E8F3' : error ? '#F04438' : '#1E77F8',
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
  };

  return (
    <div className="flex flex-col items-start self-stretch gap-1 w-full">
      <div className="flex items-center self-stretch justify-between">
        <span className="text-caption-2">
          {title}
          <RequiredMarker required={required} />
        </span>
      </div>

      <Select<CatalogItemOption, false, GroupBase<CatalogItemOption>>
        key={instanceKey}
        value={selectedOption}
        options={options}
        onInputChange={(value) => setInputValue(value)}
        inputValue={inputValue}
        placeholder={placeholder}
        isDisabled={disabled}
        isLoading={isLoading}
        isClearable={true}
        classNamePrefix="react-select"
        noOptionsMessage={() => 'Geen items gevonden'}
        loadingMessage={() => 'Laden...'}
        id={testId || `catalog-item-select-${index}`}
        instanceId={instanceKey}
        menuPortalTarget={document.body}
        className={clsx(
          'w-full text-body-1',
          disabled
            ? 'text-color-text-disabled cursor-not-allowed'
            : 'text-color-text-secondary'
        )}
        styles={selectStyles}
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
            update(
              index,
              {
                ...field,
                catalogItemId: option.item.catalogItemId,
                wasteStreamNumber: option.item.wasteStreamNumber || undefined,
              },
              option.item
            );
          } else {
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
