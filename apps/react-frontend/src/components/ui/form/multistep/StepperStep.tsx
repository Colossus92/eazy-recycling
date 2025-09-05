import clsx from 'clsx';
import CheckCircle from '@/assets/icons/CheckCircle.svg?react';

interface StepperStepProps {
  title: string;
  step: string;
  active: boolean;
  last: boolean;
  passed: boolean;
  onClick: () => void;
}

export const StepperStep = ({
  title,
  step,
  active,
  last,
  passed,
  onClick,
}: StepperStepProps) => {
  const baseClasses =
    'flex items-center justify-center w-8 h-8 rounded-full subtitle-1';
  const formattingClasses = active
    ? 'bg-color-brand-primary text-color-text-invert-primary'
    : 'border border-color-border-primary ';

  return (
    <>
      <div
        className={'flex items-center gap-2 hover:cursor-pointer'}
        onClick={onClick}
      >
        {passed ? (
          <CheckCircle className={'w-8 h-8'} />
        ) : (
          <div className={clsx(baseClasses, formattingClasses)}>{step}</div>
        )}
        {active && <span className={'subtitle-2'}>{title}</span>}
      </div>
      {!last && <div className="flex-grow h-px bg-color-border-primary"></div>}
    </>
  );
};
