import { useMutation, useQueryClient } from '@tanstack/react-query';
import { weightTicketService } from '@/api/services/weightTicketService';
import { toastService } from '@/components/ui/toast/toastService';
import { useNavigate } from 'react-router-dom';

export function useCreateWeightTicketFromTransport() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const createMutation = useMutation({
    mutationFn: (transportId: string) =>
      weightTicketService.createFromTransport(transportId),
    onSuccess: (result) => {
      toastService.success('Weegbon aangemaakt');
      queryClient.invalidateQueries({ queryKey: ['planning'] });
      navigate(`/weight-tickets?weightTicketId=${result.weightTicketId}`);
    },
  });

  const createWeightTicket = async (transportId: string) => {
    return createMutation.mutateAsync(transportId);
  };

  return {
    createWeightTicket,
    isCreating: createMutation.isPending,
  };
}
