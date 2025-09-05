import SignatureCanvas from 'react-signature-canvas';
import { ClipLoader } from 'react-spinners';
import Eraser from '@/assets/icons/Eraser.svg?react';

interface SignatureFormFieldProps {
  title: string;
  canvasRef: React.RefObject<SignatureCanvas>;
  errorMessage?: string;
  isLoading?: boolean;
}

export const SignatureFormField = ({
  title,
  canvasRef,
  errorMessage,
  isLoading = false,
}: SignatureFormFieldProps) => {
  const hasError = !!errorMessage;

  const clearSignature = () => {
    if (canvasRef.current) {
      canvasRef.current.clear();
    }
  };

  return (
    <div className="flex flex-1 flex-col items-start self-stretch gap-1">
      <span className="text-subtitle-2">{title}</span>
      <div className="relative w-full">
        {isLoading ? (
          <div className="w-full h-[200px] border border-solid border-color-border-primary rounded-radius-xs flex items-center justify-center">
            <ClipLoader size={40} color="#4A6CF7" />
          </div>
        ) : (
          <SignatureCanvas
            ref={canvasRef}
            canvasProps={{
              className: `w-full h-[200px] border border-solid ${hasError ? 'border-color-status-error-dark' : 'border-color-border-primary'} rounded-radius-xs`,
            }}
          />
        )}
        {!isLoading && (
          <button
            type="button"
            className="absolute top-2 right-2 p-2 bg-color-surface-primary rounded-radius-full border border-solid border-color-border-primary"
            onClick={clearSignature}
          >
            <Eraser className="size-5" />
          </button>
        )}
      </div>
      {hasError && (
        <span className="text-caption-1 text-color-status-error-dark">
          {errorMessage}
        </span>
      )}
    </div>
  );
};
