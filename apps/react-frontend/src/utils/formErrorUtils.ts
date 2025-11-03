import { FieldErrors, FieldValues, Path } from 'react-hook-form';

/**
 * Safely extract error message from nested field errors
 * Handles paths like "pickupLocation.buildingNumber"
 */
export const getFieldError = <TFieldValues extends FieldValues>(
  errors: FieldErrors<TFieldValues> | undefined,
  fieldName: Path<TFieldValues> | string | undefined
): string | undefined => {
  if (!errors || !fieldName) return undefined;

  const pathParts = String(fieldName).split('.');
  let current: any = errors;

  for (const part of pathParts) {
    current = current?.[part];
    if (!current) return undefined;
  }

  return current?.message?.toString();
};
