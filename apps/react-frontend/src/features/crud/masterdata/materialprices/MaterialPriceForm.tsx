import { MaterialPriceResponse } from '@/api/client';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { MaterialSelectFormField } from '@/components/ui/form/selectfield/MaterialSelectFormField';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormEvent, useEffect } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useForm } from 'react-hook-form';

interface MaterialPriceFormProps {
  isOpen: boolean;
  onCancel: () => void;
  onSubmit: (catalogItemId: string, price: number) => void;
  initialData?: MaterialPriceResponse;
}

interface MaterialPriceFormValues {
  catalogItemId: number | string;
  price: number;
}

export const MaterialPriceForm = ({
  isOpen,
  onCancel,
  onSubmit,
  initialData,
}: MaterialPriceFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();
  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<MaterialPriceFormValues>({
    defaultValues: {
      catalogItemId: '',
      price: 0,
    },
  });

  useEffect(() => {
    if (initialData) {
      reset({
        catalogItemId: initialData.id.toString(),
        price: initialData.defaultPrice ?? 0,
      });
    } else {
      reset({
        catalogItemId: '',
        price: 0,
      });
    }
  }, [initialData, reset]);

  const cancel = () => {
    reset({
      catalogItemId: '',
      price: 0,
    });
    onCancel();
  };

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        const catalogItemId =
          typeof data.catalogItemId === 'string'
            ? data.catalogItemId
            : String(data.catalogItemId);
        await onSubmit(catalogItemId, data.price);
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
            title={
              initialData
                ? 'Materiaalprijs bewerken'
                : 'Materiaalprijs toevoegen'
            }
            onClick={cancel}
          />
          <div className="flex flex-col items-center self-stretch p-4 gap-4">
            <div className="flex items-start self-stretch gap-4">
              <MaterialSelectFormField
                formHook={{
                  register,
                  name: 'catalogItemId',
                  rules: { required: 'Materiaal is verplicht' },
                  errors,
                  control,
                }}
                disabled={!!initialData}
              />
            </div>
            <div className="flex items-start self-stretch gap-4">
              <NumberFormField
                title={'Prijs'}
                placeholder={'Bijv. 10.50'}
                formHook={{
                  register,
                  name: 'price',
                  rules: { required: 'Prijs is verplicht' },
                  errors,
                }}
                step={0.01}
              />
              <TextFormField
                title={'Valuta'}
                placeholder={'Bijv. EUR'}
                disabled={true}
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
