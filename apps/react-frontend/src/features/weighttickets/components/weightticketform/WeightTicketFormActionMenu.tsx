import Plus from '@/assets/icons/Plus.svg?react';
import CheckCircleOutline from '@/assets/icons/CheckCircleOutline.svg?react';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';
import { WeightTicketDetailView } from '@/api/client/models';

interface WeightTicketFormActionMenuProps {
    weightTicket: WeightTicketDetailView;
    onDelete: (id: number) => void;
    onSplit?: (id: number) => void;
    onComplete?: (id: number) => void;
}

export const WeightTicketFormActionMenu = ({
    weightTicket,
    onDelete,
    onSplit,
    onComplete,
}: WeightTicketFormActionMenuProps) => {
    const actions = [];
    if (weightTicket.status === 'DRAFT' && onSplit) {
        actions.push(
            {
                label: 'Splitsen',
                icon: Plus,
                onClick: () => onSplit(weightTicket.id),
            }
        );
    }
    if (weightTicket.status === 'DRAFT' && onComplete) {
        actions.push(
            {
                label: 'Voltooien',
                icon: CheckCircleOutline,
                onClick: () => onComplete(weightTicket.id),
            }
        );
    }
    
    return (<FormActionMenu
        onDelete={() => onDelete(weightTicket.id)}
        additionalActions={actions}
    />);
}