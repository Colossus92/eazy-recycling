import { UserFormValues } from '@/features/users/UserForm.tsx';

export type Truck = {
  licensePlate: string;
  model: string;
  brand: string;
};

export type WasteStream = {
  number: string;
  name: string;
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

export type WasteContainer = {
  uuid: string;
  id: string;
  location: {
    companyId?: string;
    companyName?: string;
    address?: Address;
  };
  notes?: string;
};

export type Eural = {
  code: string;
  description: string;
};

export type ProcessingMethod = {
  code: string;
  description: string;
};

export type User = {
  id: string;
  email: string;
  roles: string[];
  lastSignInAt: string;
  firstName: string;
  lastName: string;
};

export function toUser(data: UserFormValues) {
  return {
    id: data.id,
    email: data.email,
    roles: data.roles,
    firstName: data.firstName,
    lastName: data.lastName,
    password: data.password,
    lastSignInAt: data.lastSignInAt,
  } as User;
}

export function getWasteContainerLocation(container: WasteContainer): string {
  if (container.location.companyName) {
    return `${container.location.companyName}, ${container.location.address?.city}`;
  }
  if (container.location.address) {
    const address = container.location.address;
    return `${address.streetName} ${address.buildingNumber}, ${address.city}`;
  }
  return '';
}

export type DeleteResponse = { success: boolean };
