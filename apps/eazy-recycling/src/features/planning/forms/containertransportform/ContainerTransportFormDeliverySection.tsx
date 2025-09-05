import { useFormContext } from 'react-hook-form';
import {
  CompanyAddressInput,
  FieldNames,
} from '@/components/ui/form/CompanyAddressInput.tsx';
import { DateTimeInput } from '@/components/ui/form/DateTimeInput.tsx';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';

export const ContainerTransportFormDeliverySection = () => {
  const formContext = useFormContext<ContainerTransportFormValues>();
  const deliveryFieldnames: FieldNames<ContainerTransportFormValues> = {
    companyId: 'deliveryCompanyId',
    branchId: 'deliveryCompanyBranchId',
    street: 'deliveryStreet',
    buildingNumber: 'deliveryBuildingNumber',
    postalCode: 'deliveryPostalCode',
    city: 'deliveryCity',
  };

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <CompanyAddressInput
        formContext={formContext}
        fieldNames={deliveryFieldnames}
        title="Afleverlocatie"
        includeBranches
      />

      <DateTimeInput
        title={'Aflever datum en tijd'}
        formHook={{
          register: formContext.register,
          name: 'deliveryDateTime',
          errors: formContext.formState.errors,
        }}
      />
    </div>
  );
};
