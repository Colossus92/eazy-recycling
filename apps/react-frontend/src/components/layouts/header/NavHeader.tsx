import { SVGProps } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { fallbackRender } from '@/utils/fallbackRender';
import { AvatarMenu } from '@/components/layouts/header/AvatarMenu.tsx';
import { Button } from '@/components/ui/button/Button.tsx';
import IcBaselineHelpOutline from '@/assets/icons/IcBaselineHelpOutline.svg?react';

// Create a styled version of the icon with the correct size
const StyledHelpIcon = (props: SVGProps<SVGSVGElement>) => (
  <IcBaselineHelpOutline width="20" height="20" {...props} />
);

interface HeaderProps {
  pageName: string;
}

export const NavHeader = ({ pageName }: HeaderProps) => {
  return (
    <div className="flex justify-between items-center self-stretch px-4 py-3 border-t border-t-transparent border-b border-solid border-b-primary leading-6">
      <ErrorBoundary fallbackRender={fallbackRender}>
        <div>
          <h3>{pageName}</h3>
        </div>
        <div className={'flex items-start gap-4'}>
          <Button
            icon={StyledHelpIcon}
            variant="icon"
            size="small"
            showText={false}
            onClick={(e) => {
              e.preventDefault();
              window.open('/manual/manual.html', '_blank');
            }}
          />
          <AvatarMenu />
        </div>
      </ErrorBoundary>
    </div>
  );
};
