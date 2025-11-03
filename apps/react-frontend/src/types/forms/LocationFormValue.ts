/**
 * Clean form data structure for location fields.
 * This type is independent of API models and designed for optimal form UX.
 */

export type LocationType = 'dutch_address' | 'company' | 'project_location' | 'proximity' | 'none';

/**
 * Base interface for all location form values
 */
interface BaseLocationFormValue {
  type: LocationType;
}

/**
 * Dutch address - Manual address input
 */
export interface DutchAddressFormValue extends BaseLocationFormValue {
  type: 'dutch_address';
  streetName: string;
  buildingNumber: string;
  buildingNumberAddition?: string;
  postalCode: string;
  city: string;
  country: string;
}

/**
 * Company address - Selected company location
 */
export interface CompanyFormValue extends BaseLocationFormValue {
  type: 'company';
  companyId: string;
  companyName?: string; // Optional for display purposes
}

/**
 * Project location - Company with specific project address
 */
export interface ProjectLocationFormValue extends BaseLocationFormValue {
  type: 'project_location';
  projectLocationId?: string; // Optional for existing project locations
  companyId: string;
  companyName?: string; // Optional for display purposes
  streetName: string;
  buildingNumber: string;
  buildingNumberAddition?: string;
  postalCode: string;
  city: string;
  country: string;
}

/**
 * Proximity description - Approximate location
 */
export interface ProximityFormValue extends BaseLocationFormValue {
  type: 'proximity';
  description: string;
  postalCodeDigits: string; // First 4 digits of postal code
  city: string;
  country: string;
}

/**
 * No location - When no specific location is required
 */
export interface NoLocationFormValue extends BaseLocationFormValue {
  type: 'none';
}

/**
 * Union type for all location form values
 */
export type LocationFormValue =
  | DutchAddressFormValue
  | CompanyFormValue
  | ProjectLocationFormValue
  | ProximityFormValue
  | NoLocationFormValue;

/**
 * Helper to create an empty location form value based on type
 */
export const createEmptyLocationFormValue = (type: LocationType): LocationFormValue => {
  switch (type) {
    case 'dutch_address':
      return {
        type: 'dutch_address',
        streetName: '',
        buildingNumber: '',
        buildingNumberAddition: '',
        postalCode: '',
        city: '',
        country: 'Nederland',
      };
    case 'company':
      return {
        type: 'company',
        companyId: '',
        companyName: '',
      };
    case 'project_location':
      return {
        type: 'project_location',
        projectLocationId: '',
        companyId: '',
        companyName: '',
        streetName: '',
        buildingNumber: '',
        buildingNumberAddition: '',
        postalCode: '',
        city: '',
        country: 'Nederland',
      };
    case 'proximity':
      return {
        type: 'proximity',
        description: '',
        postalCodeDigits: '',
        city: '',
        country: 'Nederland',
      };
    case 'none':
      return {
        type: 'none',
      };
  }
};
