import { useMutation, useQueryClient } from '@tanstack/react-query';
import { lmaImportService } from '@/api/services/lmaImportService';
import { LmaImportResponse } from '@/api/client';

export const useLmaImport = () => {
  const queryClient = useQueryClient();

  const importMutation = useMutation({
    mutationFn: async (file: File) => {
      const response = await lmaImportService.importCsv(file);
      return response.data;
    },
    onSuccess: () => {
      // Invalidate errors query to refresh the list
      queryClient.invalidateQueries({ queryKey: ['lmaImportErrors'] });
    },
  });

  const deleteErrorsMutation = useMutation({
    mutationFn: async () => {
      await lmaImportService.deleteAllErrors();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lmaImportErrors'] });
    },
  });

  return {
    importCsv: (file: File) => importMutation.mutateAsync(file),
    isImporting: importMutation.isPending,
    importResult: importMutation.data as LmaImportResponse | undefined,
    importError: importMutation.error,
    reset: importMutation.reset,
    deleteAllErrors: () => deleteErrorsMutation.mutateAsync(),
    isDeletingErrors: deleteErrorsMutation.isPending,
  };
};
