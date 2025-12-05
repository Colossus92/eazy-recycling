import { Tag, TagColor } from '@/components/ui/tag/Tag';

export interface LMADeclarationStatusTagProps {
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'WAITING_APPROVAL';
}

export const LMADeclarationStatusTag = ({ status }: LMADeclarationStatusTagProps) => {
  const color = {
    PENDING: TagColor.YELLOW,
    COMPLETED: TagColor.GREEN,
    FAILED: TagColor.RED,
    WAITING_APPROVAL: TagColor.BLUE,
  }[status];

  const text = {
    PENDING: 'In behandeling',
    COMPLETED: 'Voltooid',
    FAILED: 'Mislukt',
    WAITING_APPROVAL: 'Wacht op goedkeuring',
  }[status];
  return <Tag color={color} text={text} />;
};
