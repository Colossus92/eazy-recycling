import { Button } from '@/components/ui/button/Button.tsx';
import { SplitButton } from '@/components/ui/button/SplitButton.tsx';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';

interface FormNavigationWithDraftProps {
  step: number;
  totalSteps: number;
  onNext?: () => Promise<void>;
  onBack?: () => void;
  onCancel?: () => void;
  onSaveDraft?: () => Promise<void>;
  onSaveAndValidate?: () => Promise<void>;
  isSubmitting?: boolean;
}

export const FormNavigationWithDraft = ({
  step,
  totalSteps,
  onNext,
  onBack,
  onCancel,
  onSaveDraft,
  onSaveAndValidate,
  isSubmitting = false,
}: FormNavigationWithDraftProps) => {
  return (
    <div className="flex py-3 px-4 justify-end items-center self-stretch gap-4 border-t border-solid border-color-border-primary relative z-0">
      <div className={'flex-1'}>
        {step === 0 ? (
          <Button
            variant={'secondary'}
            label={'Annuleren'}
            onClick={onCancel}
            fullWidth={true}
          />
        ) : (
          <Button
            variant={'secondary'}
            label={'Terug'}
            onClick={onBack}
            icon={CaretLeft}
            iconPosition={'left'}
            fullWidth={true}
          />
        )}
      </div>
      {step === totalSteps ? (
        <div className={'flex-1'}>
          <SplitButton
            primaryLabel={isSubmitting ? 'Valideren...' : 'Opslaan en valideren'}
            secondaryLabel={isSubmitting ? 'Opslaan...' : 'Opslaan als concept'}
            onPrimaryClick={() => onSaveAndValidate?.()}
            onSecondaryClick={() => onSaveDraft?.()}
            disabled={isSubmitting}
            isSubmitting={isSubmitting}
            fullWidth={true}
          />
        </div>
      ) : (
        <div className={'flex-1'}>
          <Button
            variant={'primary'}
            label={'Volgende'}
            onClick={onNext}
            icon={CaretRight}
            iconPosition={'right'}
            fullWidth={true}
            disabled={isSubmitting}
            data-testid="next-button"
          />
        </div>
      )}
    </div>
  );
};
