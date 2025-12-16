import { MaterialRequest, MaterialResponse } from '@/api/client';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { MaterialGroupSelectFormField } from '@/components/ui/form/selectfield/MaterialGroupSelectFormField';
import { VatRateSelectFormField } from '@/components/ui/form/selectfield/VatRateSelectFormField';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormEvent, useEffect } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useForm } from 'react-hook-form';
import { AuditMetadataFooter } from '@/components/ui/form/AuditMetadataFooter';

interface MaterialFormProps {
  isOpen: boolean;
  onCancel: () => void;
  onSubmit: (material: MaterialRequest) => void;
  initialData?: MaterialResponse;
}

type MaterialFormValues = Omit<MaterialRequest, 'status'>;

export const MaterialForm = ({
  isOpen,
  onCancel,
  onSubmit,
  initialData,
}: MaterialFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();
  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<MaterialFormValues>({
    defaultValues: {
      code: '',
      name: '',
      materialGroupId: 0,
      unitOfMeasure: '',
      vatCode: '',
      purchaseAccountNumber: '',
      salesAccountNumber: '',
    },
  });

  useEffect(() => {
    if (initialData) {
      reset({
        code: initialData.code,
        name: initialData.name,
        materialGroupId: initialData.materialGroupId,
        unitOfMeasure: initialData.unitOfMeasure,
        vatCode: initialData.vatCode,
        purchaseAccountNumber: initialData.purchaseAccountNumber ?? '',
        salesAccountNumber: initialData.salesAccountNumber ?? '',
      });
    }
  }, [initialData, reset]);

  const cancel = () => {
    reset({
      code: '',
      name: '',
      materialGroupId: 0,
      unitOfMeasure: '',
      vatCode: '',
      purchaseAccountNumber: '',
      salesAccountNumber: '',
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
          materialGroupId: data.materialGroupId,
          unitOfMeasure: data.unitOfMeasure,
          vatCode: data.vatCode,
          salesAccountNumber: data.salesAccountNumber || undefined,
          purchaseAccountNumber: data.purchaseAccountNumber || undefined,
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
            title={initialData ? 'Materiaal bewerken' : 'Materiaal toevoegen'}
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
              <MaterialGroupSelectFormField
                formHook={{
                  register,
                  name: 'materialGroupId',
                  rules: { required: 'Materiaalgroep is verplicht' },
                  errors,
                  control,
                }}
              />
            </div>
            <div className="flex items-start self-stretch gap-4">
              <div className="w-1/4">
                <TextFormField
                  title={'Eenheid'}
                  placeholder={'Bijv. kg'}
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
                    name: 'vatCode',
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
