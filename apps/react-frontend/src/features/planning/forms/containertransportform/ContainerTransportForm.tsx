import { FormProvider } from 'react-hook-form';
import { KeyboardEvent, useEffect } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useFormStepNavigation } from '../../hooks/useFormStepNavigation';
import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { Stepper } from '@/components/ui/form/multistep/Stepper.tsx';
import { ContainerTransportFormDeliverySection } from '@/features/planning/forms/containertransportform/ContainerTransportFormDeliverySection.tsx';
import { ContainerTransportFormPickupSection } from '@/features/planning/forms/containertransportform/ContainerTransportFormPickupSection.tsx';
import { FormNavigation } from '@/features/planning/forms/FormNavigation.tsx';
import { TransportFormDetailsSection } from '@/features/planning/forms/TransportFormDetailsSection.tsx';
import { ContainerTransportMainSection } from '@/features/planning/forms/containertransportform/ContainerTransportMainSection.tsx';
import { useContainerTransportForm } from '@/features/planning/hooks/useContainerTransportForm';
import { fallbackRender } from '@/utils/fallbackRender';
import { AuditMetadataFooter } from '@/components/ui/form/AuditMetadataFooter';

interface ContainerTransportFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  transportId?: string;
}

export const ContainerTransportForm = ({
  isOpen,
  setIsOpen,
  transportId,
}: ContainerTransportFormProps) => {
  const {
    data,
    isLoading: isLoadingTransport,
    formContext,
    fieldsToValidate,
    mutation,
    resetForm,
  } = useContainerTransportForm(transportId, () => setIsOpen(false));
  const { step, navigateToStep, next, back, reset, isSubmitting } =
    useFormStepNavigation({
      formContext,
      fieldsToValidate,
      onSubmit: async (data) => {
        await mutation.mutateAsync(data);
      },
      isSubmitting: mutation.isPending,
    });

  // Reset form and step when form opens
  useEffect(() => {
    if (isOpen) {
      resetForm();
      reset();
    }
  }, [isOpen, reset, resetForm]);

  const onCancel = () => {
    reset();
    setIsOpen(false);
  };

  const steps = [
    <ContainerTransportMainSection />,
    <ContainerTransportFormPickupSection />,
    <ContainerTransportFormDeliverySection />,
    <TransportFormDetailsSection />,
  ];

  const handleKeyDown = (e: KeyboardEvent<HTMLFormElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      next();
    }
  };

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <FormDialog isOpen={isOpen} setIsOpen={onCancel}>
        <div className={'w-full'}>
          <FormProvider {...formContext}>
            <form
              onSubmit={(e) => e.preventDefault()}
              onKeyDown={handleKeyDown}
            >
              <FormTopBar
                title={
                  data
                    ? 'Container transport bewerken'
                    : 'Nieuw container transport'
                }
                onClick={onCancel}
              />
              <div
                className={'flex flex-col items-start self-stretch gap-5 p-4 w'}
              >
                <Stepper
                  navigateToStep={navigateToStep}
                  step={step}
                  stepDescriptions={[
                    'Transport info',
                    'Ophaal info',
                    'Aflever info',
                    'Transport details',
                  ]}
                />
                {isLoadingTransport ? (
                  <div className="flex justify-center items-center w-full p-8">
                    <p>Transport gegevens laden...</p>
                  </div>
                ) : (
                  steps[step]
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
