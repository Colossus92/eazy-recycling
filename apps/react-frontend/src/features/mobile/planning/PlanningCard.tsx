import { useNavigate } from 'react-router-dom';
import { DriverPlanningItemStatusEnum } from '@/api/client/models/driver-planning-item';
import { DriverPlanningItem } from '@/api/client/models/driver-planning-item';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import MapPin from '@/assets/icons/MapPin.svg?react';
import ShippingContainer from '@/assets/icons/ShippingContainer.svg?react';
import { resolveLocationAddress } from '@/api/services/transportService';

interface PlanningCardProps {
  transport: DriverPlanningItem;
  selectedDate?: Date;
}

export const PlanningCard = ({
  transport,
  selectedDate,
  ...props
}: PlanningCardProps) => {
  const navigate = useNavigate();

  const handleCardClick = () => {
    const dateToPass = selectedDate
      ? selectedDate.toISOString()
      : new Date().toISOString();

    sessionStorage.setItem('eazy-recycling-selected-date', dateToPass);

    navigate(`/mobile/transport/${transport.id}`);
  };
  const colors = new Map([
    [
      DriverPlanningItemStatusEnum.Unplanned,
      'border-color-status-warning-primary bg-color-status-warning-light',
    ],
    [DriverPlanningItemStatusEnum.Planned, 'border-color-brand-primary bg-color-status-info-light'],
    [
      DriverPlanningItemStatusEnum.Finished,
      'border-color-status-success-primary bg-color-status-success-light',
    ],
    [DriverPlanningItemStatusEnum.Invoiced, 'border-color-text-disabled bg-color-surface-background'],
  ]);
  const deliveryAddress = resolveLocationAddress(transport.deliveryLocation);
  const pickupAddress = resolveLocationAddress(transport.pickupLocation);

  const deliveryAddressText =
    deliveryAddress?.street +
    ' ' +
    deliveryAddress?.houseNumber +
    ', ' +
    deliveryAddress?.city;

  return (
    <div
      className={`flex flex-col items-start self-stretch pl-1 gap-1 border-solid border-l-4 rounded-radius-md ${colors.get(transport.status)} cursor-pointer`}
      onClick={handleCardClick}
      {...props}
    >
      <div
        className={
          'flex flex-col justify-center items-center self-stretch py-3 pl-4 pr-3 gap-3'
        }
      >
        <div className="flex flex-col items-start self-stretch">
          <span className={'text-subtitle-2 text-color-text-secondary'}>
            #{transport.displayNumber || 'Onbekend'}
          </span>
          <div className={'flex items-center self-stretch gap-1'}>
            <span className={'text-subtitle-1 text-color'}>
              {pickupAddress?.city}
            </span>
            <div className="flex-shrink-0">
              <CaretRight className={'text-color-text-secondary'} />
            </div>
            <span className={'text-subtitle-1'}>
              {deliveryAddress?.city}
            </span>
          </div>
        </div>
        <div className={'flex flex-col items-start self-stretch gap-1'}>
          <div className={'flex items-center self-stretch gap-2'}>
            <MapPin className={'size-5 text-color-text-secondary'} />
            <span className={'text-body-2 text-color-text-primary'}>
              {deliveryAddressText}
            </span>
          </div>
          <div className={'flex items-center self-stretch gap-2'}>
            <ShippingContainer className={'size-5 text-color-text-secondary'} />
            <span className={'text-body-2 text-color-text-primary'}>
              {transport.containerId || '-'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
