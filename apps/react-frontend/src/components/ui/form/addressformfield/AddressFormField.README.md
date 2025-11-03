# AddressFormField Component

## Overview

`AddressFormField` is a comprehensive location input component that handles all types of locations in forms. It provides a clean API that is independent of backend API types and integrates seamlessly with react-hook-form.

## Features

- **5 Location Types**: Dutch address, Company, Project location, Proximity description, and No location
- **react-hook-form Integration**: Full support for validation, error handling, and form state
- **Clean API**: Form data structure independent of backend API models
- **Type-safe**: TypeScript support with discriminated unions
- **Conversion Utilities**: Helpers to convert between API types and form values

## Location Types

### 1. Dutch Address (`dutch_address`)
Manual address input for Dutch addresses.

**Fields:**
- `streetName` - Street name
- `buildingNumber` - Building number
- `buildingNumberAddition` - Optional building number addition
- `postalCode` - Dutch postal code (e.g., "1234AB")
- `city` - City name
- `country` - Country (defaults to "Nederland")

### 2. Company (`company`)
Company address from the company database.

**Fields:**
- `companyId` - UUID of the company
- `companyName` - Optional display name

### 3. Project Location (`project_location`)
Company with a specific project address.

**Fields:**
- `projectLocationId` - Optional UUID for existing project locations
- `companyId` - UUID of the company
- `companyName` - Optional display name
- `streetName`, `buildingNumber`, `buildingNumberAddition`, `postalCode`, `city`, `country`

### 4. Proximity Description (`proximity`)
Approximate location with postal code area.

**Fields:**
- `description` - Description of the location
- `postalCodeDigits` - First 4 digits of postal code
- `city` - City name
- `country` - Country

### 5. No Location (`none`)
When no specific location is required.

**Fields:** None

## Usage

### Basic Usage

```tsx
import { useForm } from 'react-hook-form';
import { AddressFormField } from '@/components/ui/form/AddressFormField';
import { LocationFormValue } from '@/types/forms/LocationFormValue';

interface MyFormValues {
  pickupLocation: LocationFormValue;
  // ... other fields
}

const MyForm = () => {
  const { control, handleSubmit } = useForm<MyFormValues>({
    defaultValues: {
      pickupLocation: {
        type: 'none', // Start with no location
      },
    },
  });

  const onSubmit = (data: MyFormValues) => {
    console.log('Form data:', data.pickupLocation);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <AddressFormField
        control={control}
        name="pickupLocation"
        label="Ophaaladres"
        required
      />
      <button type="submit">Submit</button>
    </form>
  );
};
```

### With API Integration

```tsx
import { useForm } from 'react-hook-form';
import { AddressFormField } from '@/components/ui/form/AddressFormField';
import { LocationFormValue } from '@/types/forms/LocationFormValue';
import { 
  pickupLocationViewToFormValue,
  locationFormValueToPickupLocationRequest 
} from '@/types/forms/locationConverters';

interface MyFormValues {
  name: string;
  pickupLocation: LocationFormValue;
}

const EditForm = ({ existingData }) => {
  // Convert API response to form values
  const defaultValues = {
    name: existingData.name,
    pickupLocation: pickupLocationViewToFormValue(existingData.pickupLocation),
  };

  const { control, handleSubmit } = useForm<MyFormValues>({
    defaultValues,
  });

  const onSubmit = async (data: MyFormValues) => {
    // Convert form values to API request
    const request = {
      name: data.name,
      pickupLocation: locationFormValueToPickupLocationRequest(data.pickupLocation),
    };
    
    await apiService.update(request);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('name')} />
      <AddressFormField
        control={control}
        name="pickupLocation"
        label="Locatie"
      />
      <button type="submit">Save</button>
    </form>
  );
};
```

## Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `control` | `Control<TFieldValues>` | Yes | - | react-hook-form control object |
| `name` | `Path<TFieldValues>` | Yes | - | Field name in the form |
| `label` | `string` | No | `"Locatie"` | Label for the location section |
| `required` | `boolean` | No | `false` | Whether the field is required |
| `disabled` | `boolean` | No | `false` | Whether the field is disabled |
| `testId` | `string` | No | `"address-form-field"` | Test ID for e2e testing |

## Type Definitions

### LocationFormValue

```typescript
type LocationFormValue =
  | DutchAddressFormValue
  | CompanyFormValue
  | ProjectLocationFormValue
  | ProximityFormValue
  | NoLocationFormValue;
```

Each type has a discriminator `type` field and type-specific fields.

### Helper Functions

#### `createEmptyLocationFormValue(type: LocationType): LocationFormValue`

Creates an empty location form value of the specified type with default values.

```typescript
const emptyAddress = createEmptyLocationFormValue('dutch_address');
// Returns: { type: 'dutch_address', streetName: '', buildingNumber: '', ... }
```

#### `pickupLocationViewToFormValue(location: PickupLocationView): LocationFormValue`

Converts API response (PickupLocationView) to form values.

```typescript
const formValue = pickupLocationViewToFormValue(apiResponse.pickupLocation);
```

#### `locationFormValueToPickupLocationRequest(location: LocationFormValue): PickupLocationRequest`

Converts form values to API request format.

```typescript
const apiRequest = locationFormValueToPickupLocationRequest(formData.pickupLocation);
```

## Migration Guide for Existing Forms

Current forms use flat field structure:
```typescript
interface OldFormValues {
  pickupCompanyId: string;
  pickupStreet: string;
  pickupBuildingNumber: string;
  pickupPostalCode: string;
  pickupCity: string;
  // ...
}
```

Should be migrated to:
```typescript
interface NewFormValues {
  pickupLocation: LocationFormValue;
  // ...
}
```

**Steps:**
1. Update form values interface to use `LocationFormValue`
2. Update default values initialization
3. Replace separate address field components with `<AddressFormField />`
4. Update submission logic to use `locationFormValueToPickupLocationRequest`
5. Update edit logic to use `pickupLocationViewToFormValue`

## Implementation Status

✅ Type definitions created  
✅ Conversion utilities implemented  
✅ Component interface implemented  
✅ react-hook-form integration  
⏳ Location type-specific field inputs (TODO)  
⏳ Field validation logic (TODO)  
⏳ Company selector (TODO)  
⏳ Project location management (TODO)  

## TODO

The following features need to be implemented:

1. **Dutch Address Fields**: Text inputs for street, building number, postal code, city
2. **Company Selector**: Searchable dropdown to select companies
3. **Project Location Fields**: Company selector + address fields
4. **Proximity Fields**: Description textarea + postal code digits input
5. **Field Validation**: Type-specific validation rules
6. **Styling**: Consistent styling with design system
7. **E2E Test Support**: Test IDs for all interactive elements

## Testing

When writing tests, use the provided test IDs:

```typescript
// Select location type
await page.locator('[data-testid="address-form-field"]').click();

// Type selector is a ListboxFormField with nested test ID
const typeSelector = page.locator('[data-testid="listbox-form-field"]');
await typeSelector.click();
```

## Related Files

- **Type definitions**: `/src/types/forms/LocationFormValue.ts`
- **Converters**: `/src/types/forms/locationConverters.ts`
- **Component**: `/src/components/ui/form/AddressFormField.tsx`
- **Listbox component**: `/src/components/ui/form/ListboxFormField.tsx`
