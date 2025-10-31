import { WasteStreamFormValues } from '@/features/wastestreams/components/wastetransportform/hooks/useWasteStreamFormHook.ts';
import { RadioFormField, RadioFormFieldProps } from '@/components/ui/form/RadioFormField';

type ConsignorClassificationSelectProps = Omit<RadioFormFieldProps<WasteStreamFormValues>, 'options'>;

const consignorTypes = [
  { value: '1', label: 'ontdoener' },
  { value: '2', label: 'ontvanger' },
  { value: '3', label: 'handelaar' },
  { value: '4', label: 'bemiddelaar' },
];

export const ConsignorClassificationSelect = ({
  formHook,
  testId = 'consignor-classification-select',
  ...props
}: ConsignorClassificationSelectProps) => {
  return (
    <RadioFormField<WasteStreamFormValues>
      options={consignorTypes}
      formHook={formHook}
      testId={testId}
      {...props}
    />
  );
};
