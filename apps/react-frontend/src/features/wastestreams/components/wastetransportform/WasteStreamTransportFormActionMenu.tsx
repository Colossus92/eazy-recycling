import Scale from '@/assets/icons/Scale.svg?react';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';

interface WasteStreamTransportFormActionMenuProps {
  transportId: string;
  onDelete: (id: string) => void;
  onCreateWeightTicket?: (id: string) => void;
}

export const WasteStreamTransportFormActionMenu = ({
  transportId,
  onDelete,
  onCreateWeightTicket,
}: WasteStreamTransportFormActionMenuProps) => {
  const actions = [];

  if (onCreateWeightTicket) {
    actions.push({
      label: 'Weegbon aanmaken',
      icon: Scale,
      onClick: () => onCreateWeightTicket(transportId),
    });
  }

  return (
    <FormActionMenu
      onDelete={() => onDelete(transportId)}
      additionalActions={actions}
    />
  );
};
