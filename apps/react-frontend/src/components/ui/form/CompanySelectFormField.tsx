import { useFormContext } from "react-hook-form";
import { SelectFormField } from "./selectfield/SelectFormField";
import { useQuery } from "@tanstack/react-query";
import { Company } from "@/api/services/companyService";
import { companyService } from "@/api/services/companyService.ts";
import { FieldValues, Path } from "react-hook-form";

interface CompanySelectFormFieldProps<TFieldValues extends FieldValues> {
    title: string;
    placeholder: string;
    name: Path<TFieldValues>;
    rules: any;
    disabled?: boolean;
}


export const CompanySelectFormField = <TFieldValues extends FieldValues>({
    title,
    placeholder,
    name,
    rules,
    disabled = false,
}: CompanySelectFormFieldProps<TFieldValues>) => {
      const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
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