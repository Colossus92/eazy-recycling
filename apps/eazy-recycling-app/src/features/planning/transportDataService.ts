import { format } from 'date-fns';
import { Transport } from '@/api/transportService.ts';
import { ContainerTransportFormValues } from '@/features/planning/hooks/useContainerTransportForm';
import { WasteTransportFormValues } from '@/features/planning/hooks/useWasteTransportForm.ts';

export const transportDataService = {
  apiToContainerTransportFormValues: (
    data: Transport
  ): ContainerTransportFormValues => {
    return {
      transportType: data.transportType,
      containerOperation: data.containerOperation,
      consignorPartyId: data.consignorParty?.id || '',
      carrierPartyId: data.carrierParty?.id || '',

      // Pickup section
      pickupCompanyId: data.pickupCompany?.id || '',
      pickupCompanyBranchId: data.pickupCompanyBranch?.id || '',
      pickupStreet: data.pickupLocation.address.streetName,
      pickupBuildingNumber: data.pickupLocation.address.buildingNumber,
      pickupPostalCode: data.pickupLocation.address.postalCode,
      pickupCity: data.pickupLocation.address.city,
      pickupDateTime: format(
        new Date(data.pickupDateTime),
        "yyyy-MM-dd'T'HH:mm"
      ),

      // Delivery section
      deliveryCompanyId: data.deliveryCompany?.id || '',
      deliveryCompanyBranchId: data.deliveryCompanyBranch?.id || '',
      deliveryStreet: data.deliveryLocation.address.streetName,
      deliveryBuildingNumber: data.deliveryLocation.address.buildingNumber,
      deliveryPostalCode: data.deliveryLocation.address.postalCode,
      deliveryCity: data.deliveryLocation.address.city,
      deliveryDateTime: data.deliveryDateTime
        ? format(new Date(data.deliveryDateTime), "yyyy-MM-dd'T'HH:mm")
        : undefined,

      // Details section
      truckId: data.truck?.licensePlate || '',
      driverId: data.driver?.id || '',
      containerId: data.wasteContainer?.uuid || '',
      note: data.note,
    };
  },

  apiToWasteTransportFormValues: (
    data: Transport
  ): WasteTransportFormValues => {
    if (!data.goods) {
      throw new Error('Cannot convert waste transport: goods data is missing');
    }
    const goods = data.goods;

    return {
      transportType: data.transportType,
      containerOperation: data.containerOperation,
      consignorPartyId: data.consignorParty?.id || '',
      consignorClassification: goods.consignorClassification,
      carrierPartyId: data.carrierParty?.id || '',

      // Pickup section
      pickupCompanyId: data.pickupCompany?.id || '',
      pickupCompanyBranchId: data.pickupCompanyBranch?.id || '',
      pickupStreet: data.pickupLocation.address.streetName,
      pickupBuildingNumber: data.pickupLocation.address.buildingNumber,
      pickupPostalCode: data.pickupLocation.address.postalCode,
      pickupCity: data.pickupLocation.address.city,
      pickupDateTime: format(
        new Date(data.pickupDateTime),
        "yyyy-MM-dd'T'HH:mm"
      ),

      // Delivery section
      deliveryCompanyId: data.deliveryCompany?.id || '',
      deliveryCompanyBranchId: data.deliveryCompanyBranch?.id || '',
      deliveryStreet: data.deliveryLocation.address.streetName,
      deliveryBuildingNumber: data.deliveryLocation.address.buildingNumber,
      deliveryPostalCode: data.deliveryLocation.address.postalCode,
      deliveryCity: data.deliveryLocation.address.city,
      deliveryDateTime: data.deliveryDateTime
        ? format(new Date(data.deliveryDateTime), "yyyy-MM-dd'T'HH:mm")
        : undefined,

      // Details section
      truckId: data.truck?.licensePlate || '',
      driverId: data.driver?.id || '',
      containerId: data.wasteContainer?.uuid || '',
      note: data.note,

      // Goods data
      consigneePartyId: goods.consigneeParty.id,
      pickupPartyId: goods.pickupParty.id,
      wasteStreamNumber: goods.goodsItem.wasteStreamNumber,
      weight: goods.goodsItem.netNetWeight,
      unit: goods.goodsItem.unit,
      quantity: goods.goodsItem.quantity,
      goodsName: goods.goodsItem.name,
      euralCode: goods.goodsItem.euralCode,
      processingMethodCode: goods.goodsItem.processingMethodCode,
    };
  },
};
