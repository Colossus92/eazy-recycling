import { Controller, Path, useFormContext } from 'react-hook-form';
import { Company } from '@/api/services/companyService';
import { companyService } from '@/api/services/companyService.ts';
import { TFieldValues } from './types';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  CompanyFormValue,
  LocationFormValue,
  ProjectLocationFormValue,
} from '@/types/forms/LocationFormValue';
import Select, { GroupBase } from 'react-select';
import { AsyncPaginate } from 'react-select-async-paginate';
import { useQueryClient } from '@tanstack/react-query';
import clsx from 'clsx';

const PAGE_SIZE = 20;

interface CompanyLocationInputProps {
  /**
   * Name of the parent location field (e.g., "pickupLocation")
   */
  name: Path<TFieldValues>;
  disabled?: boolean;
}

interface Option {
  value: string;
  label: string;
}

export const CompanyLocationInput = ({
  name,
  disabled = false,
}: CompanyLocationInputProps) => {
  const formContext = useFormContext<TFieldValues>();
  const { watch, setValue, control } = formContext;
  const queryClient = useQueryClient();

  // Cache of loaded companies for looking up branches
  const [companiesCache, setCompaniesCache] = useState<Map<string, Company>>(
    new Map()
  );
  // Track selected company option for display
  const [selectedCompanyOption, setSelectedCompanyOption] =
    useState<Option | null>(null);

  // Load companies with pagination for infinite scroll
  const loadCompanyOptions = useCallback(
    async (
      inputValue: string,
      _loadedOptions: readonly (Option | GroupBase<Option>)[],
      additional?: { page: number }
    ) => {
      const page = additional?.page ?? 0;
      const response = await companyService.getAll({
        query: inputValue || undefined,
        page,
        size: PAGE_SIZE,
        includeBranches: true,
      });

      // Update cache with loaded companies
      setCompaniesCache((prev) => {
        const newCache = new Map(prev);
        response.content.forEach((c) => newCache.set(c.id!, c));
        return newCache;
      });

      const options = response.content.map((company) => ({
        value: company.id!,
        label: company.name,
      }));

      const hasMore = page < response.totalPages - 1;

      return {
        options,
        hasMore,
        additional: {
          page: page + 1,
        },
      };
    },
    []
  );

  // Watch the entire location value
  const currentLocation = watch(name) as LocationFormValue;

  // Extract companyId and projectLocationId from current location
  const watchCompanyId =
    currentLocation?.type === 'company' ||
    currentLocation?.type === 'project_location'
      ? (currentLocation as CompanyFormValue | ProjectLocationFormValue)
          .companyId
      : '';
  const watchProjectLocationId =
    currentLocation?.type === 'project_location'
      ? (currentLocation as ProjectLocationFormValue).projectLocationId
      : '';

  // Find selected company from cache
  const selectedCompany = useMemo(() => {
    return companiesCache.get(watchCompanyId);
  }, [companiesCache, watchCompanyId]);

  // Branch options for selected company (using term "projectLocation")
  const projectLocationOptions = useMemo(() => {
    if (!selectedCompany || !selectedCompany.branches) {
      return [];
    }

    const options = [
      {
        value: '', // Empty value for headquarter
        label: 'Hoofdkantoor',
      },
      ...selectedCompany.branches.map((branch) => ({
        value: branch.id,
        label: `${branch.address.street} ${branch.address.houseNumber}${branch.address.houseNumberAddition ?? ''}, ${branch.address.city}`,
      })),
    ];

    return options;
  }, [selectedCompany]);

  const hasCompanySelected =
    watchCompanyId !== undefined &&
    watchCompanyId !== null &&
    watchCompanyId.length > 0;
  const hasProjectLocationSelected =
    watchProjectLocationId !== undefined &&
    watchProjectLocationId !== null &&
    String(watchProjectLocationId).length > 0;
  const showProjectLocationSelector =
    hasCompanySelected &&
    selectedCompany?.branches &&
    selectedCompany.branches.length > 0;

  // Track previous company ID to detect actual changes (not initial loads)
  const previousCompanyIdRef = useRef<string | undefined>(undefined);
  const isInitialRender = useRef(true);

  // Reset project location selection only when company actually changes (not on initial load)
  useEffect(() => {
    if (hasCompanySelected) {
      // Skip reset on initial render to preserve existing form data
      if (isInitialRender.current) {
        isInitialRender.current = false;
        previousCompanyIdRef.current = watchCompanyId;
        return;
      }

      // Only reset if company actually changed from a previous value
      if (
        previousCompanyIdRef.current !== undefined &&
        previousCompanyIdRef.current !== watchCompanyId
      ) {
        // Reset to company type (headquarter) when company changes
        const newLocation: CompanyFormValue = {
          type: 'company',
          companyId: watchCompanyId,
          companyName: selectedCompany?.name,
        };
        setValue(name, newLocation as TFieldValues[typeof name]);
      }

      previousCompanyIdRef.current = watchCompanyId;
    }
  }, [watchCompanyId, hasCompanySelected, selectedCompany, name, setValue]);

  // Auto-select headquarters when company with branches is selected but no project location is specified
  useEffect(() => {
    if (
      hasCompanySelected &&
      selectedCompany?.branches &&
      selectedCompany.branches.length > 0
    ) {
      // If no project location is selected (undefined, null, or empty), default to headquarters (company type)
      if (!hasProjectLocationSelected && currentLocation?.type !== 'company') {
        const newLocation: CompanyFormValue = {
          type: 'company',
          companyId: watchCompanyId,
          companyName: selectedCompany.name,
        };
        setValue(name, newLocation as TFieldValues[typeof name]);
      }
    }
  }, [
    hasCompanySelected,
    selectedCompany,
    hasProjectLocationSelected,
    currentLocation,
    watchCompanyId,
    name,
    setValue,
  ]);

  // Handle company selection change
  const handleCompanyChange = (
    companyId: string | null,
    companyName: string | null
  ) => {
    if (!companyId) {
      setSelectedCompanyOption(null);
      return;
    }
    const company = companiesCache.get(companyId);
    const newLocation: CompanyFormValue = {
      type: 'company',
      companyId: companyId,
      companyName: companyName || company?.name,
    };
    setValue(name, newLocation as TFieldValues[typeof name]);
  };

  // Load initial company option if form has a value (with caching)
  useEffect(() => {
    const loadInitialCompany = async () => {
      if (watchCompanyId && !selectedCompanyOption) {
        try {
          // Use React Query's fetchQuery for caching - returns cached data if available
          const company = await queryClient.fetchQuery({
            queryKey: ['company', watchCompanyId],
            queryFn: () => companyService.getById(watchCompanyId),
            staleTime: 5 * 60 * 1000, // Cache for 5 minutes
          });
          // Update cache with the company
          setCompaniesCache((prev) => {
            const newCache = new Map(prev);
            newCache.set(company.id!, company);
            return newCache;
          });
          setSelectedCompanyOption({
            value: company.id!,
            label: company.name,
          });
        } catch {
          // Company not found, ignore
        }
      }
    };
    loadInitialCompany();
  }, [watchCompanyId, selectedCompanyOption, queryClient]);

  // Handle project location selection change
  const handleProjectLocationChange = (projectLocationId: string | null) => {
    if (!selectedCompany) return;

    // Empty string or null means headquarter (company type)
    if (!projectLocationId || projectLocationId === '') {
      const newLocation: CompanyFormValue = {
        type: 'company',
        companyId: watchCompanyId,
        companyName: selectedCompany.name,
      };
      setValue(name, newLocation as TFieldValues[typeof name]);
      return;
    }

    // Find the selected branch
    const selectedBranch = selectedCompany.branches?.find(
      (branch) => branch.id === projectLocationId
    );
    if (selectedBranch && selectedBranch.address) {
      const newLocation: ProjectLocationFormValue = {
        type: 'project_location',
        projectLocationId: projectLocationId,
        companyId: watchCompanyId,
        companyName: selectedCompany.name,
        streetName: selectedBranch.address.street || '',
        buildingNumber: selectedBranch.address.houseNumber || '',
        buildingNumberAddition:
          selectedBranch.address.houseNumberAddition || '',
        postalCode: selectedBranch.address.postalCode || '',
        city: selectedBranch.address.city || '',
        country: selectedBranch.address.country || 'Nederland',
      };
      setValue(name, newLocation as TFieldValues[typeof name]);
    }
  };

  const selectStyles = {
    control: (base: any, state: any) => ({
      ...base,
      minHeight: '40px',
      height: '40px',
      borderRadius: '8px',
      borderWidth: '1px',
      borderStyle: 'solid',
      borderColor: disabled
        ? '#E3E8F3'
        : state.isFocused
          ? '#1E77F8'
          : '#E3E8F3',
      backgroundColor: '#FFFFFF',
      cursor: disabled ? 'not-allowed' : 'default',
      boxShadow: 'none',
      '&:hover': disabled
        ? {}
        : {
            borderColor: '#1E77F8',
            backgroundColor: '#F3F8FF',
          },
    }),
    menuPortal: (base: any) => ({
      ...base,
      zIndex: 9999,
    }),
    input: (base: any) => ({
      ...base,
      'input:focus': {
        boxShadow: 'none',
      },
    }),
  };

  const selectClassNames = {
    placeholder: () =>
      clsx(
        disabled ? 'text-color-text-disabled' : 'text-color-text-disabled',
        'italic'
      ),
    option: ({ isSelected, isFocused }: any) =>
      clsx(
        'cursor-pointer',
        isSelected
          ? 'bg-color-primary text-color-text-secondary'
          : isFocused
            ? 'bg-color-surface-secondary text-color-text-primary'
            : 'bg-color-surface-primary text-color-text-primary'
      ),
    singleValue: () =>
      disabled ? 'text-color-text-disabled' : 'text-color-text-primary',
    valueContainer: () => 'px-3 py-2',
  };

  return (
    <div className="w-full flex flex-col gap-3">
      <div className="flex flex-col items-start self-stretch gap-1 w-full">
        <div className="flex items-center self-stretch justify-between">
          <span className="text-caption-2">Bedrijf</span>
        </div>
        <Controller
          control={control}
          name={`${String(name)}.companyId` as Path<TFieldValues>}
          rules={{ required: 'Bedrijf is verplicht' }}
          render={({ field, fieldState }) => (
            <>
              <AsyncPaginate
                {...field}
                loadOptions={loadCompanyOptions}
                additional={{ page: 0 }}
                debounceTimeout={300}
                placeholder="Selecteer een bedrijf"
                isClearable={true}
                isDisabled={disabled}
                classNamePrefix="react-select"
                noOptionsMessage={() => 'Geen opties beschikbaar'}
                loadingMessage={() => 'Laden...'}
                id="company-location-select"
                data-testid="company-location-select"
                menuPortalTarget={document.body}
                className="w-full text-body-1 text-color-text-secondary"
                styles={selectStyles}
                classNames={selectClassNames}
                value={selectedCompanyOption}
                onChange={(newOption: Option | null) => {
                  setSelectedCompanyOption(newOption);
                  field.onChange(newOption ? newOption.value : null);
                  handleCompanyChange(
                    newOption?.value || null,
                    newOption?.label || null
                  );
                }}
              />
              {fieldState.error && (
                <span className="text-caption-1 text-color-status-error-dark">
                  {fieldState.error.message}
                </span>
              )}
            </>
          )}
        />
      </div>

      {/* Project Location selector - only show if company has branches */}
      {showProjectLocationSelector && (
        <div className="flex flex-col items-start self-stretch gap-1 w-full">
          <div className="flex items-center self-stretch justify-between">
            <span className="text-caption-2">Kies projectlocatie</span>
          </div>
          <Controller
            control={control}
            name={`${String(name)}.projectLocationId` as Path<TFieldValues>}
            render={({ field }) => (
              <Select
                {...field}
                options={projectLocationOptions}
                placeholder="Selecteer een projectlocatie"
                isClearable={true}
                isDisabled={disabled}
                classNamePrefix="react-select"
                noOptionsMessage={() => 'Geen opties beschikbaar'}
                id="project-location-select"
                data-testid="project-location-select"
                menuPortalTarget={document.body}
                className="w-full text-body-1 text-color-text-secondary"
                styles={selectStyles}
                classNames={selectClassNames}
                value={
                  projectLocationOptions.find(
                    (option) =>
                      option.value ===
                      (hasProjectLocationSelected ? watchProjectLocationId : '')
                  ) || null
                }
                onChange={(selectedOption) => {
                  const value = selectedOption
                    ? (selectedOption as Option).value
                    : null;
                  field.onChange(value);
                  handleProjectLocationChange(value);
                }}
              />
            )}
          />
        </div>
      )}

      {hasCompanySelected && (
        <div className="flex items-center">
          <span className="text-body-2 whitespace-normal break-words">
            {hasProjectLocationSelected
              ? 'Geselecteerde projectlocatie wordt gebruikt als ophaallocatie'
              : 'Hoofdkantoor wordt gebruikt als ophaallocatie'}
          </span>
        </div>
      )}
    </div>
  );
};
