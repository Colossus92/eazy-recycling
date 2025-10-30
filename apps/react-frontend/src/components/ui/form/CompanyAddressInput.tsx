import { FieldValues, Path, UseFormReturn } from 'react-hook-form';
import { useEffect, useMemo, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { PostalCodeFormField } from './PostalCodeFormField';
import { companyService, Company } from '@/api/services/companyService.ts';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';

export interface FieldNames<T extends FieldValues> {
  companyId: Path<T>;
  branchId?: Path<T>;
  street: Path<T>;
  buildingNumber: Path<T>;
  postalCode: Path<T>;
  city: Path<T>;
}

interface CompanyAddressInputProps<T extends FieldValues> {
  company?: Company;
  formContext: UseFormReturn<T>;
  fieldNames: FieldNames<T>;
  title?: string;
  includeBranches?: boolean;
  testId?: string;
  required?: boolean;
}

export const CompanyAddressInput = <T extends FieldValues>({
  formContext,
  fieldNames,
  title = 'Huidige locatie',
  includeBranches = false,
  testId,
  required = true,
}: CompanyAddressInputProps<T>) => {
  const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies', includeBranches],
    queryFn: () => companyService.getAll(includeBranches),
    refetchOnMount: false,
  });
  const {
    register,
    formState: { errors },
    control,
    watch,
    setValue,
  } = formContext;

  const companyOptions = useMemo(() => {
    return companies.map((company) => ({
      value: company.id || '',
      label: company.name,
    }));
  }, [companies]);

  // Watch for company selection
  const watchCompanyId = watch(fieldNames.companyId);
  const watchBranchId = fieldNames.branchId
    ? watch(fieldNames.branchId)
    : undefined;

  // Find selected company and its branches
  const selectedCompany = useMemo(() => {
    return companies.find((c) => c.id === watchCompanyId);
  }, [companies, watchCompanyId]);

  // Branch options for selected company
  const branchOptions = useMemo(() => {
    if (!selectedCompany || !selectedCompany.branches || !includeBranches) {
      return [];
    }

    const options = [
      {
        value: '', // Empty value for headquarter
        label: 'Hoofdkantoor',
      },
      ...selectedCompany.branches.map((branch) => ({
        value: branch.id || '',
        label: `${branch.address.streetName} ${branch.address.buildingNumber}, ${branch.address.city}`,
      })),
    ];

    return options;
  }, [selectedCompany, includeBranches]);

  const hasCompanySelected =
    watchCompanyId !== undefined &&
    watchCompanyId !== null &&
    watchCompanyId.length > 0;
  const hasBranchSelected =
    watchBranchId !== undefined &&
    watchBranchId !== null &&
    String(watchBranchId).length > 0;
  const showBranchSelector =
    hasCompanySelected &&
    selectedCompany?.branches &&
    selectedCompany.branches.length > 0 &&
    includeBranches;

  // Track previous company ID to detect actual changes (not initial loads)
  const previousCompanyIdRef = useRef<string | undefined>(undefined);
  const isInitialRender = useRef(true);

  // Reset branch selection only when company actually changes (not on initial load)
  useEffect(() => {
    if (fieldNames.branchId && hasCompanySelected) {
      // Skip reset on initial render to preserve existing form data
      if (isInitialRender.current) {
        isInitialRender.current = false;
        previousCompanyIdRef.current = watchCompanyId;
        return;
      }

      // Only reset if company actually changed from a previous value
      if (
        previousCompanyIdRef.current !== undefined &&
        previousCompanyIdRef.current !== watchCompanyId &&
        fieldNames.branchId
      ) {
        setValue(fieldNames.branchId, '' as T[typeof fieldNames.branchId]);
      }

      previousCompanyIdRef.current = watchCompanyId;
    }
  }, [
    watchCompanyId,
    fieldNames.branchId,
    setValue,
    hasCompanySelected,
  ]);

  // Auto-select headquarters when company with branches is selected but no branch is specified
  useEffect(() => {
    if (
      fieldNames.branchId &&
      hasCompanySelected &&
      selectedCompany?.branches &&
      selectedCompany.branches.length > 0 &&
      includeBranches
    ) {
      // If no branch is selected (undefined, null, or empty), default to headquarters (empty string)
      if (!hasBranchSelected && fieldNames.branchId) {
        setValue(fieldNames.branchId, '' as T[typeof fieldNames.branchId]);
      }
    }
  }, [
    hasCompanySelected,
    selectedCompany,
    hasBranchSelected,
    fieldNames.branchId,
    setValue,
    includeBranches,
  ]);

  // Handle address loading based on company/branch selection
  useEffect(() => {
    if (!hasCompanySelected) return;

    const mapToFormValue = <T,>(value: string): T => value as unknown as T;

    // If branch is selected, use branch address
    if (hasBranchSelected && selectedCompany?.branches) {
      const selectedBranch = selectedCompany.branches.find(
        (branch) => branch.id === watchBranchId
      );
      if (selectedBranch && selectedBranch.address && selectedBranch.address.streetName && selectedBranch.address.buildingNumber && selectedBranch.address.postalCode && selectedBranch.address.city) {
        setValue(
          fieldNames.street,
          mapToFormValue(selectedBranch.address.streetName)
        );
        setValue(
          fieldNames.buildingNumber,
          mapToFormValue(selectedBranch.address.buildingNumber)
        );
        setValue(
          fieldNames.postalCode,
          mapToFormValue(selectedBranch.address.postalCode)
        );
        setValue(fieldNames.city, mapToFormValue(selectedBranch.address.city));
        return;
      }
    }

    // Otherwise, use company address (headquarter)
    if (selectedCompany && selectedCompany.address && selectedCompany.address.streetName && selectedCompany.address.buildingNumber && selectedCompany.address.postalCode && selectedCompany.address.city) {
      setValue(
        fieldNames.street,
        mapToFormValue(selectedCompany.address.streetName)
      );
      setValue(
        fieldNames.buildingNumber,
        mapToFormValue(selectedCompany.address.buildingNumber)
      );
      setValue(
        fieldNames.postalCode,
        mapToFormValue(selectedCompany.address.postalCode)
      );
      setValue(fieldNames.city, mapToFormValue(selectedCompany.address.city));
    }
  }, [
    watchCompanyId,
    watchBranchId,
    hasCompanySelected,
    hasBranchSelected,
    selectedCompany,
    setValue,
    fieldNames,
  ]);

  return (
    <div className="flex flex-col items-start self-stretch gap-4 p-4 bg-color-surface-secondary rounded-radius-md">
      <span className="text-subtitle-1">{title}</span>

      <div className="flex-grow-0 flex flex-col items-start self-stretch gap-4">
        <SelectFormField
          title={'Kies bedrijf (optioneel)'}
          placeholder={'Selecteer een bedrijf of vul zelf een adres in'}
          options={companyOptions}
          testId={testId}
          formHook={{
            register,
            name: fieldNames.companyId,
            rules: {},
            errors,
            control,
          }}
        />

        {/* Branch selector - only show if company has branches and includeBranches is true */}
        {showBranchSelector && fieldNames.branchId && (
          <SelectFormField
            title={'Kies vestiging'}
            placeholder={'Selecteer een vestiging'}
            options={branchOptions}
            formHook={{
              register,
              name: fieldNames.branchId,
              rules: {},
              errors,
              control,
            }}
          />
        )}

        {hasCompanySelected && (
          <div className="flex items-center max-w-96">
            <span className="text-body-2 whitespace-normal break-words">
              Adresgegevens worden automatisch ingevuld op basis van het
              geselecteerde {hasBranchSelected ? 'vestiging' : 'bedrijf'}
            </span>
          </div>
        )}
      </div>

      <div className="flex items-start self-stretch gap-4 flex-grow">
        <TextFormField
          title={'Straat'}
          placeholder={'Vul straatnaam in'}
          formHook={{
            register,
            name: fieldNames.street,
            rules: !hasCompanySelected && required
              ? { required: 'Straat is verplicht' }
              : {},
            errors,
          }}
          disabled={hasCompanySelected}
        />

        <TextFormField
          title={'Nummer'}
          placeholder={'Vul huisnummer in'}
          formHook={{
            register,
            name: fieldNames.buildingNumber,
            rules: !hasCompanySelected && required
              ? { required: 'Huisnummer is verplicht' }
              : {},
            errors,
          }}
          disabled={hasCompanySelected}
        />
      </div>

      <div className="flex items-start self-stretch gap-4">
        <PostalCodeFormField
          register={register}
          setValue={setValue}
          errors={errors}
          name={fieldNames.postalCode}
          value={watch(fieldNames.postalCode)}
          required={!hasCompanySelected && required}
          disabled={hasCompanySelected}
        />

        <TextFormField
          title={'Plaats'}
          placeholder={'Vul Plaats in'}
          formHook={{
            register,
            name: fieldNames.city,
            rules: !hasCompanySelected && required
              ? { required: 'Plaats is verplicht' }
              : {},
            errors,
          }}
          disabled={hasCompanySelected}
        />
      </div>
    </div>
  );
};
