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
import { User } from '@/types/api.ts';
import { userService } from '@/api/userService.ts';

interface DriverSelectFormFieldProps<TFieldValues extends FieldValues> {
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    errors: FieldErrors;
    control: Control<TFieldValues>;
  };
}

export const DriverSelectFormField = <T extends FieldValues>({
  formHook,
}: DriverSelectFormFieldProps<T>) => {
  const { data: drivers = [] } = useQuery<User[]>({
    queryKey: ['users'],
    queryFn: () => userService.listDrivers(),
  });
  const driverOptions: Option[] = drivers.map((driver) => ({
    value: driver.id,
    label: driver.firstName + ' ' + driver.lastName,
  }));

  return (
    <SelectFormField
      title={'Chauffeur (optioneel)'}
      placeholder={'Selecteer een chauffeur'}
      options={driverOptions}
      testId='driver-select'
      formHook={{
        register: formHook.register,
        name: formHook.name,
        errors: formHook.errors,
        control: formHook.control,
      }}
    />
  );
};
