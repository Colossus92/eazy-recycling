import clsx from 'clsx';

interface LmaErrorCodeTagProps {
  errorCode: string;
}

const errorCodeLabels: Record<string, string> = {
  COMPANY_NOT_FOUND: 'Bedrijf niet gevonden',
  INVALID_EURAL_CODE: 'Ongeldige Euralcode',
  INVALID_PROCESSING_METHOD: 'Ongeldige verwerkingsmethode',
  DUPLICATE_WASTE_STREAM: 'Dubbele afvalstroom',
  MISSING_REQUIRED_FIELD: 'Verplicht veld ontbreekt',
  INVALID_CSV_FORMAT: 'Ongeldig CSV formaat',
  PROCESSOR_NOT_FOUND: 'Verwerker niet gevonden',
  VALIDATION_ERROR: 'Validatiefout',
};

const errorCodeColors: Record<string, string> = {
  COMPANY_NOT_FOUND:
    'bg-color-warning-light text-color-warning border-color-warning',
  INVALID_EURAL_CODE:
    'bg-color-error-light text-color-error border-color-error',
  INVALID_PROCESSING_METHOD:
    'bg-color-error-light text-color-error border-color-error',
  DUPLICATE_WASTE_STREAM:
    'bg-color-warning-light text-color-warning border-color-warning',
  MISSING_REQUIRED_FIELD:
    'bg-color-error-light text-color-error border-color-error',
  INVALID_CSV_FORMAT:
    'bg-color-error-light text-color-error border-color-error',
  PROCESSOR_NOT_FOUND:
    'bg-color-warning-light text-color-warning border-color-warning',
  VALIDATION_ERROR: 'bg-color-error-light text-color-error border-color-error',
};

export const LmaErrorCodeTag = ({ errorCode }: LmaErrorCodeTagProps) => {
  const label = errorCodeLabels[errorCode] || errorCode;
  const colorClass =
    errorCodeColors[errorCode] ||
    'bg-color-surface-secondary text-color-text-secondary border-color-border-primary';

  return (
    <span
      className={clsx(
        'inline-flex items-center px-2 py-0.5 rounded-full text-caption font-medium border',
        colorClass
      )}
    >
      {label}
    </span>
  );
};
