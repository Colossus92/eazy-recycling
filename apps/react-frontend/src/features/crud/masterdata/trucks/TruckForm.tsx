import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { FormEvent, useEffect } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { FormProvider, useForm } from 'react-hook-form';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { TruckRequest } from '@/api/client';
import { Truck } from '@/api/services/truckService';
import { AuditMetadataFooter } from '@/components/ui/form/AuditMetadataFooter';
import { useTenantCompany } from '@/hooks/useTenantCompany';

interface TruckFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  onCancel: () => void;
  onSubmit: (eural: TruckRequest) => void;
  initialData?: Truck;
}

export interface TruckFormValues {
  brand: string;
  description: string;
  licensePlate: string;
  carrierPartyId?: string;
}

function toTruckRequest(data: TruckFormValues) {
  return {
    brand: data.brand,
    description: data.description,
    licensePlate: data.licensePlate,
    carrierPartyId: data.carrierPartyId,
  } as TruckRequest;
}

function toFormData(truck?: Truck) {
  return {
    brand: truck?.brand,
    description: truck?.description,
    licensePlate: truck?.licensePlate,
    carrierPartyId: truck?.carrierPartyId,
  } as TruckFormValues;
}

export const TruckForm = ({
  isOpen,
  setIsOpen,
  onCancel,
  onSubmit,
  initialData,
}: TruckFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();
  const { data: tenantCompany } = useTenantCompany();
  const methods = useForm<TruckFormValues>({
    values: toFormData(initialData),
  });

  // Set tenant company as default carrier for new trucks
  useEffect(() => {
    if (!initialData && tenantCompany?.id && !methods.getValues('carrierPartyId')) {
      methods.setValue('carrierPartyId', tenantCompany.id);
    }
  }, [initialData, tenantCompany, methods]);

  const cancel = () => {
    methods.reset({
      licensePlate: '',
      description: '',
      brand: '',
      carrierPartyId: '',
    });
    onCancel();
  };

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await methods.handleSubmit(async (data) => {
      try {
        await onSubmit(toTruckRequest(data));
        cancel();
      } catch (error) {
        handleError(error);
      }
    })();
  };

  return (
    <FormDialog isOpen={isOpen} setIsOpen={setIsOpen}>
      <ErrorBoundary fallbackRender={fallbackRender}>
        <FormProvider {...methods}>
          <form
            className="flex flex-col items-center self-stretch"
            onSubmit={(e) => submitForm(e)}
          >
            <FormTopBar
              title={
                initialData ? 'Vrachtwagen bewerken' : 'Vrachtwagen toevoegen'
              }
              onClick={cancel}
            />
            <div className="flex flex-col items-center self-stretch p-4 gap-4">
              <TextFormField
                title={'Merk'}
                placeholder={'Vul merk in'}
                formHook={{
                  register: methods.register,
                  name: 'brand',
                  rules: { required: 'Merk is verplicht' },
                  errors: methods.formState.errors,
                }}
              />
              <div className="flex items-start self-stretch gap-4">
                <TextFormField
                  title={'Beschrijving'}
                  placeholder={'Vul een beschrijving in'}
                  formHook={{
                    register: methods.register,
                    name: 'description',
                    rules: { required: 'Beschrijving is verplicht' },
                    errors: methods.formState.errors,
                  }}
                />
                <TextFormField
                  title={'Kenteken'}
                  placeholder={'Vul kenteken in'}
                  formHook={{
                    register: methods.register,
                    name: 'licensePlate',
                    rules: { required: 'Kenteken is verplicht' },
                    errors: methods.formState.errors,
                  }}
                  disabled={Boolean(initialData?.licensePlate)}
                />
              </div>
              <CompanySelectFormField
                name="carrierCompanyId"
                title="Transporteur"
                placeholder="Selecteer transporteur"
                rules={undefined}
                role="CARRIER"
              />
              <AuditMetadataFooter
                createdAt={initialData?.createdAt}
                createdByName={initialData?.createdByName}
                updatedAt={initialData?.updatedAt}
                updatedByName={initialData?.updatedByName}
              />
            </div>
            <FormActionButtons onClick={cancel} item={initialData} />
          </form>
        </FormProvider>
        <ErrorDialogComponent />
      </ErrorBoundary>
    </FormDialog>
  );
};
