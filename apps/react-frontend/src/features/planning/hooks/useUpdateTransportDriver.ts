import { useMutation, useQueryClient } from '@tanstack/react-query';
import { transportService } from '@/api/services/transportService';
import { toastService } from '@/components/ui/toast/toastService';

export const useUpdateTransportDriver = () => {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: ({
      transportId,
      driverId,
    }: {
      transportId: string;
      driverId?: string;
    }) => transportService.updateDriver(transportId, { driverId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['planning'] });
      queryClient.invalidateQueries({ queryKey: ['driver-planning'] });
      toastService.success('Chauffeur succesvol gewijzigd');
    },
    onError: (error: any) => {
      const message =
        error?.response?.data?.message || 'Fout bij wijzigen chauffeur';
      toastService.error(message);
    },
  });

  return {
    updateDriver: mutation.mutate,
    isUpdating: mutation.isPending,
  };
};
