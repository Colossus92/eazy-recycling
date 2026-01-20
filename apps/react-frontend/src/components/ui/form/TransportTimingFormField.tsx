import { Controller, Control, FieldValues, Path } from 'react-hook-form';
import { TransportTimingInput } from './TransportTimingInput';
import { TimingConstraint, createEmptyTimingConstraint } from '@/types/forms/TimingConstraint';

interface TransportTimingFormFieldProps<T extends FieldValues> {
  name: Path<T>;
  control: Control<T>;
  label: string;
  required?: boolean;
  disabled?: boolean;
  testId?: string;
}

export const TransportTimingFormField = <T extends FieldValues>({
  name,
  control,
  label,
  required = false,
  disabled = false,
  testId,
}: TransportTimingFormFieldProps<T>) => {
  return (
    <Controller
      name={name}
      control={control}
      defaultValue={createEmptyTimingConstraint() as T[Path<T>]}
      rules={{
        validate: (value: TimingConstraint) => {
          if (required && !value.date) {
            return 'Dit veld is verplicht';
          }
          if (value.mode !== 'DATE_ONLY') {
            if (!value.windowStart || !value.windowEnd) {
              return 'Start- en eindtijd zijn verplicht voor tijdvenster';
            }
            const [startHour, startMin] = value.windowStart.split(':').map(Number);
            const [endHour, endMin] = value.windowEnd.split(':').map(Number);
            const startMinutes = startHour * 60 + startMin;
            const endMinutes = endHour * 60 + endMin;
            if (startMinutes > endMinutes) {
              return 'Begintijd moet vóór eindtijd liggen';
            }
          }
          return true;
        },
      }}
      render={({ field, fieldState }) => (
        <TransportTimingInput
          label={label}
          value={field.value || createEmptyTimingConstraint()}
          onChange={field.onChange}
          required={required}
          disabled={disabled}
          testId={testId}
          error={fieldState.error?.message}
        />
      )}
    />
  );
};
