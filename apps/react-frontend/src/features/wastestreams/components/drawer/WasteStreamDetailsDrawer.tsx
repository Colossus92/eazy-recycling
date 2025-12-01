import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { wasteStreamService } from '@/api/services/wasteStreamService';
import {
  NormalizedAddress,
  resolveLocationAddress,
} from '@/api/services/transportService';
import { Drawer } from '@/components/ui/drawer/Drawer';
import { Button } from '@/components/ui/button/Button';
import Hash from '@/assets/icons/Hash.svg?react';
import CheckCircle from '@/assets/icons/CheckCircleOutline.svg?react';
import MapPin from '@/assets/icons/MapPin.svg?react';
import Ellipse from '@/assets/icons/Ellipse.svg?react';
import PhRecycleLight from '@/assets/icons/PhRecycleLight.svg?react';
import BuildingOffice from '@/assets/icons/BuildingOffice.svg?react';
import Scale from '@/assets/icons/Scale.svg?react';
import DottedStroke from '@/assets/icons/DottedStroke.svg?react';
import { CompanyCard } from '@/components/ui/company/CompanyCard';
import {
  WasteStreamStatusTag,
  WasteStreamStatusTagProps,
} from '@/features/wastestreams/components/WasteStreamStatusTag';
import { DeliveryLocationView } from '@/api/client/models/delivery-location-view';
import { WeightTicketsTable } from './WeightTicketsTable';

interface WasteStreamDetailsDrawerProps {
  isDrawerOpen: boolean;
  setIsDrawerOpen: (value: boolean) => void;
  wasteStreamNumber: string;
  onEdit?: () => void;
  onDelete?: () => void;
}

/**
 * Resolves delivery location to a normalized address format
 */
const resolveDeliveryLocationAddress = (
  location: DeliveryLocationView | undefined
): NormalizedAddress | null => {
  if (!location) {
    return null;
  }

  // Handle company type
  const address = location.processor?.address;
  return {
    companyId: location.processor?.id,
    companyName: location.processor?.name,
    street: address?.street || '',
    houseNumber: address?.houseNumber || '',
    postalCode: address?.postalCode || '',
    city: address?.city || '',
    country: address?.country,
  };
};

