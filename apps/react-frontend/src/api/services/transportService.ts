import {
  ContainerTransportControllerApi,
  TransportControllerApi,
  TransportDto,
  TransportFinishedRequest,
  WasteTransportControllerApi,
  WasteStreamDetailViewPickupLocation,
  PickupLocationView
} from '@/api/client';
import { apiInstance } from './apiInstance';
import { ContainerTransportRequest } from '@/api/client/models/container-transport-request';
import {
  CreateContainerTransportRequest,
  CreateContainerTransportRequestContainerOperationEnum
} from '@/api/client/models/create-container-transport-request';
import {
  CreateWasteTransportRequest,
  CreateWasteTransportRequestContainerOperationEnum
} from '@/api/client/models/create-waste-transport-request';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { WasteTransportFormValues } from '@/features/planning/hooks/useWasteTransportForm';
import { format } from 'date-fns';


const transportApi = new TransportControllerApi(apiInstance.config)
const containerTransportApi = new ContainerTransportControllerApi(apiInstance.config)
const wasteTransportApi = new WasteTransportControllerApi(apiInstance.config)

/**
 * Represents a normalized address structure
 */
export interface NormalizedAddress {
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
export const  resolveLocationAddress = (location: PickupLocationView): NormalizedAddress | null => {
  if (!location) {
    return null;
  }

  const locationAny = location as any;
  
  // Handle PickupCompanyView (type === 'company')
  if (locationAny.type === 'company' && locationAny.company?.address) {
    const address = locationAny.company.address;
    return {
      companyName: locationAny.company.name,
      street: address.street || '',
      houseNumber: address.houseNumber || '',
      postalCode: address.postalCode || '',
      city: address.city || '',
      country: address.country
    };
  }
  
  // Handle DutchAddressView (type === 'dutch_address')
  if (locationAny.type === 'dutch_address') {
    return {
      street: locationAny.streetName || locationAny.street || '',
      houseNumber: locationAny.buildingNumber || locationAny.houseNumber || '',
      postalCode: locationAny.postalCode || '',
      city: locationAny.city || '',
      country: locationAny.country || 'Nederland'
    };
  }
  
  // Fallback: try to extract address directly if it exists
  if (locationAny.address) {
    return {
      street: locationAny.address.street || locationAny.address.streetName || '',
      houseNumber: locationAny.address.houseNumber || locationAny.address.buildingNumber || '',
      postalCode: locationAny.address.postalCode || '',
      city: locationAny.address.city || '',
      country: locationAny.address.country
    };
  }

  return null;
}

/**
 * Resolves company ID from a WasteStreamDetailViewPickupLocation union type.
 * Returns the company ID if the location is of type 'company'.
 * 
 * @param location - The pickup or delivery location from TransportDetailView
 * @returns The company ID or undefined if not a company location
 */
export const resolveLocationCompanyId = (location: WasteStreamDetailViewPickupLocation): string | undefined => {
  const locationAny = location as any;
  
  if (locationAny.type === 'company' && locationAny.company?.id) {
    return locationAny.company.id;
  }
  
  return undefined;
}

export const transportService = {
    deleteTransport: (id: string) => transportApi.deleteTransport(id),
    getTransportById: (id: string) => transportApi.getTransportById(id).then((response) => response.data),
    updateContainerTransport: (id: string, data: CreateContainerTransportRequest) => containerTransportApi.updateContainerTransport(id, data),
    createContainerTransport: (data: CreateContainerTransportRequest) => containerTransportApi.createContainerTransport(data),
    createWasteTransport: (data: CreateWasteTransportRequest) => wasteTransportApi.createWasteTransport(data),
    updateWasteTransport: (id: string, data: CreateWasteTransportRequest) => wasteTransportApi.updateWasteTransport(id, data),
    reportFinished: (id: string, data: TransportFinishedRequest) => transportApi.markTransportAsFinished(id, data),
}

export const formValuesToCreateContainerTransportRequest = (formValues: ContainerTransportFormValues) => {



    const request: ContainerTransportRequest = {
        consignorPartyId: formValues.consignorPartyId,
        carrierPartyId: formValues.carrierPartyId,
        containerOperation: formValues.containerOperation as CreateContainerTransportRequestContainerOperationEnum,
        pickupDateTime: formValues.pickupDateTime,
        deliveryDateTime: formValues.deliveryDateTime,
        truckId: formValues.truckId,
        driverId: formValues.driverId,
        containerId: formValues.containerId,
        note: formValues.note || '',
        transportType: "CONTAINER",
        deliveryStreet: "",
        deliveryBuildingNumber: "",
        deliveryPostalCode: "",
        deliveryCity: ""
    }

    //TODO should be possible to just send all available fields

    if (formValues.pickupCompanyBranchId) {
        request.pickupProjectLocationId = formValues.pickupCompanyBranchId
    } else if (formValues.pickupCompanyId) {
        request.pickupCompanyId = formValues.pickupCompanyId
    } else if (formValues.pickupStreet) {
        request.pickupStreet = formValues.pickupStreet
        request.pickupBuildingNumber = formValues.pickupBuildingNumber
        request.pickupPostalCode = formValues.pickupPostalCode
        request.pickupCity = formValues.pickupCity
    }

    if (formValues.deliveryCompanyBranchId) {
        request.deliveryProjectLocationId = formValues.deliveryCompanyBranchId
    } else if (formValues.deliveryCompanyId) {
        request.deliveryCompanyId = formValues.deliveryCompanyId
    } else if (formValues.deliveryStreet) {
        request.deliveryStreet = formValues.deliveryStreet
        request.deliveryBuildingNumber = formValues.deliveryBuildingNumber
        request.deliveryPostalCode = formValues.deliveryPostalCode
        request.deliveryCity = formValues.deliveryCity
    }
    return request
}

export const transportDtoToContainerTransportFormValues = (dto: TransportDto) => {
    const formValues: ContainerTransportFormValues = {
        consignorPartyId: dto.consignorParty?.id || '',
        carrierPartyId: dto.carrierParty?.id || '',
        containerOperation: dto.containerOperation || '',
        pickupCompanyId: dto.pickupCompany?.id || '',
        pickupCompanyBranchId: dto.pickupCompanyBranch?.id || '',
        pickupStreet: dto.pickupLocation.address.streetName || '',
        pickupBuildingNumber: dto.pickupLocation.address.buildingNumber || '',
        pickupPostalCode: dto.pickupLocation.address.postalCode || '',
        pickupCity: dto.pickupLocation.address.city || '',
        pickupDateTime: format(
            new Date(dto.pickupDateTime),
            "yyyy-MM-dd'T'HH:mm"
          ),
        deliveryCompanyId: dto.deliveryCompany?.id || '',
        deliveryCompanyBranchId: dto.deliveryCompanyBranch?.id || '',
        deliveryStreet: dto.deliveryLocation.address.streetName || '',
        deliveryBuildingNumber: dto.deliveryLocation.address.buildingNumber || '',
        deliveryPostalCode: dto.deliveryLocation.address.postalCode || '',
        deliveryCity: dto.deliveryLocation.address.city || '',
        deliveryDateTime: dto.deliveryDateTime
                ? format(new Date(dto.deliveryDateTime), "yyyy-MM-dd'T'HH:mm")
                : undefined,
        truckId: dto.truck?.licensePlate || '',
        driverId: dto.driver?.id || '',
        containerId: dto.wasteContainer?.uuid || '',
        note: dto.note,
        transportType: dto.transportType
    }
    return formValues
}

export const formValuesToCreateWasteTransportRequest = (formValues: WasteTransportFormValues) => {
    const request: CreateWasteTransportRequest = {
        consignorPartyId: formValues.consignorPartyId,
        carrierPartyId: formValues.carrierPartyId,
        containerOperation: formValues.containerOperation as CreateWasteTransportRequestContainerOperationEnum,
        pickupCompanyId: formValues.pickupCompanyId,
        pickupCompanyBranchId: formValues.pickupCompanyBranchId,
        pickupStreet: formValues.pickupStreet,
        pickupBuildingNumber: formValues.pickupBuildingNumber,
        pickupPostalCode: formValues.pickupPostalCode,
        pickupCity: formValues.pickupCity,
        pickupDateTime: formValues.pickupDateTime,
        deliveryCompanyId: formValues.deliveryCompanyId,
        deliveryCompanyBranchId: formValues.deliveryCompanyBranchId,
        deliveryStreet: formValues.deliveryStreet,
        deliveryBuildingNumber: formValues.deliveryBuildingNumber,
        deliveryPostalCode: formValues.deliveryPostalCode,
        deliveryCity: formValues.deliveryCity,
        deliveryDateTime: formValues.deliveryDateTime,
        truckId: formValues.truckId,
        driverId: formValues.driverId,
        containerId: formValues.containerId,
        note: formValues.note || '',
        transportType: "WASTE",
        consigneePartyId: formValues.consigneePartyId,
        pickupPartyId: formValues.pickupPartyId,
        consignorClassification: formValues.consignorClassification,
        wasteStreamNumber: formValues.wasteStreamNumber,
        weight: formValues.weight,
        unit: "kg",
        quantity: formValues.quantity,
        goodsName: formValues.goodsName,
        euralCode: formValues.euralCode,
        processingMethodCode: formValues.processingMethodCode,
    }
    return request
}

export const transportDtoToWasteTransportFormValues = (dto: TransportDto) => {
    const goods = dto.goods;

    const formValues: WasteTransportFormValues = {
        consignorPartyId: dto.consignorParty?.id || '',
        carrierPartyId: dto.carrierParty?.id || '',
        containerOperation: dto.containerOperation || '',
        pickupCompanyId: dto.pickupCompany?.id || '',
        pickupCompanyBranchId: dto.pickupCompanyBranch?.id || '',
        pickupStreet: dto.pickupLocation.address.streetName || '',
        pickupBuildingNumber: dto.pickupLocation.address.buildingNumber || '',
        pickupPostalCode: dto.pickupLocation.address.postalCode || '',
        pickupCity: dto.pickupLocation.address.city || '',
        pickupDateTime: format(
            new Date(dto.pickupDateTime),
            "yyyy-MM-dd'T'HH:mm"
          ),
        deliveryCompanyId: dto.deliveryCompany?.id || '',
        deliveryCompanyBranchId: dto.deliveryCompanyBranch?.id || '',
        deliveryStreet: dto.deliveryLocation.address.streetName || '',
        deliveryBuildingNumber: dto.deliveryLocation.address.buildingNumber || '',
        deliveryPostalCode: dto.deliveryLocation.address.postalCode || '',
        deliveryCity: dto.deliveryLocation.address.city || '',
        deliveryDateTime: dto.deliveryDateTime
                ? format(new Date(dto.deliveryDateTime), "yyyy-MM-dd'T'HH:mm")
                : undefined,
        truckId: dto.truck?.licensePlate || '',
        driverId: dto.driver?.id || '',
        containerId: dto.wasteContainer?.uuid || '',
        note: dto.note,
        transportType: dto.transportType,


      // Goods data
      consignorClassification: goods?.consignorClassification || 0,
      consigneePartyId: goods?.consigneeParty.id || '',
      pickupPartyId: goods?.pickupParty.id || '',
      wasteStreamNumber: goods?.goodsItem.wasteStreamNumber || '',
      weight: goods?.goodsItem.netNetWeight || 0,
      unit: goods?.goodsItem.unit || '',
      quantity: goods?.goodsItem.quantity || 0,
      goodsName: goods?.goodsItem.name || '',
      euralCode: goods?.goodsItem.euralCode || '',
      processingMethodCode: goods?.goodsItem.processingMethodCode || '',
    }
    return formValues
}

/**
 * Example usage of resolveLocationAddress with TransportDetailView
 * 
 * @example
 * ```typescript
 * const transport: TransportDetailView = await transportService.getTransportById(id);
 * 
 * // Resolve pickup location address
 * const pickupAddress = resolveLocationAddress(transport.pickupLocation);
 * if (pickupAddress) {
 *   console.log(`Pickup: ${pickupAddress.street} ${pickupAddress.houseNumber}, ${pickupAddress.city}`);
 * }
 * 
 * // Resolve delivery location address
 * const deliveryAddress = resolveLocationAddress(transport.deliveryLocation);
 * if (deliveryAddress) {
 *   console.log(`Delivery: ${deliveryAddress.street} ${deliveryAddress.houseNumber}, ${deliveryAddress.city}`);
 * }
 * 
 * // Get company ID if location is a company
 * const pickupCompanyId = resolveLocationCompanyId(transport.pickupLocation);
 * if (pickupCompanyId) {
 *   console.log(`Pickup company ID: ${pickupCompanyId}`);
 * }
 * ```
 */
