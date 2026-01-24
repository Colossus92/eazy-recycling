import { useFormContext } from 'react-hook-form';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { AddressFormField } from '@/components/ui/form/addressformfield/AddressFormField';
import { TransportTimingFormField } from '@/components/ui/form/TransportTimingFormField';

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

      <TransportTimingFormField
        name="deliveryTiming"
        control={formContext.control}
        label="Aflever datum en tijd"
        required={false}
        testId="delivery-timing"
      />
    </div>
  );
};
