import { SelectFormField } from './SelectFormField';

import { useQuery } from '@tanstack/react-query';
import { productCategoryService } from '@/api/services/productCategoryService';
import {
  Control,
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from 'react-hook-form';
import { useMemo } from 'react';

interface ProductCategorySelectFormFieldProps<TFieldValues extends FieldValues> {
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control?: Control<TFieldValues>;
  };
}

export const ProductCategorySelectFormField = ({
  formHook,
}: ProductCategorySelectFormFieldProps<any>) => {
  const { register } = formHook;

  const { data: productCategories = [] } = useQuery({
    queryKey: ['productCategories'],
    queryFn: () => productCategoryService.getAll(),
  });

  const productCategoryOptions = useMemo(
    () =>
      productCategories.map((category) => ({
        value: category.id.toString(),
        label: `${category.code} - ${category.name}`,
      })),
    [productCategories]
  );

  return (
    <SelectFormField
      title={'Productcategorie'}
      placeholder={'Selecteer een productcategorie'}
      options={productCategoryOptions}
      formHook={{
        register,
        name: formHook.name,
        rules: formHook.rules,
        errors: formHook.errors,
        control: formHook.control,
      }}
      testId="product-category-select"
    />
  );
};
