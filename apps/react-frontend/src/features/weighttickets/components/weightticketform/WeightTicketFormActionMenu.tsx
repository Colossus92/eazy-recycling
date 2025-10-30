import Plus from '@/assets/icons/Plus.svg?react';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';
import { WeightTicketDetailView } from '@/api/client/models';

interface WeightTicketFormActionMenuProps {
    weightTicket: WeightTicketDetailView;
    onDelete: (id: number) => void;
    onSplit?: (id: number) => void;
}

export const WeightTicketFormActionMenu = ({
    weightTicket,
    onDelete,
    onSplit,
}: WeightTicketFormActionMenuProps) => {
    const canSplit = weightTicket.status === 'DRAFT' && onSplit;
    
    return (<FormActionMenu
        onDelete={() => onDelete(weightTicket.id)}
        additionalActions={canSplit ? [
            {
                label: 'Splitsen',
                icon: Plus,
                onClick: () => onSplit(weightTicket.id),
            },
        ] : []}
    />);
}