import { useQuery } from '@tanstack/react-query';
import { signatureService } from '@/api/services/signatureService';
import { SignatureStatusView } from '@/api/client/models/signature-status-view';

interface UseSignatureStatusProps {
  transportId: string;
}

export const useSignatureStatus = ({
  transportId,
}: UseSignatureStatusProps) => {
  const { data, isLoading, error, refetch } = useQuery<SignatureStatusView>({
    queryKey: ['signatureStatus', transportId],
    queryFn: () => signatureService.getSignatureStatus(transportId),
    enabled: !!transportId,
  });

  const totalSignatures = 4;
  const collectedSignatures = data
    ? [
        data.consignorSigned,
        data.carrierSigned,
        data.consigneeSigned,
        data.pickupSigned,
      ].filter(Boolean).length
    : 0;

  const progressPercentage = Math.round(
    (collectedSignatures / totalSignatures) * 100
  );

  return {
    signatureStatus: data,
    isLoading,
    error,
    refetch,
    totalSignatures,
    collectedSignatures,
    progressPercentage,
  };
};
