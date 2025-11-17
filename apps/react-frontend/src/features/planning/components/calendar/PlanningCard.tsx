import Avatar from 'react-avatar';
import { Popover, PopoverButton } from '@headlessui/react';
import { useRef, useState } from 'react';
import { DeleteTransportDialog } from '../DeleteTransportDialog';
import { useTransportDeletion } from '../../hooks/useTransportDeletion';
import { PlanningItem } from '@/features/planning/hooks/usePlanning';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import { PlanningCardPopover } from '@/features/planning/components/calendar/PlanningCardPopover.tsx';
import { ContainerTransportForm } from '@/features/planning/forms/containertransportform/ContainerTransportForm.tsx';
import { TransportDetailsDrawer } from '@/features/planning/components/drawer/TransportDetailsDrawer';
import { DriverPlanningItemStatusEnum } from '@/api/client/models/driver-planning-item';
import { WasteStreamTransportForm } from '@/features/wastestreams/components/wastetransportform';

interface PlanningCardProps {
  transport: PlanningItem;
  activePopoverId?: string;
  onMouseEnter: () => void;
  onMouseLeave: () => void;
}

export const PlanningCard = ({
  transport,
  activePopoverId,
  onMouseEnter,
  onMouseLeave,
}: PlanningCardProps) => {
  const {
    isDeleting,
    showDeleteDialog,
    transportToDelete,
    confirmDelete,
    handleDelete,
    cancelDelete,
  } = useTransportDeletion();
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const cardRef = useRef<HTMLDivElement>(null);
  const clickTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const isDoubleClickRef = useRef(false);
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

  const truncateCity = (cityName: string) => {
    return cityName.length > 4 ? `${cityName.substring(0, 4)}` : cityName;
  };

  const handleSingleClick = () => {
    if (isDoubleClickRef.current) {
      isDoubleClickRef.current = false;
      return;
    }

    clickTimeoutRef.current = setTimeout(() => {
      if (!isDoubleClickRef.current) {
        setIsDrawerOpen(true);
      }
      isDoubleClickRef.current = false;
    }, 250);
  };

  const handleDoubleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    isDoubleClickRef.current = true;

    if (clickTimeoutRef.current) {
      clearTimeout(clickTimeoutRef.current);
      clickTimeoutRef.current = null;
    }

    setIsFormOpen(true);
  };
  return (
    <>
      <div
        className="relative"
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
        onDoubleClick={handleDoubleClick}
        ref={cardRef}
      >
        <Popover className="relative">
          <PopoverButton
            as="div"
            className="w-full cursor-pointer"
            onClick={handleSingleClick}
          >
            <div
              className={`flex flex-col items-start content-center self-stretch py-1.5 pr-1.5 pl-2 gap-3 border-solid border-l-4 rounded-radius-md ${colors.get(transport.status)} `}
            >
              <div className={'flex flex-col items-start self-stretch'}>
                <span className={'text-subtitle-1 text-color-text-secondary'}>
                  {transport.displayNumber}
                </span>
                <div
                  className={'flex flex-wrap items-center self-stretch gap-2'}
                >
                  <span className={'text-subtitle-1'}>
                    {truncateCity(transport.originCity)}
                  </span>
                  <div className="flex-shrink-0">
                    <CaretRight className={'text-color-text-secondary'} />
                  </div>
                  <span className={'text-subtitle-1'}>
                    {truncateCity(transport.destinationCity || '-')}
                  </span>
                </div>
              </div>
              <div className={'flex items-center gap-1'}>
                {transport.driver ? (
                  <>
                    <Avatar
                      name={`${transport.driver.firstName} ${transport.driver.lastName}`}
                      maxInitials={2}
                      size={'20px'}
                      round={true}
                    />
                    {`${transport.driver.firstName} ${transport.driver.lastName}`}
                  </>
                ) : (
                  'Onbekend'
                )}
              </div>
            </div>
          </PopoverButton>

          {activePopoverId === transport.id && (
            <PlanningCardPopover
              transport={transport}
              cardRef={cardRef}
              setIsFormOpen={setIsFormOpen}
              onDelete={() => confirmDelete(transport)}
              isDeleting={isDeleting}
            />
          )}
        </Popover>
      </div>
      {transport.transportType === 'CONTAINER' && isFormOpen && (
        <ContainerTransportForm
          isOpen={isFormOpen}
          setIsOpen={setIsFormOpen}
          transportId={transport.id}
        />
      )}
      {isFormOpen && transport.transportType === 'WASTE' && (
        <WasteStreamTransportForm
          isOpen={isFormOpen}
          setIsOpen={setIsFormOpen}
          transportId={transport.id}
        />
      )}
      {isDrawerOpen && (
        <TransportDetailsDrawer
          isDrawerOpen={isDrawerOpen}
          setIsDrawerOpen={setIsDrawerOpen}
          transportId={transport.id}
          onEdit={
            transport.status === DriverPlanningItemStatusEnum.Finished
              ? undefined
              : () => setIsFormOpen(true)
          }
          onDelete={
            transport.status === DriverPlanningItemStatusEnum.Finished
              ? undefined
              : () => confirmDelete(transport)
          }
        />
      )}
      <DeleteTransportDialog
        isOpen={showDeleteDialog}
        setIsOpen={cancelDelete}
        onDelete={handleDelete}
        transport={transportToDelete}
      />
    </>
  );
};
