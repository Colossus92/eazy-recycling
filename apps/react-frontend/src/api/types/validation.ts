/**
 * Validation response types from the backend
 */

export interface ValidationErrorResponse {
  code: string;
  description: string;
}

export interface ConsignorDataResponse {
  companyRegistrationNumber: string | null;
  name: string | null;
  country: string | null;
  isPrivate: boolean;
}

export interface PickupLocationDataResponse {
  postalCode: string | null;
  buildingNumber: string | null;
  buildingNumberAddition: string | null;
  city: string | null;
  streetName: string | null;
  proximityDescription: string | null;
  country: string | null;
}

export interface CompanyDataResponse {
  companyRegistrationNumber: string | null;
  name: string | null;
  country: string | null;
}

export interface ValidationRequestDataResponse {
  wasteStreamNumber: string;
  routeCollection: string | null;
  collectorsScheme: string | null;
  consignor: ConsignorDataResponse | null;
  pickupLocation: PickupLocationDataResponse | null;
  deliveryLocation: string | null;
  consignorParty: CompanyDataResponse | null;
  collectorParty: CompanyDataResponse | null;
  dealerParty: CompanyDataResponse | null;
  brokerParty: CompanyDataResponse | null;
  wasteCode: string | null;
  wasteName: string | null;
  processingMethod: string | null;
}

export interface WasteStreamValidationResponse {
  isValid: boolean;
  errors: ValidationErrorResponse[];
  requestData: ValidationRequestDataResponse | null;
}
