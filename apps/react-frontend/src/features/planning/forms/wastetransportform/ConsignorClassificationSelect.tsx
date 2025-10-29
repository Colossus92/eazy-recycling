import { RadioGroup, Label, Field, Radio } from '@headlessui/react';
import {
  FieldValues,
  UseFormRegister,
  Path,
  RegisterOptions,
  FieldErrors,
  Control,
} from 'react-hook-form';
import { Controller } from 'react-hook-form';
import { WasteStreamFormValues } from '@/features/wastestreams/components/wastetransportform/hooks/useWasteStreamFormHook.ts';

interface ConsignorClassificationSelectProps<TFieldValues extends FieldValues> {
  testId?: string;
  formHook: {
    register: UseFormRegister<TFieldValues>;
    name: Path<TFieldValues>;
    rules?: RegisterOptions<TFieldValues>;
    errors: FieldErrors;
    control?: Control<TFieldValues>;
  };
}

const consignorTypes = [
  { value: 1, label: 'ontdoener' },
  { value: 2, label: 'ontvanger' },
  { value: 3, label: 'handelaar' },
  { value: 4, label: 'bemiddelaar' },
];

export const ConsignorClassificationSelect = ({
  formHook,
  testId = 'consignor-classification-select',
}: ConsignorClassificationSelectProps<WasteStreamFormValues>) => {
  const { name, rules, errors, control } = formHook;

  return (
    <div className="space-y-2 w-full">
      <Controller
        control={control}
        name={name}
        rules={rules}
        defaultValue={consignorTypes[0].value}
        render={({ field }) => (
          <>
            <RadioGroup
              value={field.value || consignorTypes[0].value}
              onChange={field.onChange}
              className="w-full"
            >
              <div className="flex flex-row items-center justify-between w-full gap-x-4 gap-y-2">
                {consignorTypes.map((type) => (
                  <Field
                    key={type.value}
                    className="flex items-center gap-2 cursor-pointer"
                  >
                    <Radio
                      value={type.value}
                      data-testid={`${testId}-option-${type.value}`}
                      className="h-5 w-5 cursor-pointer rounded-full border border-gray-300 bg-white"
                    >
                      {({ checked }) => (
                        <div className="flex items-center justify-center h-full w-full">
                          {checked && (
                            <div className="h-2.5 w-2.5 rounded-full bg-blue-600" />
                          )}
                        </div>
                      )}
                    </Radio>
                    <Label className="cursor-pointer text-sm">
                      {type.label}
                    </Label>
                  </Field>
                ))}
              </div>
            </RadioGroup>
          </>
        )}
      />
      {errors[name] && (
        <p className="text-red-500 text-xs mt-1">
          {errors[name]?.message as string}
        </p>
      )}
    </div>
  );
};
