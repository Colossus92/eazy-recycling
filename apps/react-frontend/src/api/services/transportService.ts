import {
  ContainerTransportControllerApi,
  CreateWasteTransportFromWeightTicketRequest,
  PickupLocationView,
  TransportControllerApi,
  TransportDetailView,
  TransportFinishedRequest,
  WasteTransportControllerApi,
  WasteTransportRequest,
} from '@/api/client';
import { ContainerTransportRequest } from '@/api/client/models/container-transport-request';
import { CreateContainerTransportRequestContainerOperationEnum } from '@/api/client/models/create-container-transport-request';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import {
  locationFormValueToPickupLocationRequest,
  pickupLocationViewToFormValue,
} from '@/types/forms/locationConverters';
import { apiInstance } from './apiInstance';

const transportApi = new TransportControllerApi(apiInstance.config);
const containerTransportApi = new ContainerTransportControllerApi(
  apiInstance.config
);
const wasteTransportApi = new WasteTransportControllerApi(apiInstance.config);

/**
 * Represents a normalized address structure
 */
export interface NormalizedAddress {
  companyId?: string;
  companyName?: string;
  street: string;
  houseNumber: string;
  postalCode: string;
  city: string;
  country?: string;
}

/**
 * Resolves address details from a WasteStreamDetailViewPickupLocation union type.
 * Supports PickupCompanyView (type === 'company') and DutchAddressView (type === 'dutch_address').
 *
 * @param location - The pickup or delivery location from TransportDetailView
 * @returns A normalized address object or null if location is invalid
 */
export const resolveLocationAddress = (
  location: PickupLocationView
): NormalizedAddress | null => {
  if (!location) {
    return null;
  }

  const locationAny = location as any;

  // Handle PickupCompanyView (type === 'company')
  if (locationAny.type === 'company') {
    const address = locationAny.company.address;
    return {
      companyId: locationAny.company.id,
      companyName: locationAny.company.name,
      street: address.street || '',
      houseNumber: address.houseNumber || '',
      postalCode: address.postalCode || '',
      city: address.city || '',
      country: address.country,
    };
  }

  // Handle DutchAddressView (type === 'dutch_address')
  if (locationAny.type === 'dutch_address') {
    return {
      street: locationAny.streetName || locationAny.street || '',
      houseNumber: locationAny.buildingNumber || locationAny.houseNumber || '',
      postalCode: locationAny.postalCode || '',
      city: locationAny.city || '',
      country: locationAny.country || 'Nederland',
    };
  }

  // Handle ProjectLocationView (type === 'project_location')
  if (locationAny.type === 'project_location') {
    return {
      companyId: locationAny.company.id,
      companyName: locationAny.company.name,
      street: locationAny.streetName,
      houseNumber: locationAny.buildingNumber,
      postalCode: locationAny.postalCode,
      city: locationAny.city,
      country: locationAny.country,
    };
  }

  // Fallback: try to extract address directly if it exists
  if (locationAny.type === 'proximity') {
    return {
      street: locationAny.description,
      houseNumber: '',
      postalCode: locationAny.postalCodeDigits,
      city: locationAny.city,
      country: locationAny.country,
    };
  }

  return null;
};

/**
 * Resolves company ID from a WasteStreamDetailViewPickupLocation union type.
 * Returns the company ID if the location is of type 'company'.
 *
 * @param location - The pickup or delivery location from TransportDetailView
 * @returns The company ID or undefined if not a company location
 */
export const resolveLocationCompanyId = (
  location: PickupLocationView
): string | undefined => {
  const locationAny = location as any;

  if (locationAny.type === 'company' && locationAny.company?.id) {
    return locationAny.company.id;
  }

  return undefined;
};

export const transportService = {
  deleteTransport: (id: string) => transportApi.deleteTransport(id),
  getTransportById: (id: string) =>
    transportApi.getTransportById(id).then((response) => response.data),
  updateContainerTransport: (id: string, data: ContainerTransportRequest) =>
    containerTransportApi.updateContainerTransport(id, data),
  createContainerTransport: (data: ContainerTransportRequest) =>
    containerTransportApi.createContainerTransport(data),
  createWasteTransport: (data: WasteTransportRequest) =>
    wasteTransportApi.createWasteTransport(data),
  updateWasteTransport: (id: string, data: WasteTransportRequest) =>
    wasteTransportApi.updateWasteTransport(id, data),
  reportFinished: (id: string, data: TransportFinishedRequest) =>
    transportApi.markTransportAsFinished(id, data),
};

export const formValuesToCreateContainerTransportRequest = (
  formValues: ContainerTransportFormValues
) => {
  const request: ContainerTransportRequest = {
    consignorPartyId: formValues.consignorPartyId,
    carrierPartyId: formValues.carrierPartyId,
    containerOperation:
      formValues.containerOperation as CreateContainerTransportRequestContainerOperationEnum,
    pickupDateTime: formValues.pickupDateTime,
    deliveryDateTime: formValues.deliveryDateTime,
    pickupLocation: locationFormValueToPickupLocationRequest(
      formValues.pickupLocation
    ),
    deliveryLocation: locationFormValueToPickupLocationRequest(
      formValues.deliveryLocation
    ),
    truckId: formValues.truckId || undefined,
    driverId: formValues.driverId || undefined,
    containerId: formValues.containerId || undefined,
    note: formValues.note || '',
    transportType: 'CONTAINER',
  };

  return request;
};

export const transportDetailViewToContainerTransportFormValues = (
  data: TransportDetailView
) => {
  const formValues: ContainerTransportFormValues = {
    consignorPartyId: data.consignorParty?.id || '',
    carrierPartyId: data.carrierParty?.id || '',
    containerOperation: data.containerOperation || 'DELIVERY',
    pickupLocation: pickupLocationViewToFormValue(data.pickupLocation),
    pickupDateTime: data.pickupDateTime,
    deliveryLocation: pickupLocationViewToFormValue(data.deliveryLocation),
    deliveryDateTime: data.deliveryDateTime,
    truckId: data.truck?.licensePlate || '',
    driverId: data.driver?.id || '',
    containerId: data.wasteContainer?.id || '',
    note: data.note,
    transportType: data.transportType,
  };

  return formValues;
};

/**
 * Creates a waste transport from a weight ticket
 */
export const createWasteTransportFromWeightTicket = async (
  weightTicketId: number,
  pickupDateTime: string,
  deliveryDateTime?: string
) => {
  const request: CreateWasteTransportFromWeightTicketRequest = {
    weightTicketId,
    pickupDateTime,
    deliveryDateTime,
  };
  const response =
    await wasteTransportApi.createWasteTransportFromWeightTicket(request);
  return response.data;
};
