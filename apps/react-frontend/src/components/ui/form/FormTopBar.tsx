import { Button } from '@/components/ui/button/Button.tsx';
import X from '@/assets/icons/X.svg?react';

export interface FormTopBarProps {
  onClick: () => void;
  title: string;
  actions?: React.ReactNode;
}

export const FormTopBar = ({ onClick, title, actions }: FormTopBarProps) => {
  return (
    <div className="flex py-3 px-4 items-center gap-4 self-stretch border-b border-solid border-color-border-primary">
      <div className="flex-[1_0_0]">
        <h4>{title}</h4>
      </div>
      <div className="flex items-center gap-2">
        {actions}
        <Button
          icon={X}
          showText={false}
          variant="icon"
          iconPosition="right"
          onClick={onClick}
          data-testid={'close-button'}
        />
      </div>
    </div>
  );
};
