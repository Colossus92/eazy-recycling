import { Button } from '@/components/ui/button/Button.tsx';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';

interface FormNavigationProps {
  step: number;
  totalSteps: number;
  onNext?: () => Promise<void>;
  onBack?: () => void;
  onCancel?: () => void;
  isSubmitting?: boolean;
}

export const FormNavigation = ({
  step,
  totalSteps,
  onNext,
  onBack,
  onCancel,
  isSubmitting = false,
}: FormNavigationProps) => {
  return (
    <div className="flex py-3 px-4 justify-end items-center self-stretch gap-4 border-t border-solid border-color-border-primary">
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
      <div className={'flex-1'}>
        {step === totalSteps ? (
          <Button
            variant={'primary'}
            label={isSubmitting ? 'Opslaan...' : 'Opslaan'}
            onClick={onNext}
            fullWidth={true}
            disabled={isSubmitting}
          />
        ) : (
          <Button
            variant={'primary'}
            label={'Volgende'}
            onClick={onNext}
            icon={CaretRight}
            iconPosition={'right'}
            fullWidth={true}
            disabled={isSubmitting}
          />
        )}
      </div>
    </div>
  );
};
