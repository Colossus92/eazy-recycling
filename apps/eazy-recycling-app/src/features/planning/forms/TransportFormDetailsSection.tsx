import { useFormContext } from 'react-hook-form';
import { useQuery } from '@tanstack/react-query';
import {
  Option,
  SelectFormField,
} from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { WasteContainer } from '@/types/api.ts';
import { containerService } from '@/api/containerService.ts';
import { DriverSelectFormField } from '@/components/ui/form/selectfield/DriverSelectFormField.tsx';
import { TruckSelectFormField } from '@/components/ui/form/selectfield/TruckSelectFormField.tsx';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { WasteTransportFormValues } from '@/features/planning/hooks/useWasteTransportForm.ts';
import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField.tsx';

export const TransportFormDetailsSection = () => {
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<ContainerTransportFormValues | WasteTransportFormValues>();
  const { data: wastecontainers = [] } = useQuery<WasteContainer[]>({
    queryKey: ['containers'],
    queryFn: () => containerService.list(),
  });

  const containerOptions: Option[] = wastecontainers.map((container) => ({
    value: container.uuid,
    label: container.id,
  }));
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
      <SelectFormField
        title={'Container (optioneel)'}
        placeholder={'Selecteer een container'}
        options={containerOptions}
        formHook={{
          register: register,
          name: 'containerId',
          errors: errors,
          control: control,
        }}
      />
      <TextAreaFormField
        title={'Opmerkingen (optioneel)'}
        placeholder={'Plaats opmerkingen'}
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
