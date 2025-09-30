import { FormEvent, RefObject, useEffect, useState } from 'react';
import {
  FieldErrors,
  FieldValues,
  UseFormHandleSubmit,
  UseFormRegister,
  useForm,
} from 'react-hook-form';
import SignatureCanvas from 'react-signature-canvas';
import { CreateSignatureRequest } from '@/api/client/models/create-signature-request';
import { supabase } from '@/api/supabaseClient';
import { toastService } from '@/components/ui/toast/toastService';
import { useErrorHandling } from '@/hooks/useErrorHandling';
import { signatureService } from '@/api/services/signatureService';

interface SignatureFormValues extends FieldValues {
  email: string;
}

interface UseSignatureFormProps {
  id?: string;
  type?: string;
  party?: { name: string };
  onSuccess?: () => void;
}

interface UseSignatureFormReturn {
  register: UseFormRegister<SignatureFormValues>;
  handleSubmit: UseFormHandleSubmit<SignatureFormValues>;
  errors: FieldErrors<SignatureFormValues>;
  isSubmitting: boolean;
  isLoadingSignature: boolean;
  signatureError: string | undefined;
  submitForm: (e: FormEvent<HTMLFormElement>) => Promise<void>;
  setSignatureError: (error: string | undefined) => void;
}

export const useSignatureForm = (
  signatureCanvasRef: RefObject<SignatureCanvas>,
  { id, type, party, onSuccess }: UseSignatureFormProps
): UseSignatureFormReturn => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoadingSignature, setIsLoadingSignature] = useState(false);
  const [signatureUrl, setSignatureUrl] = useState<string | null>(null);
  const [signatureError, setSignatureError] = useState<string | undefined>();
  const { handleError } = useErrorHandling();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SignatureFormValues>();

  useEffect(() => {
    const loadSignatureFromStorage = async () => {
      if (!id || !type) return;

      setIsLoadingSignature(true);
      try {
        const { data, error } = await supabase.storage
          .from('waybills')
          .download(`signatures/${id}/${type}.png`);

        if (error) {
          console.log('Signature not found or error loading:', error);
          return;
        }

        if (data) {
          const url = URL.createObjectURL(data);
          setSignatureUrl(url);

          if (signatureCanvasRef.current) {
            const reader = new FileReader();
            reader.onload = () => {
              const dataUrl = reader.result as string;
              if (signatureCanvasRef.current) {
                signatureCanvasRef.current.fromDataURL(dataUrl);
              }
            };
            reader.readAsDataURL(data);
          }
        }
      } catch (error) {
        console.error('Error loading signature:', error);
      } finally {
        setIsLoadingSignature(false);
      }
    };

    loadSignatureFromStorage();

    return () => {
      if (signatureUrl) {
        URL.revokeObjectURL(signatureUrl);
      }
    };
  }, [id, type, signatureCanvasRef]);

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSignatureError(undefined);

    if (!signatureCanvasRef.current || signatureCanvasRef.current.isEmpty()) {
      setSignatureError('Handtekening is verplicht');
      return;
    }

    await handleSubmit(async (data) => {
      if (!id || !type || !party) {
        toastService.error('Er is een fout opgetreden');
        return;
      }

      try {
        setIsSubmitting(true);

        const signatureRequest: CreateSignatureRequest = {
          signature: signatureCanvasRef.current!.toDataURL('image/png'),
          email: data.email,
          party: type,
        };

        await signatureService.saveSignature(id, signatureRequest);

        toastService.success('Handtekening succesvol opgeslagen');

        if (onSuccess) {
          onSuccess();
        }
      } catch (error) {
        console.error('Error submitting signature:', error);
        handleError(error);
      } finally {
        setIsSubmitting(false);
      }
    })(e);
  };

  return {
    register,
    handleSubmit,
    errors,
    isSubmitting,
    isLoadingSignature,
    signatureError,
    submitForm,
    setSignatureError,
  };
};
