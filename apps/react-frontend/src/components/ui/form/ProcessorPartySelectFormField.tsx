import { Company } from "@/api/services/companyService";
import { useQuery } from "@tanstack/react-query";
import { companyService } from "@/api/services/companyService.ts";
import { SelectFormField } from "./selectfield/SelectFormField";
import { FieldValues, useFormContext } from "react-hook-form";
import { Path } from "react-hook-form";

interface ProcessorPartySelectFormFieldProps<TFieldValues extends FieldValues> {
    name: Path<TFieldValues>;
    rules: any;
    disabled?: boolean;
}

export const ProcessorPartySelectFormField = <TFieldValues extends FieldValues>({
    name,
    rules,
    disabled = false,
}: ProcessorPartySelectFormFieldProps<TFieldValues>) => {
    const formContext = useFormContext<TFieldValues>();
    const { data: companies = [] } = useQuery<Company[]>({
        queryKey: ['companies'],
        queryFn: () => companyService.getAll(),
    });
    const processorPartyOptions = companies.filter((company) => company.processorId).map((company) => ({
        value: company.processorId!!,
        label: company.name,
    }));

    return (
        <SelectFormField
            title={'Verwerker (bestemming)'}
            placeholder={'Selecteer een verwerker'}
            options={processorPartyOptions}
            testId="processor-party-select"
            disabled={disabled}
            formHook={{
                register: formContext.register,
                name: name,
                rules: rules,
                errors: formContext.formState.errors,
                control: formContext.control,
            }}
        />
    );
};