import { Button } from '@/components/ui/button/Button.tsx';
import Plus from '@/assets/icons/Plus.svg?react';

interface EmptyStateProps {
  icon: React.FunctionComponent<React.SVGProps<SVGSVGElement>>;
  text: string;
  onClick?: () => void;
}

export const EmptyState = ({ icon: Icon, text, onClick}: EmptyStateProps) => {
  return (
    <div className="flex flex-col flex-[1_0_0] justify-center items-center self-stretch gap-3 border-t border-solid border-color-border-primary">
      <div className="flex flex-col items-center gap-5">
        <div className="flex content-center items-center h13 py-2.5 px-3 bg-color-surface-secondary border border-solid border-color-border-primary rounded-radius-md">
          <Icon className="h-7.5 w-7.5" />
        </div>
        <div className="flex flex-col items-center self-stretch gap-3">
          <span>{text}</span>
        </div>
        {onClick && <Button
          variant={'secondary'}
          label={'Voeg toe'}
          icon={Plus}
          onClick={onClick}
        />}
      </div>
    </div>
  );
};
