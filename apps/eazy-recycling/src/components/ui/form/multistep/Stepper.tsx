import { StepperStep } from '@/components/ui/form/multistep/StepperStep.tsx';

interface StepperProps {
  step: number;
  stepDescriptions: string[];
  navigateToStep: (step: number) => void;
}

export const Stepper = ({
  step,
  stepDescriptions,
  navigateToStep,
}: StepperProps) => (
  <div className={'flex items-center self-stretch gap-3'}>
    {stepDescriptions.map((stepDescription, index) => (
      <StepperStep
        key={index}
        step={(index + 1).toString()}
        title={stepDescription}
        active={step === index}
        passed={index < step}
        last={index === stepDescriptions.length - 1}
        onClick={() => navigateToStep(index)}
      />
    ))}
  </div>
);
