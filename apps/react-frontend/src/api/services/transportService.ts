import {
  ContainerTransportControllerApi,
  PickupLocationView,
  TransportControllerApi,
  TransportDetailView,
  TransportFinishedRequest,
  WasteTransportControllerApi,
  WasteTransportRequest,
  WeightTicketDetailViewPickupLocation,
} from '@/api/client';
import { ContainerTransportRequest } from '@/api/client/models/container-transport-request';
import {
  CreateContainerTransportRequestContainerOperationEnum
} from '@/api/client/models/create-container-transport-request';
import {
  CreateWasteTransportRequestContainerOperationEnum,
} from '@/api/client/models/create-waste-transport-request';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { WasteTransportFormValues } from '@/features/planning/hooks/useWasteTransportForm';
import { format } from 'date-fns';
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
  if (locationAny.type === 'company' && locationAny.company?.address) {
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

  // Handle DutchAddressView (type === 'project_location')
  if (locationAny.type === 'project_location') {
    return {
      companyId: locationAny.company.id,
      companyName: locationAny.company.name,
      street: locationAny.streetName,
      houseNumber: locationAny.buildingNumber,
      postalCode: locationAny.postalCode ,
      city: locationAny.city,
      country: locationAny.country,
    };
  }

  // Fallback: try to extract address directly if it exists
  if (locationAny.address) {
    return {
      street:
        locationAny.address.street || locationAny.address.streetName || '',
      houseNumber:
        locationAny.address.houseNumber ||
        locationAny.address.buildingNumber ||
        '',
      postalCode: locationAny.address.postalCode || '',
      city: locationAny.address.city || '',
      country: locationAny.address.country,
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
  location: WeightTicketDetailViewPickupLocation
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
    pickupStreet: '',
    pickupBuildingNumber: '',
    pickupPostalCode: '',
    pickupCity: '',
    truckId: formValues.truckId,
    driverId: formValues.driverId,
    containerId: formValues.containerId,
    note: formValues.note || '',
    transportType: 'CONTAINER',
    deliveryStreet: '',
    deliveryBuildingNumber: '',
    deliveryPostalCode: '',
    deliveryCity: '',
  };

  //TODO should be possible to just send all available fields

  if (formValues.pickupCompanyBranchId) {
    request.pickupProjectLocationId = formValues.pickupCompanyBranchId;
    request.pickupCompanyId = formValues.pickupCompanyId;
  } else if (formValues.pickupCompanyId) {
    request.pickupCompanyId = formValues.pickupCompanyId;
  } else if (formValues.pickupStreet) {
    request.pickupStreet = formValues.pickupStreet;
    request.pickupBuildingNumber = formValues.pickupBuildingNumber;
    request.pickupPostalCode = formValues.pickupPostalCode;
    request.pickupCity = formValues.pickupCity;
  }

  if (formValues.deliveryCompanyBranchId) {
    request.deliveryProjectLocationId = formValues.deliveryCompanyBranchId;
    request.deliveryCompanyId = formValues.deliveryCompanyId;
  } else if (formValues.deliveryCompanyId) {
    request.deliveryCompanyId = formValues.deliveryCompanyId;
  } else if (formValues.deliveryStreet) {
    request.deliveryStreet = formValues.deliveryStreet;
    request.deliveryBuildingNumber = formValues.deliveryBuildingNumber;
    request.deliveryPostalCode = formValues.deliveryPostalCode;
    request.deliveryCity = formValues.deliveryCity;
  }
  return request;
};

export const transportDetailViewToContainerTransportFormValues = (
  data: TransportDetailView
) => {
  const pickupLocationAddress = resolveLocationAddress(data.pickupLocation);
  const deliveryLocationAddress = resolveLocationAddress(data.deliveryLocation);

  const formValues: ContainerTransportFormValues = {
    consignorPartyId: data.consignorParty?.id || '',
    carrierPartyId: data.carrierParty?.id || '',
    containerOperation: data.containerOperation || '',
    pickupCompanyId: pickupLocationAddress?.companyId || '',
    // pickupCompanyBranchId: data.pickupCompanyBranch?.id || '', //TODO
    pickupStreet: pickupLocationAddress?.street || '',
    pickupBuildingNumber: pickupLocationAddress?.houseNumber || '',
    pickupPostalCode: pickupLocationAddress?.postalCode || '',
    pickupCity: pickupLocationAddress?.city || '',
    pickupDateTime: data.pickupDateTime,
    deliveryCompanyId: deliveryLocationAddress?.companyId || '',
    // deliveryCompanyBranchId: data.deliveryCompanyBranch?.id || '',
    deliveryStreet: deliveryLocationAddress?.street || '',
    deliveryBuildingNumber: deliveryLocationAddress?.houseNumber || '',
    deliveryPostalCode: deliveryLocationAddress?.postalCode || '',
    deliveryCity: deliveryLocationAddress?.city || '',
    deliveryDateTime: data.deliveryDateTime,
    truckId: data.truck?.licensePlate || '',
    driverId: data.driver?.id || '',
    containerId: data.wasteContainer?.uuid || '',
    note: data.note,
    transportType: data.transportType,
  };

  return formValues;
};

export const formValuesToCreateWasteTransportRequest = (
  formValues: WasteTransportFormValues
) => {
  const request: WasteTransportRequest = {
    carrierPartyId: formValues.carrierPartyId,
    containerOperation:
      formValues.containerOperation as CreateWasteTransportRequestContainerOperationEnum,
    pickupDateTime: formValues.pickupDateTime,
    deliveryDateTime: formValues.deliveryDateTime,
    truckId: formValues.truckId,
    driverId: formValues.driverId,
    containerId: formValues.containerId,
    note: formValues.note || '',
    transportType: 'WASTE',
    wasteStreamNumber: formValues.wasteStreamNumber,
    weight: formValues.weight,
    unit: 'kg',
    quantity: formValues.quantity,
  };
  return request;
};

export const transportDtoToWasteTransportFormValues = (transport: TransportDetailView) => {
  const goods = transport.goodsItem;
  const pickupLocationAddress = resolveLocationAddress(transport.pickupLocation);
  const deliveryLocationAddress = resolveLocationAddress(transport.deliveryLocation);

  const formValues: WasteTransportFormValues = {
    consignorPartyId: transport.consignorParty?.id || '',
    carrierPartyId: transport.carrierParty?.id || '',
    containerOperation: transport.containerOperation || '',
    // pickupCompanyBranchId: data.pickupCompanyBranch?.id || '', //TODO
    pickupStreet: pickupLocationAddress?.street || '',
    pickupBuildingNumber: pickupLocationAddress?.houseNumber || '',
    pickupPostalCode: pickupLocationAddress?.postalCode || '',
    pickupCity: pickupLocationAddress?.city || '',
    pickupDateTime: format(new Date(transport.pickupDateTime), "yyyy-MM-dd'T'HH:mm"),
    deliveryCompanyId: deliveryLocationAddress?.companyId || '',
    // deliveryCompanyBranchId: data.deliveryCompanyBranch?.id || '',
    deliveryStreet: deliveryLocationAddress?.street || '',
    deliveryBuildingNumber: deliveryLocationAddress?.houseNumber || '',
    deliveryPostalCode: deliveryLocationAddress?.postalCode || '',
    deliveryCity: deliveryLocationAddress?.city || '',
    deliveryDateTime: transport.deliveryDateTime
      ? format(new Date(transport.deliveryDateTime), "yyyy-MM-dd'T'HH:mm")
      : undefined,
    truckId: transport.truck?.licensePlate || '',
    driverId: transport.driver?.id || '',
    containerId: transport.wasteContainer?.uuid || '',
    note: transport.note,
    transportType: transport.transportType,

    // Goods data
    consigneePartyId: transport.consigneeParty?.id || '',
    pickupPartyId: transport.pickupParty?.id || '',
    wasteStreamNumber: goods?.wasteStreamNumber || '',
    weight: goods?.netNetWeight || 0,
    unit: goods?.unit || '',
    quantity: goods?.quantity || 0,
    goodsName: goods?.name || '',
    euralCode: goods?.euralCode || '',
    processingMethodCode: goods?.processingMethodCode || '',
  };
  return formValues;
};
