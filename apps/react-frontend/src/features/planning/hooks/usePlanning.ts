import { useQuery } from '@tanstack/react-query';
import {
  PlanningFilterParams,
  planningService,
} from '@/api/services/planningService.ts';
import { Truck } from '@/types/api.ts';
import { DriverPlanningItemStatusEnum } from '@/api/client/models/driver-planning-item';

interface Driver {
  firstName: string;
  lastName: string;
  avatar?: string;
}

export interface PlanningItem {
  displayNumber: string;
  deliveryDate?: string;
  pickupDate: string;
  id: string;
  originCity: string;
  destinationCity: string;
  driver: Driver;
  status: DriverPlanningItemStatusEnum;
  truck?: Truck;
  containerId?: string;
  transportType: string;
  sequenceNumber: number;
}

export interface Planning {
  dates: Date[];
  transports: TruckTransports[];
}

export interface TruckTransports {
  truck: string;
  transports: {
    [date: string]: PlanningItem[];
  };
}

export const usePlanning = (date: Date, filters: PlanningFilterParams) => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['planning', date, filters],
    queryFn: () => planningService.list(date, filters),
    select: (data) => {
      // Transform string dates to Date objects
      if (data && data.dates) {
        return {
          ...data,
          dates: data.dates.map((dateStr) => new Date(dateStr)),
        };
      }
      return data;
    },
  });

  return { planning: data, isLoading, error };
};
