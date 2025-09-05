import { FormProvider } from 'react-hook-form';
import { KeyboardEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useFormStepNavigation } from '../../hooks/useFormStepNavigation';
import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { Stepper } from '@/components/ui/form/multistep/Stepper.tsx';
import { FormNavigation } from '@/features/planning/forms/FormNavigation.tsx';
import { TransportFormDetailsSection } from '@/features/planning/forms/TransportFormDetailsSection.tsx';
import { WasteTransportMainSection } from '@/features/planning/forms/wastetransportform/WasteTransportMainSection';
import { WasteTransportFormDeliverySection } from '@/features/planning/forms/wastetransportform/WasteTransportFormDeliverySection.tsx';
import { WasteTransportFormGoodsSection } from '@/features/planning/forms/wastetransportform/WasteTransportFormGoodsSection.tsx';
import { WasteTransportFormPickupSection } from '@/features/planning/forms/wastetransportform/WasteTransportFormPickupSection.tsx';
import { useWasteTransportForm } from '@/features/planning/hooks/useWasteTransportForm.ts';
import { fallbackRender } from '@/utils/fallbackRender';

interface ContainerTransportFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  transportId?: string;
}

export const WasteTransportForm = ({
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
  } = useWasteTransportForm(transportId, () => setIsOpen(false));
  const { step, navigateToStep, next, back, reset, isSubmitting } =
    useFormStepNavigation({
      formContext,
      fieldsToValidate,
      onSubmit: async (data) => {
        await mutation.mutateAsync(data);
      },
      isEditMode: !!transportId,
      isSubmitting: mutation.isPending,
    });

  const onCancel = () => {
    reset();
    setIsOpen(false);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLFormElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      next();
    }
  };

  const steps = [
    <WasteTransportMainSection />,
    <WasteTransportFormPickupSection />,
    <WasteTransportFormDeliverySection />,
    <WasteTransportFormGoodsSection />,
    <TransportFormDetailsSection />,
  ];

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
                  data ? 'Afval transport bewerken' : 'Nieuw afval transport'
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
                    'Afval details',
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
