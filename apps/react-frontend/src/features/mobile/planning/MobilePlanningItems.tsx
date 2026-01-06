import { format } from 'date-fns';
import { MobilePlanningItem } from './MobilePlanningItem';
import { DriverPlanningItem } from '@/api/client/models/driver-planning-item';
import { Button } from '@/components/ui/button/Button';
import MapPinArea from '@/assets/icons/MapPinArea.svg?react';

interface MobilePlanningItemsProps {
  isLoading: boolean;
  error: Error | null;
  driverPlanning: Record<string, Record<string, DriverPlanningItem[]>> | null;
  selectedDate: Date;
}

export const MobilePlanningItems = ({
  driverPlanning,
  isLoading,
  error,
  selectedDate,
}: MobilePlanningItemsProps) => {
  const getItemsForSelectedDate = () => {
    if (!driverPlanning) return [];

    const formattedDate = format(selectedDate, 'yyyy-MM-dd');

    if (!driverPlanning[formattedDate]) return [];

    return Object.entries(driverPlanning[formattedDate]).map(
      ([truck, items]) => ({
        truck,
        items,
      })
    );
  };

  if (error) {
    throw error;
  }

  const selectedDateItems = getItemsForSelectedDate();

  return (
    <div className="flex-1 flex flex-col items-start self-stretch p-4 gap-5 bg-color-surface-secondary">
      <Button
        icon={MapPinArea}
        label={"Containerlocatie doorgeven"}
        onClick={() => console.log("Container locatie doorgeven")}
        fullWidth={true}
        variant="primary"
        size='medium'
      />
      {isLoading ? (
        <div className="flex items-center justify-center w-full p-8">
          <span>Planning wordt geladen...</span>
        </div>
      ) : selectedDateItems.length === 0 ? (
        <div className="flex items-center justify-center w-full p-8">
          <span>Geen ritten voor deze dag</span>
        </div>
      ) : (
        selectedDateItems.map((truckData, truckIndex) => (
          <MobilePlanningItem
            key={truckIndex}
            truckData={truckData}
            selectedDate={selectedDate}
          />
        ))
      )}
    </div>
  );
};
