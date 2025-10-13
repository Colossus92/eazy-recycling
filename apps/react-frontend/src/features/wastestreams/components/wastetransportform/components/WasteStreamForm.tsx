import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { Stepper } from '@/components/ui/form/multistep/Stepper.tsx';
import { FormNavigation } from '@/features/planning/forms/FormNavigation.tsx';
import { useFormStepNavigation } from '@/features/planning/hooks/useFormStepNavigation';
import { fallbackRender } from '@/utils/fallbackRender';
import { KeyboardEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { FormProvider } from 'react-hook-form';
import { useWasteStreamForm } from '../hooks/useWasteStreamFormHook';
import { WasteStreamFormRouteSection } from './WasteStreamFormRouteSection';
import { WasteStreamFormGoodsSection } from './WasteStreamFormGoodsSection';

interface WasteStreamFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  wasteStreamNumber?: string;
}

export const WasteStreamForm = ({
  isOpen,
  setIsOpen,
  wasteStreamNumber,
}: WasteStreamFormProps) => {
  const {
    data,
    isLoading,
    formContext,
    fieldsToValidate,
    mutation,
  } = useWasteStreamForm(wasteStreamNumber, () => setIsOpen(false));
  const { step, navigateToStep, next, back, reset, isSubmitting } =
    useFormStepNavigation({
      formContext,
      fieldsToValidate,
      onSubmit: async (data) => {
        await mutation.mutateAsync(data);
      },
      isEditMode: !!wasteStreamNumber,
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
    <WasteStreamFormRouteSection />,
    <WasteStreamFormGoodsSection />,
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
                  data ? `Afvalstroomnummer ${data.wasteStreamNumber}` : 'Nieuw Afvalstroomnummer'
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
                    'Route details',
                    'Afval details',
                  ]}
                />
                {isLoading ? (
                  <div className="flex justify-center items-center w-full p-8">
                    <p>Afvalstroomnummer laden...</p>
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
