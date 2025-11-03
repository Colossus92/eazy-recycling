import { FieldValues, UseFormReturn, Path } from 'react-hook-form';
import { useState } from 'react';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { AxiosError } from 'axios';

interface UseFormStepNavigationProps<T extends FieldValues> {
  formContext: UseFormReturn<T>;
  fieldsToValidate: Array<Array<Path<T>>>;
  onSubmit: (data: T) => Promise<void> | void;
  isEditMode?: boolean;
  isSubmitting?: boolean;
}

export function useFormStepNavigation<T extends FieldValues>({
  formContext,
  fieldsToValidate,
  onSubmit,
  isEditMode = false,
  isSubmitting = false,
}: UseFormStepNavigationProps<T>) {
  const [step, setStep] = useState(0);
  const navigateToStep = async (stepToNavigateTo: number) => {
    const isValid = await formContext.trigger(fieldsToValidate[step]);

    if (isValid) {
      if (stepToNavigateTo < fieldsToValidate.length) {
        setStep(stepToNavigateTo);
      } else {
        console.error(`Form step ${stepToNavigateTo} does not exist`);
        toastService.error(`Formulier stap ${stepToNavigateTo} bestaat niet`);
      }
    } else {
      toastService.error(`Vul alle verplichte velden in`);
      return;
    }
  };

  const next = async () => {
    const isValid = await formContext.trigger(fieldsToValidate[step]);

    if (isValid) {
      if (step < fieldsToValidate.length - 1) {
        setStep(step + 1);
      } else {
        try {
          await onSubmit(formContext.getValues());
          setStep(0);
        } catch (error) {
          console.error('Error submitting form:', error);
        }
      }
    } else {
      toastService.error(`Vul alle verplichte velden in`);
      return;
    }
  };

  const back = () => setStep(Math.max(0, step - 1));

  const reset = () => {
    formContext.reset();
    setStep(0);
  };

  return {
    step,
    navigateToStep,
    next,
    back,
    reset,
    isSubmitting,
  };
}
