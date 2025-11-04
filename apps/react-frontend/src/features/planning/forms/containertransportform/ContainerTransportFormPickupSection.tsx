import { useFormContext } from 'react-hook-form';
import { DateTimeInput } from '@/components/ui/form/DateTimeInput.tsx';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { AddressFormField } from '@/components/ui/form/addressformfield/AddressFormField';

export const ContainerTransportFormPickupSection = () => {
  const formContext = useFormContext<ContainerTransportFormValues>();

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <AddressFormField
        control={formContext.control}
        name="pickupLocation"
        label="Ophaal locatie"
        entity="transport"
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
