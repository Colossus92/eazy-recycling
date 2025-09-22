import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { useEffect } from 'react';
import { WaybillDownloadSection } from './WaybillDownloadSection';
import { transportService } from '@/api/transportService';
import { Drawer } from '@/components/ui/drawer/Drawer';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import CheckCircle from '@/assets/icons/CheckCircleOutline.svg?react';
import ShippingContainer from '@/assets/icons/ShippingContainer.svg?react';
import CalendarDots from '@/assets/icons/CalendarDots.svg?react';
import Hash from '@/assets/icons/Hash.svg?react';
import Ellipse from '@/assets/icons/Ellipse.svg?react';
import MapPin from '@/assets/icons/MapPin.svg?react';
import { Tag } from '@/components/ui/tag/Tag';
import DottedStroke from '@/assets/icons/DottedStroke.svg?react';
import Warning from '@/assets/icons/Warning.svg?react';
import PhRecycleLight from '@/assets/icons/PhRecycleLight.svg?react';
import BxTimeFive from '@/assets/icons/BxTimeFive.svg?react';
import { CompanyCard } from '@/components/ui/company/CompanyCard';
import { toastService } from '@/components/ui/toast/toastService';

interface TransportDetailsDrawerProps {
  isDrawerOpen: boolean;
  setIsDrawerOpen: (value: boolean) => void;
  transportId: string;
  onEdit?: () => void;
  onDelete?: () => void;
}

