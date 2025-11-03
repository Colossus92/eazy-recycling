/**
 * Conversion utilities between API types and LocationFormValue
 */

import { PickupLocationView } from '@/api/client/models/pickup-location-view';
import {
  LocationFormValue,
  DutchAddressFormValue,
  CompanyFormValue,
  ProjectLocationFormValue,
  ProximityFormValue,
  NoLocationFormValue,
} from './LocationFormValue';

/**
 * Convert PickupLocationView (API response) to LocationFormValue (form data)
 */
export const pickupLocationViewToFormValue = (
  location: PickupLocationView | null | undefined
): LocationFormValue => {
  if (!location) {
    return { type: 'none' };
  }

  const locationAny = location as any;

  switch (locationAny.type) {
    case 'dutch_address':
      return {
        type: 'dutch_address',
        streetName: locationAny.streetName || '',
        buildingNumber: locationAny.buildingNumber || '',
        buildingNumberAddition: locationAny.buildingNumberAddition || '',
        postalCode: locationAny.postalCode || '',
        city: locationAny.city || '',
        country: locationAny.country || 'Nederland',
      } as DutchAddressFormValue;

    case 'company':
      return {
        type: 'company',
        companyId: locationAny.company?.id || '',
        companyName: locationAny.company?.name || '',
      } as CompanyFormValue;

    case 'project_location':
      return {
        type: 'project_location',
        projectLocationId: locationAny.id,
        companyId: locationAny.company?.id || '',
        companyName: locationAny.company?.name || '',
        streetName: locationAny.streetName || '',
        buildingNumber: locationAny.buildingNumber || '',
        buildingNumberAddition: locationAny.buildingNumberAddition || '',
        postalCode: locationAny.postalCode || '',
        city: locationAny.city || '',
        country: locationAny.country || 'Nederland',
      } as ProjectLocationFormValue;

    case 'proximity':
      return {
        type: 'proximity',
        description: locationAny.description || '',
        postalCodeDigits: locationAny.postalCodeDigits || '',
        city: locationAny.city || '',
        country: locationAny.country || 'Nederland',
      } as ProximityFormValue;

    case 'no_pickup':
    case 'none':
      return {
        type: 'none',
      } as NoLocationFormValue;

    default:
      console.warn('Unknown location type:', locationAny.type);
      return { type: 'none' };
  }
};

/**
 * Convert LocationFormValue (form data) to PickupLocationRequest (API request)
 * Returns the object that can be sent to the backend API
 */
export const locationFormValueToPickupLocationRequest = (
  location: LocationFormValue
): any => {
  switch (location.type) {
    case 'dutch_address':
      return {
        type: 'dutch_address',
        streetName: location.streetName,
        buildingNumber: location.buildingNumber,
        buildingNumberAddition: location.buildingNumberAddition || undefined,
        postalCode: location.postalCode,
        city: location.city,
        country: location.country,
      };

    case 'company':
      return {
        type: 'company',
        companyId: location.companyId,
      };

    case 'project_location':
      return {
        type: 'project_location',
        id: location.projectLocationId,
        companyId: location.companyId,
        streetName: location.streetName,
        buildingNumber: location.buildingNumber,
        buildingNumberAddition: location.buildingNumberAddition || undefined,
        postalCode: location.postalCode,
        city: location.city,
        country: location.country,
      };

    case 'proximity':
      return {
        type: 'proximity',
        description: location.description,
        postalCodeDigits: location.postalCodeDigits,
        city: location.city,
        country: location.country,
      };

    case 'none':
      return {
        type: 'none',
      };
  }
};
