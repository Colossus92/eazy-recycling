import { SelectFormField } from './SelectFormField';
import { useQuery } from '@tanstack/react-query';
import { materialService } from '@/api/services/materialService';
import {
  Control,
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import { useMemo } from 'react';

interface MaterialSelectFormFieldProps<TFieldValues extends FieldValues> {
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control?: Control<TFieldValues>;
  };
}

export const MaterialSelectFormField = ({
  formHook,
}: MaterialSelectFormFieldProps<any>) => {
  const { register } = formHook;

  // Fetch materials for dropdown
  const { data: materials = [] } = useQuery({
    queryKey: ['materials'],
    queryFn: () => materialService.getAll(),
  });

  const materialOptions = useMemo(
    () =>
      materials.map((material) => ({
        value: material.id.toString(),
        label: `${material.code} - ${material.name}`,
      })),
    [materials]
  );

  return (
    <SelectFormField
      title={'Materiaal'}
      placeholder={'Selecteer een materiaal'}
      options={materialOptions}
      formHook={{
        register,
        name: formHook.name,
        rules: formHook.rules,
        errors: formHook.errors,
        control: formHook.control,
      }}
      testId="material-select"
    />
  );
};
