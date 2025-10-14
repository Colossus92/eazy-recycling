import { Tag, TagColor } from '@/components/ui/tag/Tag';

interface WasteStreamStatusTagProps {
  status: 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'EXPIRED';
}

export const WasteStreamStatusTag = ({ status }: WasteStreamStatusTagProps) => {
  const color = {
    DRAFT: TagColor.YELLOW,
    ACTIVE: TagColor.GREEN,
    INACTIVE: TagColor.RED,
    EXPIRED: TagColor.GRAY,
  }[status];

  const text = {
    DRAFT: 'Concept',
    ACTIVE: 'Actief',
    INACTIVE: 'Inactief',
    EXPIRED: 'Verlopen',
  }[status];
  return <Tag color={color} text={text} />;
};
