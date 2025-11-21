import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField.tsx';
import { ContainerSelectFormField } from '@/components/ui/form/selectfield/ContainerSelectFormField';
import { DriverSelectFormField } from '@/components/ui/form/selectfield/DriverSelectFormField.tsx';
import { TruckSelectFormField } from '@/components/ui/form/selectfield/TruckSelectFormField.tsx';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { WasteTransportFormValues } from '@/features/planning/hooks/useWasteTransportForm.ts';
import { useFormContext } from 'react-hook-form';

export const TransportFormDetailsSection = () => {
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<ContainerTransportFormValues | WasteTransportFormValues>();

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <TruckSelectFormField
        formHook={{
          register: register,
          name: 'truckId',
          errors: errors,
          control: control,
        }}
      />
      <DriverSelectFormField
        formHook={{
          register: register,
          name: 'driverId',
          errors: errors,
          control: control,
        }}
      />
      <ContainerSelectFormField
        formHook={{
          register: register,
          name: 'containerId',
          errors: errors,
          control: control,
        }}
      />
      <TextAreaFormField
        title={'Opmerkingen'}
        placeholder={'Plaats opmerkingen'}
        testId="transport-notes"
        formHook={{
          register,
          name: 'note',
          rules: {},
          errors,
        }}
      />
    </div>
  );
};
