import { useFormContext } from 'react-hook-form';
import { DateTimeInput } from '@/components/ui/form/DateTimeInput.tsx';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { AddressFormField } from '@/components/ui/form/addressformfield/AddressFormField';

export const ContainerTransportFormDeliverySection = () => {
  const formContext = useFormContext<ContainerTransportFormValues>();

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <AddressFormField
        control={formContext.control}
        name="deliveryLocation"
        label="Afleverlocatie"
        entity="transport"
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
