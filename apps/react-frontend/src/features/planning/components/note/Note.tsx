
import Warning from '@/assets/icons/Warning.svg?react';

interface NoteProps {
  note: string;
}

export const Note = ({ note }: NoteProps) => {
  return (
    <div
      className={
        'flex items-center self-stretch gap-2 py-2 pl-2 pr-3 border border-solid border-color-status-warning-primary bg-color-status-warning-light rounded-radius-sm'
      }
    >
      <Warning />
      <span className={'text-body-2'}>{note}</span>
    </div>
  );
};