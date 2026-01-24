import { useFormContext } from 'react-hook-form';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { AddressFormField } from '@/components/ui/form/addressformfield/AddressFormField';
import { TransportTimingFormField } from '@/components/ui/form/TransportTimingFormField';

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
      <TransportTimingFormField
        name="pickupTiming"
        control={formContext.control}
        label="Ophaal datum en tijd"
        required={false}
        testId="pickup-timing"
      />
    </div>
  );
};
