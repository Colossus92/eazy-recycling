import {
  Control,
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import {
  Option,
  SelectFormField,
} from '@/components/ui/form/selectfield/SelectFormField.tsx';

interface ContainerOperationSelectFormFieldProps<TFieldValues extends FieldValues> {
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    errors: FieldErrors;
    control: Control<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
  };
  disabled?: boolean;
}

const CONTAINER_OPERATION_OPTIONS: Option[] = [
  {
    value: 'EMPTY',
    label: 'Container legen',
  },
  {
    value: 'EXCHANGE',
    label: 'Container wisselen',
  },
  {
    value: 'PICKUP',
    label: 'Afvoeren',
  },
];

export const ContainerOperationSelectFormField = <T extends FieldValues>({
  formHook,
  disabled = false,
}: ContainerOperationSelectFormFieldProps<T>) => {
  return (
    <SelectFormField
      title={'Type transport'}
      placeholder={'Selecteer het type transport'}
      options={CONTAINER_OPERATION_OPTIONS}
      testId='container-operation-select'
      disabled={disabled}
      formHook={{
        register: formHook.register,
        name: formHook.name,
        errors: formHook.errors,
        control: formHook.control,
        rules: formHook.rules,
      }}
    />
  );
};
