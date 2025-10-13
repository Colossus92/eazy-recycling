import { Button } from '@/components/ui/button/Button.tsx';

export function fallbackRender({
  error,
  resetErrorBoundary,
}: {
  error: Error;
  resetErrorBoundary: () => void;
}) {
  const handleReset = () => {
    // Use setTimeout to ensure the reset happens after the current event loop
    setTimeout(() => {
      resetErrorBoundary();
    }, 0);
  };

  return (
    <div
      role="alert"
      className="flex-1 flex flex-col items-center self-stretch p-4 gap-5"
    >
      <p>Er is iets fout gegaan:</p>
      <pre style={{ color: 'red' }}>{error.message}</pre>
      <Button
        onClick={handleReset}
        variant="primary"
        label="Probeer opnieuw"
      />
    </div>
  );
}
