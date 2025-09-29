import { TransportDto } from '@/api/client/models/transport-dto';
import Ellipse from '@/assets/icons/Ellipse.svg?react';
import { CompanyCard } from '@/components/ui/company/CompanyCard';
import DottedStroke from '@/assets/icons/DottedStroke.svg?react';
import MapPin from '@/assets/icons/MapPin.svg?react';
import ShippingContainer from '@/assets/icons/ShippingContainer.svg?react';
import PhRecycleLight from '@/assets/icons/PhRecycleLight.svg?react';
import Warning from '@/assets/icons/Warning.svg?react';
import { WaybillDownloadSection } from '@/features/planning/components/drawer/WaybillDownloadSection';

interface MobileTransportDetailsProps {
  transport: TransportDto;
}

const MobileTransportDetails = ({ transport }: MobileTransportDetailsProps) => {
  const goodsItem = transport?.goods?.goodsItem;
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
  const containerText = transport?.wasteContainer?.id
    ? transport.wasteContainer.id
    : 'Geen container toegewezen';

  return (
    <>
      <div className="flex flex-col items-start self-stretch gap-5">
        <div className="flex flex-col items-start self-stretch gap-4">
          <span className="text-subtitle-1">Route</span>
          <div className="grid grid-cols-[28px_1fr] gap-2 w-full relative">
            <div className="flex size-7 p-1 justify-center items-center gap-2 border-[0.875px] border-solid border-color-border-primary bg-color-surface-secondary rounded-full relative z-10">
              <Ellipse />
            </div>
            <CompanyCard
              companyName={transport.pickupCompany?.name}
              dateTime={transport.pickupDateTime}
              address={transport.pickupLocation.address}
            />
            <DottedStroke
              className="absolute left-[13px]"
              style={{
                top: '24px',
                height: 'calc(100% - 56px - 1rem)',
                width: '2px',
                zIndex: 0,
              }}
            />
            <div className="flex size-7 p-1 justify-center items-center gap-2 border-[0.875px] border-solid border-color-border-primary bg-color-surface-secondary rounded-full relative z-10">
              <MapPin />
            </div>
            <CompanyCard
              companyName={transport.deliveryCompany?.name}
              dateTime={transport.deliveryDateTime}
              address={transport.deliveryLocation.address}
            />
          </div>
        </div>
      </div>
      <div className="flex flex-col items-start self-stretch gap-3">
        <span className="text-subtitle-1">Container</span>
        <div className="flex items-center self-stretch gap-2">
          <div className="flex items-center flex-1 gap-2">
            <ShippingContainer className="size-5 text-color-text-secondary" />
            <span className="text-body-2 text-color-text-secondary">
              Nummer
            </span>
          </div>
          <span className="text-subtitle-2">{containerText}</span>
        </div>
        <div className="flex items-center self-stretch gap-2">
          <div className="flex items-center flex-1 gap-2">
            <PhRecycleLight className="size-5 text-color-text-secondary" />
            <span className="text-body-2 text-color-text-secondary">Afval</span>
          </div>
          <span className="text-subtitle-2">{goodsItemText}</span>
        </div>
      </div>
      {transport.note && (
        <div
          className={
            'flex items-center self-stretch gap-2 py-2 pl-2 pr-3 border border-solid border-color-status-warning-primary bg-color-status-warning-light rounded-radius-sm'
          }
        >
          <Warning />
          <span className={'text-body-2'}>{transport.note}</span>
        </div>
      )}
      <WaybillDownloadSection transportId={transport.id} />
    </>
  );
};

export default MobileTransportDetails;
