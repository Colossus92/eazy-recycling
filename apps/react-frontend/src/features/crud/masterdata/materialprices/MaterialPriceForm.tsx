import { MaterialPriceRequest, MaterialPriceResponse } from '@/api/client';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { MaterialSelectFormField } from '@/components/ui/form/selectfield/MaterialSelectFormField';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormEvent, useEffect } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useForm } from 'react-hook-form';

interface MaterialPriceFormProps {
  isOpen: boolean;
  onCancel: () => void;
  onSubmit: (materialPrice: MaterialPriceRequest) => void;
  initialData?: MaterialPriceResponse;
}

type MaterialPriceFormValues = Omit<MaterialPriceRequest, 'materialId' | 'price'> & {
  materialId: number | string;
  price: number;
};

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
      materialId: '',
      price: 0,
      currency: 'EUR',
    },
  });

  useEffect(() => {
    if (initialData) {
      reset({
        materialId: initialData.materialId.toString(),
        price: initialData.price,
        currency: initialData.currency,
      });
    }
  }, [initialData, reset]);

  const cancel = () => {
    reset({
      materialId: '',
      price: 0,
      currency: '',
    });
    onCancel();
  };

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        await onSubmit({
          materialId:
            typeof data.materialId === 'string'
              ? parseInt(data.materialId, 10)
              : data.materialId,
          price: data.price,
          currency: data.currency,
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
                  name: 'materialId',
                  rules: { required: 'Materiaal is verplicht' },
                  errors,
                  control,
                }}
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
                value={initialData?.currency || 'EUR'}
                disabled={true}
                formHook={{
                  register,
                  name: 'currency',
                  rules: { required: 'Valuta is verplicht' },
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
