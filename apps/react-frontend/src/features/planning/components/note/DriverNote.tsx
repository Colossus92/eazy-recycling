
import TruckTrailer from '@/assets/icons/TruckTrailer.svg?react';

interface DriverNoteProps {
  note: string;
}

export const DriverNote = ({ note }: DriverNoteProps) => {
  return (
    <div
      className={
        'flex items-center self-stretch gap-2 py-2 pl-2 pr-3 border border-solid border-color-status-info-primary bg-color-status-info-light rounded-radius-sm'
      }
    >
      <TruckTrailer className='text-color-status-info-primary' />
      <span className={'text-body-2'}>{note}</span>
    </div>
  );
};