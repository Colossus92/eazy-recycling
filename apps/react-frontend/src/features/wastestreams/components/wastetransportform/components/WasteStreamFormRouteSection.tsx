import { useFormContext } from 'react-hook-form';
import { WasteStreamFormValues } from '@/features/wastestreams/components/wastetransportform/hooks/useWasteStreamFormHook.ts';
import { useQuery } from '@tanstack/react-query';
import { Company } from '@/api/services/companyService';
import { companyService } from '@/api/services/companyService.ts';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { ConsignorClassificationSelect } from '@/features/planning/forms/wastetransportform/ConsignorClassificationSelect';
import { AddressFormField } from '@/components/ui/form/addressformfield/AddressFormField';

export const WasteStreamFormRouteSection = () => {
  const formContext = useFormContext<WasteStreamFormValues>();
  const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
  });
  const companyOptions = companies.map((company) => ({
    value: company.id || '',
    label: company.name,
  }));
  const processorPartyOptions = companies.filter((company) => company.processorId).map((company) => ({
    value: company.processorId!!,
    label: company.name,
  }));


  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <SelectFormField
        title={'Afzender'}
        placeholder={'Selecteer een afzender'}
        options={companyOptions}
        testId="consignor-party-select"
        formHook={{
          register: formContext.register,
          name: 'consignorPartyId',
          rules: { required: 'Afzender is verplicht' },
          errors: formContext.formState.errors,
          control: formContext.control,
        }}
      />
      <ConsignorClassificationSelect
        formHook={{
          name: 'consignorClassification',
          rules: { required: 'Afzender type is verplicht' },
          errors: formContext.formState.errors,
          control: formContext.control,
        }}
      />
      <AddressFormField
        control={formContext.control}
        name="pickupLocation"
        label="Locatie van herkomst"
      />
      <SelectFormField
        title={'Verwerker (bestemming)'}
        placeholder={'Selecteer een verwerker'}
        options={processorPartyOptions}
        testId="processor-party-select"
        formHook={{
          register: formContext.register,
          name: 'processorPartyId',
          rules: { required: 'Verwerker is verplicht' },
          errors: formContext.formState.errors,
          control: formContext.control,
        }}
      />
    </div>
  );
};
