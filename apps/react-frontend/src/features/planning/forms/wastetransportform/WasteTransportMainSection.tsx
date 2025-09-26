import { useFormContext } from 'react-hook-form';
import { useQuery } from '@tanstack/react-query';
import { ConsignorClassificationSelect } from './ConsignorClassificationSelect';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { companyService, Company } from '@/api/services/companyService.ts';
import { WasteTransportFormValues } from '@/features/planning/hooks/useWasteTransportForm.ts';

export const WasteTransportMainSection = () => {
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<WasteTransportFormValues>();
  const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
  });

  const companyOptions = companies.map((company) => ({
    value: company.id || '',
    label: company.name,
  }));

  const containerOperationOptions = [
    {
      value: 'EMPTY',
      label: 'Container legen',
    },
    {
      value: 'EXCHANGE',
      label: 'Container wisselen',
    },
    {
      value: 'PICKUP',
      label: 'Afvoeren',
    },
  ];

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <SelectFormField
        title={'Afzender'}
        placeholder={'Selecteer een afzender'}
        options={companyOptions}
        testId="consignor-party-select"
        formHook={{
          register: register,
          name: 'consignorPartyId',
          rules: { required: 'Afzender is verplicht' },
          errors: errors,
          control: control,
        }}
      />
      <ConsignorClassificationSelect
        formHook={{
          register: register,
          name: 'consignorClassification',
          rules: { required: 'Afzender type is verplicht' },
          errors: errors,
          control: control,
        }}
      />
      <SelectFormField
        title={'Vervoerder'}
        placeholder={'Selecteer een vervoerder'}
        options={companyOptions}
        testId="carrier-party-select"
        formHook={{
          register: register,
          name: 'carrierPartyId',
          rules: { required: 'Vervoerder is verplicht' },
          errors: errors,
          control: control,
        }}
      />
      <SelectFormField
        title={'Type transport'}
        placeholder={'Selecteer het type transport'}
        options={containerOperationOptions}
        testId="container-operation-select"
        formHook={{
          register: register,
          name: 'containerOperation',
          rules: { required: 'Type transport is verplicht' },
          errors: errors,
          control: control,
        }}
      />
    </div>
  );
};
