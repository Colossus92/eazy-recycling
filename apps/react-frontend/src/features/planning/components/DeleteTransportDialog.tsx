import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog.tsx';
import { PlanningItem } from '@/features/planning/hooks/usePlanning';

interface DeleteTransportDialogProps {
  isOpen: boolean;
  setIsOpen: (isOpen: boolean) => void;
  onDelete: () => void;
  transport: PlanningItem | null;
}

export const DeleteTransportDialog = ({
  isOpen,
  setIsOpen,
  onDelete,
  transport,
}: DeleteTransportDialogProps) => {
  if (!transport) return null;

  const displayName =
    transport.displayNumber ||
    `${transport.originCity} → ${transport.destinationCity}`;

  return (
    <DeleteDialog
      isOpen={isOpen}
      setIsOpen={setIsOpen}
      onDelete={onDelete}
      title="Transport verwijderen"
      description={`Weet u zeker dat u transport ${displayName} wilt verwijderen?`}
    />
  );
};
