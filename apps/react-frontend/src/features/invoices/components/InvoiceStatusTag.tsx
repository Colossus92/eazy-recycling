import { Tag, TagColor } from "@/components/ui/tag/Tag";

interface InvoiceStatusTagProps {
  status: 'DRAFT' | 'FINAL';
}

export const InvoiceStatusTag = ({ status }: InvoiceStatusTagProps) => {
  const color = {
    DRAFT: TagColor.YELLOW,
    SENT: TagColor.GRAY,
    FINAL: TagColor.GREEN,
  }[status];

  const text = {
    DRAFT: 'Concept',
    SENT: 'Verzonden',
    FINAL: 'Definitief',
  }[status];
  return <Tag color={color} text={text} />;
};
