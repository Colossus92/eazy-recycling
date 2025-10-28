import { JdenticonAvatar } from '../icon/JdenticonAvatar';
import { formatInstantInCET } from '@/utils/dateUtils';
import { NormalizedAddress } from '@/api/services/transportService';

interface CompanyCardProps {
  dateTime?: string;
  details: NormalizedAddress | null;
}

export const CompanyCard = ({
  dateTime,
  details,
}: CompanyCardProps) => {
  return (
    <div
      className={
        'flex flex-1 items-center gap-2 h-16 p-3 border border-solid border-color-border-primary rounded-radius-md'
      }
    >
      {details?.companyName && <JdenticonAvatar value={details.companyName} size={44} />}
      <div className={'flex flex-col justify-center items-start gap-1 flex-1'}>
        <div className={'flex justify-between items-center self-stretch'}>
          <span className="subtitle-2">{details?.companyName}</span>
          <span className="text-body-2 text-color-text-secondary">
            {dateTime ? formatInstantInCET(dateTime, 'dd-MM-yyyy HH:mm') : ''}
          </span>
        </div>
        <span
          className={'text-caption-1 text-color-text-secondary'}
        >{`${details?.street} ${details?.houseNumber}, ${details?.city}`}</span>
      </div>
    </div>
  );
};
