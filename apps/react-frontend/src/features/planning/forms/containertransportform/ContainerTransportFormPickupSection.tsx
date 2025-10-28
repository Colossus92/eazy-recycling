import { useMemo } from 'react';
import { useFormContext } from 'react-hook-form';
import {
  CompanyAddressInput,
  FieldNames,
} from '@/components/ui/form/CompanyAddressInput.tsx';
import { DateTimeInput } from '@/components/ui/form/DateTimeInput.tsx';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';

export const ContainerTransportFormPickupSection = () => {
  const formContext = useFormContext<ContainerTransportFormValues>();

  const pickupFieldNames: FieldNames<ContainerTransportFormValues> = useMemo(
    () => ({
      companyId: 'pickupCompanyId',
      branchId: 'pickupCompanyBranchId',
      street: 'pickupStreet',
      buildingNumber: 'pickupBuildingNumber',
      postalCode: 'pickupPostalCode',
      city: 'pickupCity',
    }),
    []
  );

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <CompanyAddressInput
        formContext={formContext}
        fieldNames={pickupFieldNames}
        title="Ophaal locatie"
        includeBranches
      />
      <DateTimeInput
        title={'Ophaal datum en tijd'}
        formHook={{
          register: formContext.register,
          name: 'pickupDateTime',
          rules: { required: 'Ophaal datum en tijd is verplicht' },
          errors: formContext.formState.errors,
        }}
      />
    </div>
  );
};
