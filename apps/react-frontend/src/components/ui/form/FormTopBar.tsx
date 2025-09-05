import { Button } from '@/components/ui/button/Button.tsx';
import X from '@/assets/icons/X.svg?react';

export const FormTopBar = (props: { onClick: () => void; title: string }) => {
  return (
    <div className="flex py-3 px-4 items-center gap-4 self-stretch border-b border-solid border-color-border-primary">
      <div className="flex-[1_0_0]">
        <h4>{props.title}</h4>
      </div>
      <Button
        icon={X}
        showText={false}
        variant="icon"
        iconPosition="right"
        onClick={props.onClick}
        data-testid={'close-button'}
      />
    </div>
  );
};
