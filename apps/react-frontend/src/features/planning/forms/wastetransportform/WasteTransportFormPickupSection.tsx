import { useFormContext } from 'react-hook-form';
import {
  CompanyAddressInput,
  FieldNames,
} from '@/components/ui/form/CompanyAddressInput.tsx';
import { DateTimeInput } from '@/components/ui/form/DateTimeInput.tsx';
import { WasteTransportFormValues } from '@/features/planning/hooks/useWasteTransportForm.ts';

export const WasteTransportFormPickupSection = () => {
  const formContext = useFormContext<WasteTransportFormValues>();
  const pickupFieldNames: FieldNames<WasteTransportFormValues> = {
    companyId: 'pickupCompanyId',
    branchId: 'pickupCompanyBranchId',
    street: 'pickupStreet',
    buildingNumber: 'pickupBuildingNumber',
    postalCode: 'pickupPostalCode',
    city: 'pickupCity',
  };

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <CompanyAddressInput
        formContext={formContext}
        fieldNames={pickupFieldNames}
        title="Locatie van herkomst"
        testId="pickup-company-address"
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
        testId="pickup-date-time"
      />
    </div>
  );
};
