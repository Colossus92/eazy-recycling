import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { Stepper } from '@/components/ui/form/multistep/Stepper.tsx';
import { FormNavigationWithDraft } from '@/features/planning/forms/FormNavigationWithDraft.tsx';
import { useFormStepNavigation } from '@/features/planning/hooks/useFormStepNavigation';
import { fallbackRender } from '@/utils/fallbackRender';
import { KeyboardEvent, useEffect, useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { FormProvider } from 'react-hook-form';
import { useWasteStreamForm } from '../hooks/useWasteStreamFormHook';
import { WasteStreamFormRouteSection } from './WasteStreamFormRouteSection';
import { WasteStreamFormGoodsSection } from './WasteStreamFormGoodsSection';
import { Note } from '@/features/planning/components/note/Note';
import { ValidationError } from '@/api/client/models/validation-error';

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
    draftMutation,
    validateMutation,
    resetForm,
  } = useWasteStreamForm(wasteStreamNumber, () => setIsOpen(false));

  const [validationErrors, setValidationErrors] = useState<ValidationError[]>(
    []
  );
  const isSubmitting = draftMutation.isPending || validateMutation.isPending;

  const {
    step,
    navigateToStep,
    next,
    back,
    reset: resetStepper,
  } = useFormStepNavigation({
    formContext,
    fieldsToValidate,
    onSubmit: async () => {
      // This will be called by the navigation buttons
      // but we'll handle the actual submission in handleSaveDraft and handleSaveAndValidate
    },
    isSubmitting,
  });

  // Reset step when form opens
  useEffect(() => {
    if (isOpen) {
      resetStepper();
    }
  }, [isOpen, resetStepper]);

  const onCancel = () => {
    resetForm();
    resetStepper();
    setValidationErrors([]);
    setIsOpen(false);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLFormElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      next();
    }
  };

  const handleSaveDraft = async () => {
    setValidationErrors([]); // Clear any previous validation errors
    const isValid = await formContext.trigger();
    if (isValid) {
      const formData = formContext.getValues();
      await draftMutation.mutateAsync(formData);
    }
  };

  const handleSaveAndValidate = async () => {
    setValidationErrors([]); // Clear any previous validation errors
    const isValid = await formContext.trigger();
    if (isValid) {
      const formData = formContext.getValues();
      try {
        await validateMutation.mutateAsync(formData);
      } catch (error) {
        // Check if error is validation errors array from 409 response
        if (Array.isArray(error)) {
          setValidationErrors(error);
        }
        // Other errors are already handled by the mutation's onError
      }
    }
  };

  const steps = [
    <WasteStreamFormRouteSection />,
    <WasteStreamFormGoodsSection />,
  ];

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <FormDialog isOpen={isOpen} setIsOpen={onCancel}>
        <div className={'w-full h-full'}>
          <FormProvider {...formContext}>
            <form
              className="flex flex-col items-center self-stretch h-full"
              onSubmit={(e) => e.preventDefault()}
              onKeyDown={handleKeyDown}
            >
              <FormTopBar
                title={
                  data
                    ? `Afvalstroomnummer ${data.wasteStreamNumber}`
                    : 'Nieuw Afvalstroomnummer'
                }
                onClick={onCancel}
              />
              <div
                className={
                  'flex flex-col items-start self-stretch gap-5 p-4 max-h-[calc(100vh-200px)] overflow-y-auto'
                }
              >
                <Stepper
                  navigateToStep={navigateToStep}
                  step={step}
                  stepDescriptions={['Route details', 'Afval details']}
                />
                {validationErrors.length > 0 && (
                  <div className="flex flex-col gap-2 w-full">
                    {validationErrors.map((error, index) => (
                      <Note
                        key={index}
                        note={`${error.code}: ${error.description}`}
                      />
                    ))}
                  </div>
                )}
                {isLoading ? (
                  <div className="flex justify-center items-center w-full p-8">
                    <p>Afvalstroomnummer laden...</p>
                  </div>
                ) : (
                  steps[step]
                )}
              </div>
              <FormNavigationWithDraft
                step={step}
                totalSteps={steps.length - 1}
                onNext={next}
                onBack={back}
                onCancel={onCancel}
                onSaveDraft={handleSaveDraft}
                onSaveAndValidate={handleSaveAndValidate}
                isSubmitting={isSubmitting}
              />
            </form>
          </FormProvider>
        </div>
      </FormDialog>
    </ErrorBoundary>
  );
};
