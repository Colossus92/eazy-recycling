import { ProductRequest, ProductResponse } from '@/api/client';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { ProductCategorySelectFormField } from '@/components/ui/form/selectfield/ProductCategorySelectFormField';
import { VatRateSelectFormField } from '@/components/ui/form/selectfield/VatRateSelectFormField';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormEvent, useEffect } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useForm } from 'react-hook-form';
import { AuditMetadataFooter } from '@/components/ui/form/AuditMetadataFooter';
import { NumberFormField } from '@/components/ui/form/NumberFormField';

interface ProductFormProps {
  isOpen: boolean;
  onCancel: () => void;
  onSubmit: (product: ProductRequest) => void;
  initialData?: ProductResponse;
}

type ProductFormValues = Omit<ProductRequest, 'status'>;

export const ProductForm = ({
  isOpen,
  onCancel,
  onSubmit,
  initialData,
}: ProductFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();
  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<ProductFormValues>({
    defaultValues: {
      code: '',
      name: '',
      categoryId: undefined,
      unitOfMeasure: '',
      vatRateId: '',
      salesAccountNumber: '',
      purchaseAccountNumber: '',
      defaultPrice: undefined,
    },
  });

  useEffect(() => {
    if (initialData) {
      reset({
        code: initialData.code,
        name: initialData.name,
        categoryId: initialData.categoryId,
        unitOfMeasure: initialData.unitOfMeasure,
        vatRateId: initialData.vatRateId,
        salesAccountNumber: initialData.salesAccountNumber ?? '',
        purchaseAccountNumber: initialData.purchaseAccountNumber ?? '',
        defaultPrice: initialData.defaultPrice,
      });
    }
  }, [initialData, reset]);

  const cancel = () => {
    reset({
      code: '',
      name: '',
      categoryId: undefined,
      unitOfMeasure: '',
      vatRateId: '',
      salesAccountNumber: '',
      purchaseAccountNumber: '',
      defaultPrice: undefined,
    });
    onCancel();
  };

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        await onSubmit({
          code: data.code,
          name: data.name,
          categoryId: data.categoryId || undefined,
          unitOfMeasure: data.unitOfMeasure,
          vatRateId: data.vatRateId,
          salesAccountNumber: data.salesAccountNumber || undefined,
          purchaseAccountNumber: data.purchaseAccountNumber || undefined,
          defaultPrice: data.defaultPrice,
          status: 'ACTIVE',
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
            title={initialData ? 'Product bewerken' : 'Product toevoegen'}
            onClick={cancel}
          />
          <div className="flex flex-col items-center self-stretch p-4 gap-4">
            <div className="flex items-start self-stretch gap-4">
              <TextFormField
                title={'Code'}
                placeholder={'Vul een code in'}
                formHook={{
                  register,
                  name: 'code',
                  rules: { required: 'Code is verplicht' },
                  errors,
                }}
                disabled={Boolean(initialData?.code)}
              />
              <TextFormField
                title={'Naam'}
                placeholder={'Vul een naam in'}
                formHook={{
                  register,
                  name: 'name',
                  rules: { required: 'Naam is verplicht' },
                  errors,
                }}
              />
            </div>
            <div className="flex items-start self-stretch gap-4">
              <ProductCategorySelectFormField
                formHook={{
                  register,
                  name: 'categoryId',
                  errors,
                  control,
                }}
              />
            </div>
            <div className="flex items-start self-stretch gap-4">
              <div className="w-1/4">
                <TextFormField
                  title={'Eenheid'}
                  placeholder={'Bijv. uur'}
                  formHook={{
                    register,
                    name: 'unitOfMeasure',
                    rules: { required: 'Eenheid is verplicht' },
                    errors,
                  }}
                />
              </div>
              <div className="w-3/4">
                <VatRateSelectFormField
                  formHook={{
                    register,
                    name: 'vatRateId',
                    rules: { required: 'BTW code is verplicht' },
                    errors,
                    control,
                  }}
                />
              </div>
            </div>
            <div className="flex items-start self-stretch gap-4">
              <TextFormField
                title={'Grbk inkoop'}
                placeholder={'Bijv. 8000'}
                formHook={{
                  register,
                  name: 'purchaseAccountNumber',
                  errors,
                }}
              />
              <TextFormField
                title={'Grbk verkoop'}
                placeholder={'Bijv. 8000'}
                formHook={{
                  register,
                  name: 'salesAccountNumber',
                  errors,
                }}
              />
            </div>
            <div className="flex items-start self-stretch gap-4">
              <NumberFormField
                title={'Standaardprijs'}
                placeholder={'Bijv. 10.50'}
                formHook={{
                  register,
                  name: 'defaultPrice',
                  errors,
                }}
                step={0.01}
              />
            </div>
            <AuditMetadataFooter
              createdAt={initialData?.createdAt}
              createdByName={initialData?.createdByName}
              updatedAt={initialData?.updatedAt}
              updatedByName={initialData?.updatedByName}
            />
          </div>
          <FormActionButtons onClick={cancel} item={undefined} />
        </form>
        <ErrorDialogComponent />
      </ErrorBoundary>
    </FormDialog>
  );
};
