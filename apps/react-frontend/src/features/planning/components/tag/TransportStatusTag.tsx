import { Tag, TagColor } from '@/components/ui/tag/Tag';

interface TransportStatusTagProps {
  status: 'INVOICED' | 'FINISHED' | 'UNPLANNED' | 'ERROR' | 'PLANNED';
}

export const TransportStatusTag = ({ status }: TransportStatusTagProps) => {
  const color = {
    INVOICED: TagColor.GRAY,
    FINISHED: TagColor.GREEN,
    UNPLANNED: TagColor.YELLOW,
    ERROR: TagColor.RED,
    PLANNED: TagColor.BLUE,
  }[status];

  const text = {
    INVOICED: 'Gefactureerd',
    FINISHED: 'Afgerond',
    UNPLANNED: 'Ongepland',
    ERROR: 'Foutmelding',
    PLANNED: 'Gepland',
  }[status];
  return <Tag color={color} text={text} />;
};
