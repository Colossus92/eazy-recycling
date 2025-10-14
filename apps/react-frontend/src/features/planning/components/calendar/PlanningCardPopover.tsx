import { PopoverPanel } from '@headlessui/react';
import { format } from 'date-fns';
import { RefObject, useEffect, useRef, useState } from 'react';
import { PlanningItem } from '@/features/planning/hooks/usePlanning.ts';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import PencilSimple from '@/assets/icons/PencilSimple.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';
import ShippingContainer from '@/assets/icons/ShippingContainer.svg?react';
import CalendarDots from '@/assets/icons/CalendarDots.svg?react';
import { Button } from '@/components/ui/button/Button.tsx';
import { TransportStatusTag } from '@/features/planning/components/tag/TransportStatusTag';
import CheckCircle from '@/assets/icons/CheckCircleOutline.svg?react';

interface PlanningCardPopoverProps {
  transport: PlanningItem;
  cardRef: RefObject<HTMLDivElement>;
  setIsFormOpen: (value: boolean) => void;
  onDelete: () => void;
  isDeleting: boolean;
}

export const PlanningCardPopover = ({
  transport,
  cardRef,
  setIsFormOpen,
  onDelete,
  isDeleting,
}: PlanningCardPopoverProps) => {
  const [popoverStyle, setPopoverStyle] = useState({});
  const popoverRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Function to calculate the best position for the popover
    const calculatePosition = () => {
      if (!popoverRef.current || !cardRef.current) return;

      const popoverRect = popoverRef.current.getBoundingClientRect();
      const popoverWidth = popoverRect.width;
      const cardRect = cardRef.current.getBoundingClientRect();
      const viewportWidth = window.innerWidth;

      // Check if there's enough space to the right
      const spaceToRight = viewportWidth - cardRect.right;
      const needsLeftPosition = spaceToRight < popoverWidth + 20; // 20px buffer

      if (needsLeftPosition) {
        // Position to the left
        setPopoverStyle({
          top: '-10px',
          right: '100%',
          marginRight: '10px',
        });
      } else {
        // Position to the right (default)
        setPopoverStyle({
          top: '-10px',
          left: '100%',
          marginLeft: '10px',
        });
      }
    };

    // Calculate position on mount
    // Use a small timeout to ensure the popover is rendered first
    const timer = setTimeout(() => {
      calculatePosition();
    }, 0);

    // Recalculate on window resize
    window.addEventListener('resize', calculatePosition);

    return () => {
      window.removeEventListener('resize', calculatePosition);
      clearTimeout(timer);
    };
  }, [cardRef]);

  return (
    <>
      <PopoverPanel
        static
        ref={popoverRef}
        className={
          'absolute z-10 w-[372px] flex flex-col px-3 pt-3 pb-4 gap-3 bg-color-surface-primary shadow-md rounded-radius-md border border-color-border-primary'
        }
        style={popoverStyle}
      >
        <div className={'flex flex-col items-start gap-1 self-stretch'}>
          <div
            className={'flex items-center gap-4 self-stretch justify-between'}
          >
            <div className={'flex items-center self-stretch gap-4'}>
              <div
                className={
                  'flex flex-wrap items-center gap-1 flex-shrink min-w-0'
                }
              >
                <span className={'text-subtitle-1 truncate'}>
                  {transport.originCity}
                </span>
                {
                  <CaretRight
                    className={'text-color-text-secondary flex-shrink-0'}
                  />
                }
                <span className={'text-subtitle-1 truncate'}>
                  {transport.destinationCity}
                </span>
              </div>
            </div>
            {transport.status !== 'FINISHED' && (
              <div className={'flex items-center gap-2 flex-shrink-0'}>
                <Button
                  icon={PencilSimple}
                  showText={false}
                  variant="icon"
                  iconPosition="right"
                  onClick={() => setIsFormOpen(true)}
                />
                <Button
                  icon={TrashSimple}
                  showText={false}
                  variant="icon"
                  iconPosition="right"
                  onClick={onDelete}
                  disabled={isDeleting}
                />
              </div>
            )}
          </div>
          <span
            className={
              'text-subtitle-2 text-color-text-secondary truncate w-full'
            }
          >
            {transport.truck
              ? transport.truck.brand +
                ' ' +
                transport.truck.model +
                ' (' +
                transport.truck.licensePlate +
                ')'
              : 'Geen vrachtwagen toegewezen'}
          </span>
        </div>
        <div className={'flex flex-col items-start self-stretch gap-2'}>
          <div className={'flex items-center gap-2 self-stretch'}>
            <CheckCircle
              className={'w-5 h-5 text-color-text-secondary flex-shrink-0'}
            />
            <TransportStatusTag status={transport.status} />
          </div>
          <div className={'flex items-center gap-2 self-stretch'}>
            <ShippingContainer
              className={'w-5 h-5 text-color-text-secondary flex-shrink-0'}
            />
            <span className={'text-body-2 truncate'}>
              {transport.containerId
                ? transport.containerId
                : 'Geen container toegewezen'}
            </span>
          </div>
          <div className={'flex items-center gap-2 self-stretch'}>
            <CalendarDots
              className={'w-5 h-5 text-color-text-secondary flex-shrink-0'}
            />
            <span className={'text-body-2 truncate'}>
              {format(new Date(transport.pickupDate), 'dd-MM-yyyy')}
              {transport.deliveryDate &&
                ` - ${format(new Date(transport.deliveryDate), 'dd-MM-yyyy')}`}
            </span>
          </div>
        </div>
      </PopoverPanel>
    </>
  );
};