export const TransportDetailsDrawer = ({
  isDrawerOpen,
  setIsDrawerOpen,
  transportId,
  onEdit,
  onDelete,
}: TransportDetailsDrawerProps) => {
  const { data, isLoading } = useQuery({
    queryKey: ['transport', transportId],
    queryFn: async () => {
      return await transportService
        .getTransportById(transportId)
        .catch((error) => {
          console.error('Error fetching transport:', error);
          return null;
        });
    },
  });

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (!onDelete) {
        toastService.warn('Transport kan niet verwijderd worden');
        return;
      }
      if (isDrawerOpen && event.key === 'Delete') {
        onDelete();
      }
    };

    if (isDrawerOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isDrawerOpen, onDelete]);

  const goodsItem = data?.goods?.goodsItem;
  const goodsItemText = goodsItem
    ? goodsItem?.name +
      ' - ' +
      goodsItem?.netNetWeight +
      ' ' +
      goodsItem?.unit +
      ' (Hoeveelheid: ' +
      goodsItem?.quantity +
      ')'
    : 'Geen afval';
  const containerText = data?.wasteContainer?.id
    ? data.wasteContainer.id
    : 'Geen container toegewezen';
  return (
    <Drawer
      title={'Transport details'}
      isOpen={isDrawerOpen}
      setIsOpen={setIsDrawerOpen}
      onEdit={onEdit}
      onDelete={onDelete}
    >
      {isLoading || (!data && <div>Transport laden...</div>)}
      {!isLoading && data && (
        <div
          className={'flex flex-col flex-1 self-stretch items-start p-4 gap-6'}
          data-testid='transport-details-drawer-content'
        >
          <div className={'flex flex-col items-start self-stretch gap-3'}>
            <div
              className={
                'flex flex-col justify-center items-start gap-1 self-stretch'
              }
            >
              <div className="flex items-center self-stretch gap-4">
                <h4>{data.pickupLocation.address.city}</h4>
                <CaretRight />
                <h4>{data?.deliveryLocation.address.city}</h4>
              </div>
              <span className="text-subtitle-2 text-color-text-secondary">
                {data?.truck?.brand} {data?.truck?.model}{' '}
                {data?.truck?.licensePlate}
              </span>
            </div>

            <div className={'flex flex-col items-start self-stretch gap-2'}>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <CheckCircle
                    className={'w-5 h-5 text-color-text-secondary'}
                  />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Status
                  </span>
                </div>
                <Tag status={data.status} />
              </div>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <Hash className={'w-5 h-5 text-color-text-secondary'} />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Transportnummer
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {data.displayNumber}
                </span>
              </div>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <CalendarDots
                    className={'w-5 h-5 text-color-text-secondary'}
                  />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Datum
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {format(new Date(data.pickupDateTime), 'dd-MM-yyyy')}{' '}
                  {data.deliveryDateTime
                    ? ' - ' +
                      format(new Date(data.deliveryDateTime), 'dd-MM-yyyy')
                    : ''}
                </span>
              </div>
              {data.transportHours && (
                <div className={'flex items-center gap-2 self-stretch'}>
                  <div className="flex items-center flex-1 gap-2">
                    <BxTimeFive
                      className={'w-5 h-5 text-color-text-secondary'}
                    />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Transport uren
                    </span>
                  </div>
                  <span className={'text-body-2 truncate'}>
                    {data.transportHours.toString().replace('.', ',')} uur
                  </span>
                </div>
              )}
            </div>
          </div>
          <div className="flex flex-col items-start self-stretch gap-3">
            <span className="subtitle-1">Route</span>
            <div className="flex flex-col items-start self-stretch gap-5 relative">
              <div className=" flex items-start gap-2 self-stretch">
                <div className="flex size-7 p-1 justify-center items-center gap-2 border-[0.875px] border-solid border-color-border-primary bg-color-surface-secondary rounded-full relative z-10">
                  <Ellipse />
                </div>
                <CompanyCard
                  companyName={data?.pickupCompany?.name}
                  dateTime={data.pickupDateTime}
                  address={data.pickupLocation.address}
                />
              </div>
              <DottedStroke
                className="absolute left-[13px]"
                style={{
                  top: '24px',
                  height: 'calc(100% - 56px - 1rem)',
                  width: '2px',
                  zIndex: 0,
                }}
              />
              <div className="flex items-start gap-2 self-stretch">
                <div className="flex size-7 p-1 justify-center items-center gap-2 border-[0.875px] border-solid border-color-border-primary bg-color-surface-secondary rounded-full relative z-10">
                  <MapPin />
                </div>
                <CompanyCard
                  companyName={data?.deliveryCompany?.name}
                  dateTime={data.deliveryDateTime}
                  address={data.deliveryLocation.address}
                />
              </div>
            </div>
          </div>
          <div className={'flex flex-col items-start self-stretch gap-3'}>
            <span className={'text-subtitle-1'}>Container</span>
            <div className={'flex items-center gap-2 self-stretch'}>
              <div className="flex items-center flex-1 gap-2">
                <ShippingContainer
                  className={'w-5 h-5 text-color-text-secondary'}
                />
                <span className={'text-body-2 text-color-text-secondary'}>
                  Nummer
                </span>
              </div>
              <span className={'text-body-2 truncate'}>{containerText}</span>
            </div>
            <div className={'flex items-center gap-2 self-stretch'}>
              <div className="flex items-center flex-1 gap-2">
                <PhRecycleLight
                  className={'w-5 h-5 text-color-text-secondary'}
                />
                <span className={'text-body-2 text-color-text-secondary'}>
                  Afvalstof
                </span>
              </div>
              <span className={'text-body-2 truncate'}>{goodsItemText}</span>
            </div>
            {goodsItem?.wasteStreamNumber && (
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <Hash className={'w-5 h-5 text-color-text-secondary'} />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Afvalstroomnummer
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {goodsItem.wasteStreamNumber}
                </span>
              </div>
            )}
          </div>

          <WaybillDownloadSection transportId={data.id} />

          {data.note && (
            <div
              className={
                'flex items-center self-stretch gap-2 py-2 pl-2 pr-3 border border-solid border-color-status-warning-primary bg-color-status-warning-light rounded-radius-sm'
              }
            >
              <Warning />
              <span className={'text-body-2'}>{data.note}</span>
            </div>
          )}
        </div>
      )}
    </Drawer>
  );
};
