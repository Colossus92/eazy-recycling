import { materialPriceSyncService } from '@/api/services/materialPriceSyncService';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { toastService } from '@/components/ui/toast/toastService';

export const useMaterialPriceSync = () => {
  const queryClient = useQueryClient();
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);

  const {
    data: preview,
    isLoading: isLoadingPreview,
    error: previewError,
    refetch: refetchPreview,
  } = useQuery({
    queryKey: ['materialPriceSync', 'preview'],
    queryFn: () => materialPriceSyncService.getSyncPreview(),
    enabled: isPreviewOpen,
  });

  const executeSyncMutation = useMutation({
    mutationFn: () => materialPriceSyncService.executeSyncAsync(),
    onSuccess: (result) => {
      toastService.success(
        `Synchronisatie gestart: ${result.totalJobsEnqueued} taken in wachtrij`
      );
      setIsPreviewOpen(false);
      queryClient.invalidateQueries({ queryKey: ['materialPrices'] });
    },
    onError: () => {
      toastService.error('Synchronisatie mislukt');
    },
  });

  const openPreview = () => {
    setIsPreviewOpen(true);
  };

  const closePreview = () => {
    setIsPreviewOpen(false);
  };

  const executeSync = () => {
    executeSyncMutation.mutate();
  };

  const hasChanges =
    preview &&
    (preview.toCreate.length > 0 ||
      preview.toUpdate.length > 0 ||
      preview.toDelete.length > 0);

  return {
    isPreviewOpen,
    openPreview,
    closePreview,
    preview,
    isLoadingPreview,
    previewError,
    refetchPreview,
    executeSync,
    isExecuting: executeSyncMutation.isPending,
    hasChanges,
  };
};
