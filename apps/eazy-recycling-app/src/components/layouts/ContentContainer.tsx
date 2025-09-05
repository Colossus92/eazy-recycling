import { ReactNode } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { fallbackRender } from '@/utils/fallbackRender';
import { NavHeader } from '@/components/layouts/header/NavHeader.tsx';

export const ContentContainer = ({
  children,
  title,
}: {
  children: ReactNode;
  title: string;
}) => {
  return (
    <div className="flex flex-col items-end self-stretch h-screen w-full">
      <NavHeader pageName={title} />
      <div className="flex-1 flex flex-col items-center self-stretch gap-3 p-4 overflow-hidden">
        <ErrorBoundary fallbackRender={fallbackRender}>
          {children}
        </ErrorBoundary>
      </div>
    </div>
  );
};
