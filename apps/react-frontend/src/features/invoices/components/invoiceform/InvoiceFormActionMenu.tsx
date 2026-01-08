import Eraser from '@/assets/icons/Eraser.svg?react';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';

interface InvoiceFormActionMenuProps {
  onDelete: () => void;
  onCreateCredit: () => void;
  isReadOnly: boolean;
  isCreditNote: boolean;
}

export const InvoiceFormActionMenu = ({
  onDelete,
  onCreateCredit,
  isReadOnly,
  isCreditNote,
}: InvoiceFormActionMenuProps) => {
  // Determine additional actions based on invoice state
  const additionalActions =
    isReadOnly && !isCreditNote
      ? [
          {
            label: 'Creditnota aanmaken',
            icon: Eraser,
            onClick: onCreateCredit,
          },
        ]
      : undefined;

  return (
    <FormActionMenu
      onDelete={onDelete}
      additionalActions={additionalActions}
      showDelete={!isReadOnly}
    />
  );
};
