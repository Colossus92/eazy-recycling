import { Tag, TagColor } from '@/components/ui/tag/Tag';

export interface WeightTicketStatusTagProps {
  status: 'DRAFT' | 'COMPLETED' | 'INVOICED' | 'CANCELLED';
}

export const WeightTicketStatusTag = ({ status }: WeightTicketStatusTagProps) => {
  const color = {
    DRAFT: TagColor.YELLOW,
    COMPLETED: TagColor.BLUE,
    INVOICED: TagColor.GREEN,
    CANCELLED: TagColor.GRAY,
  }[status];

  const text = {
    DRAFT: 'Concept',
    COMPLETED: 'Verwerkt',
    INVOICED: 'Factureerd',
    CANCELLED: 'Geannuleerd',
  }[status];
  return <Tag color={color} text={text} />;
};
