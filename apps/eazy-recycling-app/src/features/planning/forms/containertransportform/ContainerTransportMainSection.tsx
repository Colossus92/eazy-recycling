import { useFormContext } from 'react-hook-form';
import { useQuery } from '@tanstack/react-query';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { Company } from '@/types/api.ts';
import { companyService } from '@/api/companyService.ts.tsx';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';

export const ContainerTransportMainSection = () => {
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<ContainerTransportFormValues>();
  const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies'],
    queryFn: () => companyService.list(),
  });

  const companyOptions = companies.map((company) => ({
    value: company.id || '',
    label: company.name,
  }));

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <SelectFormField
        title={'Opdrachtgever'}
        placeholder={'Selecteer een opdrachtgever'}
        options={companyOptions}
        formHook={{
          register: register,
          name: 'consignorPartyId',
          rules: { required: 'Opdrachtgever is verplicht' },
          errors: errors,
          control: control,
        }}
      />
      <SelectFormField
        title={'Vervoerder'}
        placeholder={'Selecteer een vervoerder'}
        options={companyOptions}
        formHook={{
          register: register,
          name: 'carrierPartyId',
          rules: { required: 'Vervoerder is verplicht' },
          errors: errors,
          control: control,
        }}
      />
      <input
        type="hidden"
        {...register('containerOperation')}
        value="DELIVERY"
      />
    </div>
  );
};
