import { Tag, TagColor } from '@/components/ui/tag/Tag';

export interface LMADeclarationStatusTagProps {
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
}

export const LMADeclarationStatusTag = ({ status }: LMADeclarationStatusTagProps) => {
  const color = {
    PENDING: TagColor.YELLOW,
    COMPLETED: TagColor.GREEN,
    FAILED: TagColor.RED,
  }[status];

  const text = {
    PENDING: 'In behandeling',
    COMPLETED: 'Voltooid',
    FAILED: 'Mislukt',
  }[status];
  return <Tag color={color} text={text} />;
};
