import { Button } from '@/components/ui/button/Button.tsx';

export function fallbackRender({
  error,
  resetErrorBoundary,
}: {
  error: Error;
  resetErrorBoundary: () => void;
}) {
  return (
    <div
      role="alert"
      className="flex-1 flex flex-col items-center self-stretch p-4 gap-5"
    >
      <p>Er is iets fout gegaan:</p>
      <pre style={{ color: 'red' }}>{error.message}</pre>
      <Button
        onClick={resetErrorBoundary}
        variant="primary"
        label="Probeer opnieuw"
      />
    </div>
  );
}
