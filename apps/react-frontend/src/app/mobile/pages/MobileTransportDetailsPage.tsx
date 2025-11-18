import { useQuery } from '@tanstack/react-query';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { formatInstantInCET } from '@/utils/dateUtils';
import { lazy, Suspense, useState } from 'react';
import CalendarDots from '@/assets/icons/CalendarDots.svg?react';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import CheckCircleOutline from '@/assets/icons/CheckCircleOutline.svg?react';
import Hash from '@/assets/icons/Hash.svg?react';
import { TransportStatusTag } from '@/features/planning/components/tag/TransportStatusTag';
import { MobileTabBar } from '@/components/ui/mobile/MobileTabBar';
import { Button } from '@/components/ui/button/Button';
import { ReportFinishedComponent } from '@/features/mobile/planning/ReportFinishedComponent';
import {
  resolveLocationAddress,
  transportService,
} from '@/api/services/transportService';

const MobileTransportDetailsTab = lazy(
  () => import('@/features/mobile/planning/MobileTransportDetails')
);
const SignaturesTab = lazy(
  () => import('@/features/mobile/planning/SignaturesTab')
);

export const MobileTransportDetailsPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { state } = useLocation();
  const [activeTab, setActiveTab] = useState(state?.activeTab || 'Algemeen');
  const { data: transport, isLoading } = useQuery({
    queryKey: ['transport', id],
    queryFn: async () => {
      if (!id) {
        console.error('Transport ID is undefined');
        return null;
      }
      return await transportService.getTransportById(id).catch((error) => {
        console.error('Error fetching transport:', error);
        return null;
      });
    },
    enabled: !!id,
    staleTime: 0,
    gcTime: 5 * 60 * 1000,
    refetchOnMount: 'always',
    refetchOnWindowFocus: false,
  });

  const pickupAddress =
    transport && resolveLocationAddress(transport.pickupLocation);
  const deliveryAddress =
    transport && resolveLocationAddress(transport.deliveryLocation);

  return (
    <div className="flex flex-col w-full">
      <div className="flex items-center py-2 px-4 gap-3 border-b border-solid border-color-border-primary">
        <Button
          variant="icon"
          icon={CaretLeft}
          showText={false}
          onClick={() => {
            const calendarDate =
              sessionStorage.getItem('eazy-recycling-selected-date') ||
              new Date().toISOString();

            sessionStorage.setItem(
              'eazy-recycling-selected-date',
              calendarDate
            );

            navigate('/mobile/planning');
          }}
        />
        <h4>Transport Details</h4>
      </div>

      {isLoading ? (
        <div className="flex justify-center items-center h-[calc(100vh-56px)]">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-color-primary"></div>
        </div>
      ) : !transport ? (
        <div className="flex flex-col items-center justify-center h-[calc(100vh-56px)] p-4">
          <p className="text-color-text-secondary">
            Transport details niet gevonden
          </p>
        </div>
      ) : (
        <div className="flex flex-col items-start self-stretch gap-5 p-4">
          <div className="flex flex-col items-start self-stretch gap-4">
            <div className="flex flex-col items-start self-stretch gap-2">
              <div className="flex items-center self-stretch gap-4">
                <h4>{pickupAddress?.city}</h4>
                <CaretRight className="text-color-text-secondary" />
                <h4>{deliveryAddress?.city}</h4>
              </div>
              <span className="text-subtitle-2 text-color-text-secondary">
                {transport.truck?.brand} {transport.truck?.description} (
                {transport.truck?.licensePlate})
              </span>
            </div>
          </div>
          <div className="flex flex-col items-start self-stretch gap-2">
            <div className="flex items-center self-stretch gap-2">
              <div className="flex items-center flex-1 gap-2">
                <CheckCircleOutline className="size-5text-color-text-secondary" />
                <span className="text-body-2 text-color-text-secondary">
                  Status
                </span>
              </div>
              <TransportStatusTag
                status={transport.status}
              ></TransportStatusTag>
            </div>
            <div className="flex items-center self-stretch gap-2">
              <div className="flex items-center flex-1 gap-2">
                <Hash className="size-5text-color-text-secondary" />
                <span className="text-body-2 text-color-text-secondary">
                  Nummer
                </span>
              </div>
              <span className="text-subtitle-2">
                #{transport.displayNumber}
              </span>
            </div>
            <div className="flex items-center self-stretch gap-2">
              <div className="flex items-center flex-1 gap-2">
                <CalendarDots className="size-5 text-color-text-secondary" />
                <span className="text-body-2 text-color-text-secondary">
                  Datum
                </span>
              </div>
              <span className="text-subtitle-2">
                {formatInstantInCET(transport.pickupDateTime, 'dd-MM-yyyy')}
              </span>
            </div>
          </div>
          <ReportFinishedComponent transport={transport} />
          <div className="flex flex-col items-start self-stretch gap-4">
            <MobileTabBar
              activeTab={activeTab}
              setActiveTab={setActiveTab}
              transportType={transport.transportType}
            />
          </div>

          <Suspense
            fallback={
              <div className="flex justify-center items-center p-4">
                <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-color-primary"></div>
              </div>
            }
          >
            {activeTab === 'Algemeen' && (
              <MobileTransportDetailsTab transport={transport} />
            )}
            {activeTab === 'Handtekeningen' &&
              transport.transportType === 'WASTE' && (
                <SignaturesTab transport={transport} />
              )}
          </Suspense>
        </div>
      )}
    </div>
  );
};

export default MobileTransportDetailsPage;
