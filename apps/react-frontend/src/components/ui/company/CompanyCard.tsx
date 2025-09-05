import { format } from 'date-fns';
import { JdenticonAvatar } from '../icon/JdenticonAvatar';
import { Address } from '@/types/api';

interface CompanyCardProps {
  companyName?: string;
  dateTime?: string;
  address: Address;
}

export const CompanyCard = ({
  companyName,
  dateTime,
  address,
}: CompanyCardProps) => {
  return (
    <div
      className={
        'flex flex-1 items-center gap-2 h-16 p-3 border border-solid border-color-border-primary rounded-radius-md'
      }
    >
      {companyName && <JdenticonAvatar value={companyName} size={44} />}
      <div className={'flex flex-col justify-center items-start gap-1 flex-1'}>
        <div className={'flex justify-between items-center self-stretch'}>
          <span className="subtitle-2">{companyName}</span>
          <span className="text-body-2 text-color-text-secondary">
            {dateTime ? format(new Date(dateTime), 'dd-MM-yyyy') : ''}
          </span>
        </div>
        <span
          className={'text-caption-1 text-color-text-secondary'}
        >{`${address.streetName} ${address.buildingNumber}, ${address.city}`}</span>
      </div>
    </div>
  );
};
