import { useQuery } from '@tanstack/react-query';
import {
  Control,
  FieldErrors,
  FieldValues,
  Path,
  UseFormRegister,
} from 'react-hook-form';
import {
  Option,
  SelectFormField,
} from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { Truck } from '@/types/api.ts';
import { truckService } from '@/api/truckService.ts';

interface TruckSelectFormFieldProps<TFieldValues extends FieldValues> {
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    errors: FieldErrors;
    control: Control<TFieldValues>;
  };
}

export const TruckSelectFormField = <T extends FieldValues>({
  formHook,
}: TruckSelectFormFieldProps<T>) => {
  const { data: trucks = [] } = useQuery<Truck[]>({
    queryKey: ['trucks'],
    queryFn: () => truckService.list(),
  });

  const truckOptions: Option[] = trucks.map((truck) => ({
    value: truck.licensePlate,
    label: `${truck.brand} ${truck.model} ${truck.licensePlate}`,
  }));

  return (
    <SelectFormField
      title={'Vrachtwagen (optioneel)'}
      placeholder={'Selecteer een vrachtwagen'}
      options={truckOptions}
      formHook={{
        register: formHook.register,
        name: formHook.name,
        errors: formHook.errors,
        control: formHook.control,
      }}
    />
  );
};
