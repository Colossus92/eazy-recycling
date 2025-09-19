import { useFormContext } from 'react-hook-form';
import { useQuery } from '@tanstack/react-query';
import {
  CompanyAddressInput,
  FieldNames,
} from '@/components/ui/form/CompanyAddressInput.tsx';
import { DateTimeInput } from '@/components/ui/form/DateTimeInput.tsx';
import { WasteTransportFormValues } from '@/features/planning/hooks/useWasteTransportForm.ts';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { Company } from '@/types/api.ts';
import { companyService } from '@/api/companyService.ts.tsx';

export const WasteTransportFormDeliverySection = () => {
  const formContext = useFormContext<WasteTransportFormValues>();
  const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies'],
    queryFn: () => companyService.list(),
  });

  const companyOptions = companies.map((company) => ({
    value: company.id || '',
    label: company.name,
  }));
  const deliveryFieldnames: FieldNames<WasteTransportFormValues> = {
    companyId: 'deliveryCompanyId',
    branchId: 'deliveryCompanyBranchId',
    street: 'deliveryStreet',
    buildingNumber: 'deliveryBuildingNumber',
    postalCode: 'deliveryPostalCode',
    city: 'deliveryCity',
  };

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <SelectFormField
        title={'Ontvanger'}
        placeholder={'Selecteer een ontvanger'}
        options={companyOptions}
        testId='consignee-party-select'
        formHook={{
          register: formContext.register,
          name: 'consigneePartyId',
          rules: { required: 'Ontvanger is verplicht' },
          errors: formContext.formState.errors,
          control: formContext.control,
        }}
      />
      <CompanyAddressInput
        formContext={formContext}
        fieldNames={deliveryFieldnames}
        title="Locatie van bestemming"
        includeBranches
        testId='delivery-company-address'
      />

      <DateTimeInput
        title={'Aflever datum en tijd'}
        testId='delivery-date-time'
        formHook={{
          register: formContext.register,
          name: 'deliveryDateTime',
          errors: formContext.formState.errors,
        }}
      />
    </div>
  );
};
