import { SelectFormField } from './SelectFormField';
import { useQuery } from '@tanstack/react-query';
import { catalogService } from '@/api/services/catalogService';
import {
  Control,
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import { useMemo } from 'react';

interface CatalogItemSelectFormFieldProps<TFieldValues extends FieldValues> {
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control?: Control<TFieldValues>;
  };
  disabled?: boolean;
}

export const CatalogItemSelectFormField = ({
  formHook,
  disabled = false,
}: CatalogItemSelectFormFieldProps<any>) => {
  const { register } = formHook;

  // Fetch catalog items for dropdown
  const { data: catalogItems = [] } = useQuery({
    queryKey: ['catalogItems'],
    queryFn: () => catalogService.search(),
  });

  const catalogItemOptions = useMemo(
    () =>
      catalogItems.map((item) => ({
        value: item.id.toString(),
        label: item.wasteStreamNumber
          ? `${item.name} (${item.wasteStreamNumber})`
          : item.name,
      })),
    [catalogItems]
  );

  return (
    <SelectFormField
      title={'Catalogus item'}
      placeholder={'Selecteer een catalogus item'}
      options={catalogItemOptions}
      formHook={{
        register,
        name: formHook.name,
        rules: formHook.rules,
        errors: formHook.errors,
        control: formHook.control,
      }}
      testId="catalog-item-select"
      disabled={disabled}
    />
  );
};
