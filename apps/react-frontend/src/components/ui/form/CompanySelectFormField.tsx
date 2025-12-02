import { FieldValues, Path, useFormContext } from 'react-hook-form';
import { SelectFormField } from './selectfield/SelectFormField';
import { useQuery } from '@tanstack/react-query';
import { Company } from '@/api/services/companyService';
import { companyService } from '@/api/services/companyService.ts';

interface CompanySelectFormFieldProps<TFieldValues extends FieldValues> {
  title: string;
  placeholder: string;
  name: Path<TFieldValues>;
  rules: any;
  disabled?: boolean;
  role?: string;
}

export const CompanySelectFormField = <TFieldValues extends FieldValues>({
  title,
  placeholder,
  name,
  rules,
  disabled = false,
  role = undefined,
}: CompanySelectFormFieldProps<TFieldValues>) => {
  const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies', 'list', role],
    queryFn: () => companyService.getAllAsList(false, role),
  });
  const companyOptions = companies.map((company) => ({
    value: company.id || '',
    label: company.name,
  }));

  const formContext = useFormContext<TFieldValues>();
  return (
    <SelectFormField
      title={title}
      placeholder={placeholder}
      options={companyOptions}
      testId="company-select"
      formHook={{
        register: formContext.register,
        name,
        rules,
        errors: formContext.formState.errors,
        control: formContext.control,
      }}
      disabled={disabled}
    />
  );
};