export const WasteStreamDetailsDrawer = ({
  isDrawerOpen,
  setIsDrawerOpen,
  wasteStreamNumber,
  onEdit,
  onDelete,
}: WasteStreamDetailsDrawerProps) => {
  const navigate = useNavigate();
  const [isWeightTicketsTableOpen, setIsWeightTicketsTableOpen] =
    useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['wasteStream', wasteStreamNumber],
    queryFn: async () => {
      return await wasteStreamService
        .getByNumber(wasteStreamNumber)
        .catch((error) => {
          console.error('Error fetching waste stream:', error);
          return null;
        });
    },
    enabled: isDrawerOpen && !!wasteStreamNumber,
  });

  const pickupDetails = data?.pickupLocation
    ? resolveLocationAddress(data.pickupLocation as any)
    : null;
  const deliveryDetails = resolveDeliveryLocationAddress(
    data?.deliveryLocation
  );

  const handleWeightTicketClick = (weightTicketNumber: number) => {
    navigate(`/weight-tickets?weightTicketId=${weightTicketNumber}`);
  };

  return (
    <>
      <Drawer
        title={'Afvalstroomnummer details'}
        isOpen={isDrawerOpen}
        setIsOpen={setIsDrawerOpen}
        onEdit={onEdit}
        onDelete={onDelete}
      >
        {isLoading || (!data && <div>Afvalstroomnummer laden...</div>)}
        {!isLoading && data && (
          <div
            className={
              'flex flex-col flex-1 self-stretch items-start p-4 gap-6'
            }
            data-testid="waste-stream-details-drawer-content"
          >
            {/* Header Section */}
            <div className={'flex flex-col items-start self-stretch gap-3'}>
              <div
                className={
                  'flex flex-col justify-center items-start gap-1 self-stretch'
                }
              >
                <h4>{data.wasteStreamNumber}</h4>
                <span className="text-subtitle-2 text-color-text-secondary">
                  {data.wasteType?.name || 'Geen afvalstof'}
                </span>
              </div>

              {/* Details Grid */}
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
                  <WasteStreamStatusTag
                    status={data.status as WasteStreamStatusTagProps['status']}
                  />
                </div>
                <div className={'flex items-center gap-2 self-stretch'}>
                  <div className="flex items-center flex-1 gap-2">
                    <Hash className={'w-5 h-5 text-color-text-secondary'} />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Afvalstroomnummer
                    </span>
                  </div>
                  <span className={'text-body-2 truncate'}>
                    {data.wasteStreamNumber}
                  </span>
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
                  <span className={'text-body-2 truncate'}>
                    {`${data.wasteType?.name} (${data.wasteType?.euralCode?.code})` ||
                      '-'}
                  </span>
                </div>
              </div>
            </div>

            {/* Route Section */}
            <div className="flex flex-col items-start self-stretch gap-3">
              <span className="subtitle-1">Route</span>
              <div className="flex flex-col items-start self-stretch gap-5 relative">
                <div className="flex items-start gap-2 self-stretch">
                  <div className="flex size-7 p-1 justify-center items-center gap-2 border-[0.875px] border-solid border-color-border-primary bg-color-surface-secondary rounded-full relative z-10">
                    <Ellipse />
                  </div>
                  <CompanyCard details={pickupDetails} />
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
                  <CompanyCard details={deliveryDetails} />
                </div>
              </div>
            </div>

            {/* Parties Section */}
            <div className={'flex flex-col items-start self-stretch gap-3'}>
              <span className={'text-subtitle-1'}>Partijen</span>
              {data.consignorParty && (
                <div className={'flex items-center gap-2 self-stretch'}>
                  <div className="flex items-center flex-1 gap-2">
                    <BuildingOffice
                      className={'w-5 h-5 text-color-text-secondary'}
                    />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Afzender
                    </span>
                  </div>
                  <span className={'text-body-2 truncate'}>
                    {(data.consignorParty as any)?.company?.name ||
                      (data.consignorParty as any)?.name ||
                      '-'}
                  </span>
                </div>
              )}
              {data.pickupParty && (
                <div className={'flex items-center gap-2 self-stretch'}>
                  <div className="flex items-center flex-1 gap-2">
                    <BuildingOffice
                      className={'w-5 h-5 text-color-text-secondary'}
                    />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Ontdoener
                    </span>
                  </div>
                  <span className={'text-body-2 truncate'}>
                    {data.pickupParty.name}
                  </span>
                </div>
              )}
              {data.collectorParty && (
                <div className={'flex items-center gap-2 self-stretch'}>
                  <div className="flex items-center flex-1 gap-2">
                    <BuildingOffice
                      className={'w-5 h-5 text-color-text-secondary'}
                    />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Inzamelaar
                    </span>
                  </div>
                  <span className={'text-body-2 truncate'}>
                    {data.collectorParty.name}
                  </span>
                </div>
              )}
              {data.brokerParty && (
                <div className={'flex items-center gap-2 self-stretch'}>
                  <div className="flex items-center flex-1 gap-2">
                    <BuildingOffice
                      className={'w-5 h-5 text-color-text-secondary'}
                    />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Makelaar
                    </span>
                  </div>
                  <span className={'text-body-2 truncate'}>
                    {data.brokerParty.name}
                  </span>
                </div>
              )}
              {data.dealerParty && (
                <div className={'flex items-center gap-2 self-stretch'}>
                  <div className="flex items-center flex-1 gap-2">
                    <BuildingOffice
                      className={'w-5 h-5 text-color-text-secondary'}
                    />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Handelaar
                    </span>
                  </div>
                  <span className={'text-body-2 truncate'}>
                    {data.dealerParty.name}
                  </span>
                </div>
              )}
            </div>

            {/* Weight Tickets Button */}
            <div className={'flex flex-col items-start self-stretch gap-3'}>
              <span className={'text-subtitle-1'}>Weegbonnen</span>
              <Button
                variant="secondary"
                icon={Scale}
                label="Bekijk weegbonnen"
                iconPosition="left"
                onClick={() => setIsWeightTicketsTableOpen(true)}
              />
            </div>
          </div>
        )}
      </Drawer>

      {/* Weight Tickets Table Modal */}
      <WeightTicketsTable
        isOpen={isWeightTicketsTableOpen}
        setIsOpen={setIsWeightTicketsTableOpen}
        wasteStreamNumber={wasteStreamNumber}
        onWeightTicketClick={handleWeightTicketClick}
      />
    </>
  );
};
