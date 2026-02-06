import { SelectFormField } from "./SelectFormField";
import { useQuery } from "@tanstack/react-query";
import { vatRateService } from "@/api/services/vatRateService";
import { useMemo } from "react";
import {
  Control,
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from "react-hook-form";

interface VatRateSelectFormFieldProps<TFieldValues extends FieldValues> {
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control?: Control<TFieldValues>;
  };
}
export const VatRateSelectFormField = ({
      formHook,
}: VatRateSelectFormFieldProps<any>) => {
    const { register, } = formHook;

      // Fetch VAT rates for dropdown
      const { data: vatRates = [] } = useQuery({
        queryKey: ['vatRates'],
        queryFn: () => vatRateService.getAll(),
      });

        const vatCodeOptions = useMemo(
    () =>
      vatRates.map((rate) => ({
        value: rate.id,
        label: `${rate.vatCode} - ${rate.percentage}%`,
      })),
    [vatRates]
  );

  return (
    <SelectFormField
                    title={'BTW Code'}
                    placeholder={'Selecteer een BTW code'}
                    options={vatCodeOptions}
                    formHook={{
                      register,
                      name: formHook.name,
                      rules: formHook.rules,
                      errors: formHook.errors,
                      control: formHook.control,
                    }}
                    testId="vat-code-select"
                  />
  );
}