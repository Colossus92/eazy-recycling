import { VatRateRequest, VatRateResponse } from '@/api/client';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { FormEvent, useEffect } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useForm } from 'react-hook-form';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { DateTimeInput } from '@/components/ui/form/DateTimeInput';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';

// Convert ISO 8601 datetime (e.g., 2025-11-20T11:09:00Z) to datetime-local format (YYYY-MM-DDThh:mm)
const formatDateTimeForInput = (dateTimeString: string | undefined): string => {
  if (!dateTimeString) return '';
  // Remove 'Z' and seconds, keep only YYYY-MM-DDThh:mm format
  return dateTimeString.replace(/:\d{2}Z?$/, '').substring(0, 16);
};

interface VatRateFormProps {
  isOpen: boolean;
  onCancel: () => void;
  onSubmit: (vatRate: VatRateRequest) => void;
  initialData?: VatRateResponse;
}

type VatRateFormValues = Omit<VatRateRequest, 'percentage'> & {
  percentage: number;
};

export const VatRateForm = ({
  isOpen,
  onCancel,
  onSubmit,
  initialData,
}: VatRateFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<VatRateFormValues>({
    defaultValues: {
      vatCode: '',
      percentage: 0,
      validFrom: '',
      validTo: '',
      countryCode: '',
      description: '',
    },
  });

  useEffect(() => {
    if (initialData) {
      reset({
        vatCode: initialData.vatCode,
        percentage: parseFloat(initialData.percentage),
        validFrom: formatDateTimeForInput(initialData.validFrom),
        validTo: formatDateTimeForInput(initialData.validTo),
        countryCode: initialData.countryCode,
        description: initialData.description,
      });
    }
  }, [initialData, reset]);

  const cancel = () => {
    reset({
      vatCode: '',
      percentage: 0,
      validFrom: '',
      validTo: '',
      countryCode: '',
      description: '',
    });
    onCancel();
  };

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        await onSubmit({
          vatCode: data.vatCode,
          percentage: data.percentage.toString(),
          validFrom: data.validFrom,
          validTo: data.validTo || undefined,
          countryCode: data.countryCode,
          description: data.description,
        });
        cancel();
      } catch (error) {
        handleError(error);
      }
    })();
  };

  return (
    <FormDialog isOpen={isOpen} setIsOpen={cancel}>
      <ErrorBoundary fallbackRender={fallbackRender}>
        <form
          className="flex flex-col items-center self-stretch"
          onSubmit={(e) => submitForm(e)}
        >
          <FormTopBar
            title={initialData ? 'BTW tarief bewerken' : 'BTW tarief toevoegen'}
            onClick={cancel}
          />
          <div className="flex flex-col items-center self-stretch p-4 gap-4">
            <div className="flex items-start self-stretch gap-4">
              <TextFormField
                title={'BTW Code'}
                placeholder={'Bijv. H'}
                formHook={{
                  register,
                  name: 'vatCode',
                  rules: { required: 'BTW code is verplicht' },
                  errors,
                }}
                disabled={Boolean(initialData?.vatCode)}
              />
              <NumberFormField
                title={'Percentage'}
                placeholder={'Bijv. 21'}
                formHook={{
                  register,
                  name: 'percentage',
                  rules: { required: 'Percentage is verplicht' },
                  errors,
                }}
                step={'any'}
              />
              <TextFormField
                title={'Land'}
                placeholder={'Bijv. NL'}
                formHook={{
                  register,
                  name: 'countryCode',
                  rules: { required: 'Land is verplicht' },
                  errors,
                }}
              />
            </div>
            <div className="flex items-start self-stretch gap-4">
              <DateTimeInput
                title={'Geldig vanaf'}
                formHook={{
                  register,
                  name: 'validFrom',
                  rules: { required: 'Geldig vanaf is verplicht' },
                  errors,
                }}
                testId="valid-from-input"
              />
              <DateTimeInput
                title={'Geldig tot'}
                formHook={{
                  register,
                  name: 'validTo',
                  errors,
                }}
                testId="valid-to-input"
              />
            </div>
            <div className="flex items-start self-stretch gap-4">
              <TextFormField
                title={'Beschrijving'}
                placeholder={'Vul een beschrijving in'}
                formHook={{
                  register,
                  name: 'description',
                  rules: { required: 'Beschrijving is verplicht' },
                  errors,
                }}
              />
            </div>
          </div>
          <FormActionButtons onClick={cancel} item={undefined} />
        </form>
        <ErrorDialogComponent />
      </ErrorBoundary>
    </FormDialog>
  );
};
