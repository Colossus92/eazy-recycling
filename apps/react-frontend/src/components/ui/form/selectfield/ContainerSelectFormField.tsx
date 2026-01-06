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
import { WasteContainerView } from '@/api/client';
import { containerService } from '@/api/services/containerService';

interface ContainerSelectFormFieldProps<TFieldValues extends FieldValues> {
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    errors: FieldErrors;
    control: Control<TFieldValues>;
  };
  disabled?: boolean;
  required?: boolean;
}

export const ContainerSelectFormField = <T extends FieldValues>({
  formHook,
  disabled = false,
  required = false,
}: ContainerSelectFormFieldProps<T>) => {
  const { data: containers = [] } = useQuery<WasteContainerView[]>({
    queryKey: ['containers'],
    queryFn: () => containerService.getAll(),
  });

  const containerOptions: Option[] = containers.map((container) => ({
    value: container.id,
    label: container.id,
  }));

  return (
    <SelectFormField
      title={'Container'}
      placeholder={'Selecteer een container'}
      options={containerOptions}
      testId='container-select'
      disabled={disabled}
      formHook={{
        register: formHook.register,
        name: formHook.name,
        errors: formHook.errors,
        control: formHook.control,
        rules: {
          required: required ? 'Container is verplicht' : undefined,
        },
      }}
    />
  );
};
