import { http } from '@/api/http.ts';
import { Planning, Status } from '@/features/planning/hooks/usePlanning';
import { Location } from '@/api/transportService';

export interface DriverPlanningItem {
  id: string;
  pickupDateTime: string;
  deliveryDateTime: string;
  displayNumber: string;
  pickupLocation: Location;
  deliveryLocation: Location;
  containerId: string;
  status: Status;
}

export interface PlanningFilterParams {
  driverId?: string;
  truckId?: string;
  statuses?: string[];
}

interface ReorderRequest {
  date: string; // LocalDate in ISO format (YYYY-MM-DD)
  licensePlate: string;
  transportIds: string[]; // List of UUIDs
}

export const planningService = {
  list: async (date: Date, filters?: PlanningFilterParams) => {
    let url = `/planning/${date.toISOString().split('T')[0]}`;

    // Add query parameters if filters are provided
    if (filters) {
      const params = new URLSearchParams();

      if (filters.driverId) {
        params.append('driverId', filters.driverId);
      }

      if (filters.truckId) {
        params.append('truckId', filters.truckId);
      }

      if (filters.statuses && filters.statuses.length > 0) {
        params.append('status', filters.statuses.join(','));
      }

      const queryString = params.toString();
      if (queryString) {
        url += `?${queryString}`;
      }
    }

    return await http.get<Planning>(url).then((r) => r.data);
  },

  reorder: async (
    date: string,
    licensePlate: string,
    transportIds: string[]
  ) => {
    const payload: ReorderRequest = {
      date,
      licensePlate,
      transportIds,
    };

    return await http
      .put<Planning>('/planning/reorder', payload)
      .then((r) => r.data);
  },

  getDriverPlanning: async (
    driverId: string,
    startDate: string,
    endDate: string
  ) => {
    const url = `/planning/driver/${driverId}?startDate=${startDate}&endDate=${endDate}`;
    return await http
      .get<Record<string, Record<string, DriverPlanningItem[]>>>(url)
      .then((r) => r.data);
  },
};
