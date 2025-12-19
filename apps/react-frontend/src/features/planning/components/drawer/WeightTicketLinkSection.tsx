import { useNavigate } from 'react-router-dom';
import Scale from '@/assets/icons/Scale.svg?react';

interface WeightTicketLinkSectionProps {
  weightTicketId?: number;
}

export const WeightTicketLinkSection = ({
  weightTicketId,
}: WeightTicketLinkSectionProps) => {
  const navigate = useNavigate();

  if (!weightTicketId) return null;

  return (
    <div 
      className={'flex items-center gap-2 self-stretch cursor-pointer hover:bg-color-surface-secondary rounded-radius-md -mx-2 px-2 py-1'}
      onClick={() => navigate(`/weight-tickets?weightTicketId=${weightTicketId}`)}
    >
      <div className="flex items-center flex-1 gap-2">
        <Scale
          className={'w-5 h-5 text-color-text-secondary'}
        />
        <span className={'text-body-2 text-color-text-secondary'}>
          Weegbon
        </span>
      </div>
      <span className={'text-body-2 truncate text-color-brand-primary underline'}>
        {weightTicketId}
      </span>
    </div>
  );
};
