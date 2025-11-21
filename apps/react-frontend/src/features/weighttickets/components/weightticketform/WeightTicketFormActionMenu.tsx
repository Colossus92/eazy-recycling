import Plus from '@/assets/icons/Plus.svg?react';
import CheckCircleOutline from '@/assets/icons/CheckCircleOutline.svg?react';
import IcBaselineContentCopy from '@/assets/icons/IcBaselineContentCopy.svg?react';
import TruckTrailer from '@/assets/icons/TruckTrailer.svg?react';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';
import { WeightTicketDetailView } from '@/api/client/models';

interface WeightTicketFormActionMenuProps {
    weightTicket: WeightTicketDetailView;
    onDelete: (id: number) => void;
    onSplit?: (id: number) => void;
    onCopy?: (id: number) => void;
    onComplete?: (id: number) => void;
    onCreateTransport?: (id: number) => void;
}

export const WeightTicketFormActionMenu = ({
    weightTicket,
    onDelete,
    onSplit,
    onCopy,
    onComplete,
    onCreateTransport,
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
    if (onCopy) {
        actions.push(
            {
                label: 'Kopiëren',
                icon: IcBaselineContentCopy,
                onClick: () => onCopy(weightTicket.id),
            }
        );
    }
    if (onCreateTransport) {
        actions.push(
            {
                label: 'Transport aanmaken',
                icon: TruckTrailer,
                onClick: () => onCreateTransport(weightTicket.id),
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