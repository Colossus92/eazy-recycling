import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { useFormContext } from 'react-hook-form';

export const ContainerTransportMainSection = () => {
  const { register } = useFormContext<ContainerTransportFormValues>();
  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <CompanySelectFormField
        title={'Opdrachtgever'}
        placeholder={'Selecteer een opdrachtgever'}
        name={'consignorPartyId'}
        rules={{ required: 'Opdrachtgever is verplicht' }}
      />
      <CompanySelectFormField
        title={'Vervoerder'}
        placeholder={'Selecteer een vervoerder'}
        name={'carrierPartyId'}
        rules={{ required: 'Vervoerder is verplicht' }}
      />
      <input
        type="hidden"
        {...register('containerOperation')}
        defaultValue="DELIVERY"
      />
    </div>
  );
};
