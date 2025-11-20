import { SelectFormField } from "./SelectFormField";

import { useQuery } from "@tanstack/react-query";
import { materialGroupService } from "@/api/services/materialGroupService";
import {
  Control,
  FieldErrors,
  FieldValues,
  Path,
  RegisterOptions,
  UseFormRegister,
} from "react-hook-form";
import { useMemo } from "react";

interface MaterialGroupSelectFormFieldProps<TFieldValues extends FieldValues> {
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control?: Control<TFieldValues>;
  };
}
export const MaterialGroupSelectFormField = ({
      formHook,
    }: MaterialGroupSelectFormFieldProps<any>) => {
    const { register } = formHook;


  // Fetch material groups for dropdown
  const { data: materialGroups = [] } = useQuery({
    queryKey: ['materialGroups'],
    queryFn: () => materialGroupService.getAll(),
  });

  const materialGroupOptions = useMemo(
    () =>
      materialGroups.map((group) => ({
        value: group.code,
        label: `${group.code} - ${group.name}`,
      })),
    [materialGroups]
  );
    return (
      <SelectFormField
        title={'Materiaalgroep'}
        placeholder={'Selecteer een materiaalgroep'}
        options={materialGroupOptions}
        formHook={{
          register,
          name: formHook.name,
          rules: formHook.rules,
          errors: formHook.errors,
          control: formHook.control,
        }}
        testId="material-group-select"
      />
    );
  };