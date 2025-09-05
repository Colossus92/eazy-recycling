import { useState } from 'react';
import { DriverPlanningItem } from '@/api/planningService';
import CaretDown from '@/assets/icons/CaretDown.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import { PlanningCard } from '@/features/mobile/planning/PlanningCard';

interface MobilePlanningItemProps {
  truckData: {
    truck: string;
    items: DriverPlanningItem[];
  };
  selectedDate?: Date;
}

export const MobilePlanningItem = ({
  truckData,
  selectedDate,
}: MobilePlanningItemProps) => {
  const [isCollapsed, setIsCollapsed] = useState(true);

  return (
    <div className="flex flex-col items-start self-stretch gap-4 p-4 bg-color-surface-primary border border-solid border-color-border-primary rounded-radius-md">
      <div
        className="flex items-center gap-2 w-full cursor-pointer"
        onClick={() => setIsCollapsed(!isCollapsed)}
      >
        {isCollapsed ? (
          <CaretRight className="text-color-text-secondary" />
        ) : (
          <CaretDown className="text-color-text-secondary" />
        )}
        <span className="subtitle-1">{truckData.truck}</span>
      </div>
      {!isCollapsed &&
        truckData.items.map((transport, itemIndex) => (
          <PlanningCard
            key={itemIndex}
            transport={transport}
            selectedDate={selectedDate}
          />
        ))}
    </div>
  );
};
