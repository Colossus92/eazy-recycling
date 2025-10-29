import Plus from '@/assets/icons/Plus.svg?react';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';
import { WeightTicketDetailView } from '@/api/client/models';

interface WeightTicketFormActionMenuProps {
    weightTicket: WeightTicketDetailView;
    onDelete: (id: number) => void;
}

export const WeightTicketFormActionMenu = ({
    weightTicket,
    onDelete,
}: WeightTicketFormActionMenuProps) => {
    return (<FormActionMenu
        onDelete={() => onDelete(weightTicket.id)}
        additionalActions={[
            {
                label: 'Splitsen',
                icon: Plus,
                onClick: () => {},
            },
        ]}
    />);
}