import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { planningService } from '@/api/planningService';

interface UseDriverPlanningProps {
  driverId: string;
  startDate: Date;
  endDate: Date;
}

export const useDriverPlanning = ({
  driverId,
  startDate,
  endDate,
}: UseDriverPlanningProps) => {
  const formattedStartDate = format(startDate, 'yyyy-MM-dd');
  const formattedEndDate = format(endDate, 'yyyy-MM-dd');

  const { data, isLoading, error } = useQuery({
    queryKey: [
      'driverPlanning',
      driverId,
      formattedStartDate,
      formattedEndDate,
    ],
    queryFn: () =>
      planningService.getDriverPlanning(
        driverId,
        formattedStartDate,
        formattedEndDate
      ),
  });

  return {
    driverPlanning: data,
    isLoading,
    error,
  };
};
