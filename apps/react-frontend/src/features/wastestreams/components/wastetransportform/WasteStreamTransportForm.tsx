import { FormProvider } from 'react-hook-form';
import { KeyboardEvent, useEffect } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useFormStepNavigation } from '@/features/planning/hooks/useFormStepNavigation';
import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { Stepper } from '@/components/ui/form/multistep/Stepper.tsx';
import { FormNavigation } from '@/features/planning/forms/FormNavigation.tsx';
import { useWasteStreamTransportForm } from '@/features/wastestreams/hooks/useWasteStreamTransportForm';
import { WasteStreamTransportFormSelectSection } from './WasteStreamTransportFormSelectSection';
import { WasteStreamTransportFormDetailsSection } from './WasteStreamTransportFormDetailsSection';
import { fallbackRender } from '@/utils/fallbackRender';
import { AuditMetadataFooter } from '@/components/ui/form/AuditMetadataFooter';
import { useTenantCompany } from '@/hooks/useTenantCompany';

interface WasteStreamTransportFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  transportId?: string;
}

export const WasteStreamTransportForm = ({
  isOpen,
  setIsOpen,
  transportId,
}: WasteStreamTransportFormProps) => {
  const { data: tenantCompany } = useTenantCompany();
  const {
    data,
    isLoading,
    formContext,
    fieldsToValidate,
    mutation,
    resetForm,
  } = useWasteStreamTransportForm(transportId, () => setIsOpen(false));

  const { step, navigateToStep, next, back, reset, isSubmitting } =
    useFormStepNavigation({
      formContext,
      fieldsToValidate,
      onSubmit: async (data) => {
        await mutation.mutateAsync(data);
      },
      isSubmitting: mutation.isPending,
    });

  // Reset step and set default carrier when form opens for new transport
  useEffect(() => {
    if (isOpen) {
      reset();
      if (!transportId && tenantCompany?.id) {
        formContext.setValue('carrierPartyId', tenantCompany.id);
      }
    }
  }, [isOpen, reset, transportId, tenantCompany, formContext]);

  const onCancel = () => {
    resetForm();
    reset();
    setIsOpen(false);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLFormElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      next();
    }
  };

  // Multi-step form
  const steps = [
    <WasteStreamTransportFormSelectSection />,
    <WasteStreamTransportFormDetailsSection />,
  ];

  const stepDescriptions = [
    'Afvalstroom',
    'Transport Details',
  ];

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <FormDialog isOpen={isOpen} setIsOpen={onCancel} width='w-[800px]'>
        <div className="w-full h-[80vh]">
          <FormProvider {...formContext}>
            <form
              className="flex flex-col items-center self-stretch h-full"
              onSubmit={(e) => e.preventDefault()}
              onKeyDown={handleKeyDown}
            >
              <FormTopBar
                title={
                  data
                    ? 'Afvaltransport bewerken'
                    : 'Nieuw afvaltransport'
                }
                onClick={onCancel}
              />
              
              <div className="flex flex-col items-start self-stretch gap-5 p-4 flex-1 overflow-y-auto">
                <Stepper
                  navigateToStep={navigateToStep}
                  step={step}
                  stepDescriptions={stepDescriptions}
                />
                
                {isLoading ? (
                  <div className="flex justify-center items-center w-full p-8">
                    <p>Transport gegevens laden...</p>
                  </div>
                ) : (
                  <div className="w-full">
                    {steps[step]}
                  </div>
                )}
                <AuditMetadataFooter
                  createdAt={data?.createdAt}
                  createdByName={data?.createdByName}
                  updatedAt={data?.updatedAt}
                  updatedByName={data?.updatedByName}
                />
              </div>

              <FormNavigation
                step={step}
                totalSteps={steps.length - 1}
                onNext={next}
                onBack={back}
                onCancel={onCancel}
                isSubmitting={isSubmitting}
              />
            </form>
          </FormProvider>
        </div>
      </FormDialog>
    </ErrorBoundary>
  );
};
