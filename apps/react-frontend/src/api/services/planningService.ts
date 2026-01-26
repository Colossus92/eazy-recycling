import {
  Driver,
  Planning,
  PlanningItem,
} from '@/features/planning/hooks/usePlanning';
import { PlanningControllerApi } from '../client';
import { apiInstance } from './apiInstance';
import { PlanningView } from '../client/models/planning-view';

export interface PlanningFilterParams {
  driverId?: string;
  truckId?: string;
  statuses?: string[];
}

const planningApi = new PlanningControllerApi(apiInstance.config);

/**
 * Maps a PlanningView object from the API to the internal Planning type
 * @param planningView - The planning view data from the API
 * @returns A properly typed Planning object
 */
const mapPlanningViewToPlanning = (planningView: PlanningView): Planning => {
  return {
    dates: planningView.dates.map((dateStr) => new Date(dateStr)),
    transports: planningView.transports.map((transportView) => {
      // Create a TruckTransports object for each transport
      return {
        truck: transportView.truck,
        transports: Object.fromEntries(
          Object.entries(transportView.transports).map(
            ([date, transportItems]) => {
              // Map each transport item to a PlanningItem
              const planningItems: PlanningItem[] = transportItems.map(
                (item) => {
                  let driver: Driver | undefined = undefined;
                  if (item.driver) {
                    driver = {
                      firstName: item.driver.firstName || '',
                      lastName: item.driver.lastName || '',
                    };
                  }

                  return {
                    id: item.id,
                    pickupDate: item.pickupDate,
                    deliveryDate: item.deliveryDate,
                    displayNumber: item.displayNumber || '',
                    originCity: item.originCity || '',
                    destinationCity: item.destinationCity || '',
                    driver: driver,
                    status: item.status,
                    truck: item.truck,
                    containerId: item.containerId,
                    transportType:
                      typeof item.transportType === 'string'
                        ? item.transportType
                        : String(item.transportType),
                    sequenceNumber: item.sequenceNumber,
                    consignorName: item.consignorName,
                  };
                }
              );

              return [date, planningItems];
            }
          )
        ),
      };
    }),
  };
};

export const planningService = {
  list: (date: Date, filters?: PlanningFilterParams) =>
    planningApi
      .getPlanningByDate(
        date.toISOString().split('T')[0],
        filters?.truckId,
        filters?.driverId,
        filters?.statuses?.join(',')
      )
      .then((r) => r.data)
      .then(mapPlanningViewToPlanning),
  reorder: (date: string, licensePlate: string, transportIds: string[]) =>
    planningApi
      .reorderTransports({
        date,
        licensePlate,
        transportIds,
      })
      .then((r) => r.data)
      .then(mapPlanningViewToPlanning),
  getDriverPlanning: (driverId: string, startDate: string, endDate: string) =>
    planningApi
      .getPlanningByDriver(driverId, startDate, endDate)
      .then((r) => r.data),
};
