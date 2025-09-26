export type Truck = {
  licensePlate: string;
  model: string;
  brand: string;
};

export type Company = {
  id?: string;
  name: string;
  address: Address;
  chamberOfCommerceId: string | null;
  vihbId: string | null;
  branches?: CompanyBranch[];
};

export type CompanyBranch = {
  id?: string;
  companyId: string;
  address: Address;
};

export type Address = {
  streetName: string;
  buildingNumber: string;
  postalCode: string;
  city: string;
};

export type DeleteResponse = { success: boolean };
