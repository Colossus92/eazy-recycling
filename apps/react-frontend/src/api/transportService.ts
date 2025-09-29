import { http } from '@/api/http.ts';
import { DriverPlanningItemStatusEnum } from '@/api/client/models/driver-planning-item';

export const transportService = {

  getSignatureStatus: async (id: string): Promise<SignatureStatusView> => {
    const response = await http.get<SignatureStatusView>(
      `/transport/${id}/waybill/signature-status`
    );
    return response.data;
  },

  createSignature: async (
    id: string,
    data: CreateSignatureRequest
  ): Promise<void> => {
    const response = await http.post<void>(
      `/transport/${id}/waybill/signature`,
      data
    );
    return response.data;
  },

  reportFinished: async (id: string, hours: number) => {
    const response = await http.post<Transport>(`/transport/${id}/finished`, {
      hours: hours,
    });
    return response.data;
  },
};

export interface Address {
  streetName: string;
  buildingName: string | null;
  buildingNumber: string;
  city: string;
  postalCode: string;
  country: string;
}

export interface Company {
  id: string;
  chamberOfCommerceId: string;
  vihbId: string;
  name?: string;
  address: Address;
  updatedAt: string;
}

export interface CompanyBranch {
  id: string;
  company: Company;
  address: Address;
}

export interface Location {
  id: string;
  description: string | null;
  locationTypeCode: string | null;
  address: Address;
}

export interface WasteContainerDetail {
  uuid: string;
  id: string;
  company: Company;
  address: Address | null;
  notes: string | null;
}

export interface TruckDetail {
  licensePlate: string;
  brand: string;
  model: string;
  updatedAt: string;
}

export interface DriverDetail {
  id: string;
  firstName: string;
  lastName: string;
  roles: string[] | null;
}

export interface Transport {
  id: string;
  displayNumber: string;
  consignorParty: Company;
  carrierParty: Company;
  pickupCompany: Company;
  pickupCompanyBranch: CompanyBranch;
  pickupLocation: Location;
  pickupDateTime: string;
  deliveryCompany: Company;
  deliveryCompanyBranch: CompanyBranch;
  deliveryLocation: Location;
  deliveryDateTime: string;
  containerOperation: string;
  transportType: string;
  wasteContainer: WasteContainerDetail | null;
  truck: TruckDetail | null;
  driver: DriverDetail | null;
  goods: Goods | null;
  updatedAt: string;
  status: DriverPlanningItemStatusEnum;
  note?: string;
  transportHours: number | null;
}

export interface Goods {
  consigneeParty: Company;
  pickupParty: Company;
  goodsItem: GoodsItem;
  consignorClassification: number;
}

export interface GoodsItem {
  netNetWeight: number;
  unit: string;
  quantity: number;
  name: string;
  euralCode: string;
  processingMethodCode: string;
  wasteStreamNumber?: string;
}

export interface SignatureStatusView {
  transportId: string;
  consignorSigned: boolean;
  carrierSigned: boolean;
  consigneeSigned: boolean;
  pickupSigned: boolean;
}

export interface CreateSignatureRequest {
  signature: string;
  email?: string;
  party: string;
}

export interface LatestPdfView {
  url: string;
  thumbnail: string;
}
