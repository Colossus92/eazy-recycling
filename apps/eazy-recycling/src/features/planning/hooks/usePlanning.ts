import { useQuery } from '@tanstack/react-query';
import {
  PlanningFilterParams,
  planningService,
} from '@/api/planningService.ts';
import { Truck } from '@/types/api.ts';

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
  status: Status;
  truck?: Truck;
  containerId?: string;
  transportType: string;
  sequenceNumber: number;
}

export enum Status {
  FINISHED = 'FINISHED',
  PLANNED = 'PLANNED',
  UNPLANNED = 'UNPLANNED',
  INVOICED = 'INVOICED',
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
