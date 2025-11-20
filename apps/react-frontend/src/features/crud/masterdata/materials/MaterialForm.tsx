import { MaterialRequest, MaterialResponse } from '@/api/client';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { MaterialGroupSelectFormField } from '@/components/ui/form/selectfield/MaterialGroupSelectFormField';
import { VatRateSelectFormField } from '@/components/ui/form/selectfield/VatRateSelectFormField';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useForm } from 'react-hook-form';

interface MaterialFormProps {
  isOpen: boolean;
  onCancel: () => void;
  onSubmit: (material: MaterialRequest) => void;
  initialData?: MaterialResponse;
}

type MaterialFormValues = Omit<MaterialRequest, 'status' | 'materialGroupId'> & {
  materialGroupId: number | string;
};

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
    values: initialData
      ? {
        ...initialData,
        materialGroupId: initialData.materialGroupId.toString(),
      }
      : undefined,
  });

  const cancel = () => {
    reset({
      code: '',
      name: '',
      materialGroupId: '',
      unitOfMeasure: '',
      vatCode: '',
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
          materialGroupId:
            typeof data.materialGroupId === 'string'
              ? parseInt(data.materialGroupId, 10)
              : data.materialGroupId,
          unitOfMeasure: data.unitOfMeasure,
          vatCode: data.vatCode,
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
          </div>
          <FormActionButtons onClick={cancel} item={undefined} />
        </form>
        <ErrorDialogComponent />
      </ErrorBoundary>
    </FormDialog>
  );
};
