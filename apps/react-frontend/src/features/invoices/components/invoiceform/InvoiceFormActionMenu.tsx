import Eraser from '@/assets/icons/Eraser.svg?react';
import IcBaselineContentCopy from '@/assets/icons/IcBaselineContentCopy.svg?react';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';

interface InvoiceFormActionMenuProps {
  onDelete: () => void;
  onCreateCredit: () => void;
  onCopy: () => void;
  isReadOnly: boolean;
  isCreditNote: boolean;
}

export const InvoiceFormActionMenu = ({
  onDelete,
  onCreateCredit,
  onCopy,
  isReadOnly,
  isCreditNote,
}: InvoiceFormActionMenuProps) => {
  const additionalActions = [];

  // Copy is always available for all invoice statuses
  additionalActions.push({
    label: 'Kopiëren',
    icon: IcBaselineContentCopy,
    onClick: onCopy,
  });

  // Credit note creation only for finalized invoices that are not already credit notes
  if (isReadOnly && !isCreditNote) {
    additionalActions.push({
      label: 'Creditnota aanmaken',
      icon: Eraser,
      onClick: onCreateCredit,
    });
  }

  return (
    <FormActionMenu
      onDelete={onDelete}
      additionalActions={
        additionalActions.length > 0 ? additionalActions : undefined
      }
      showDelete={!isReadOnly}
    />
  );
};
