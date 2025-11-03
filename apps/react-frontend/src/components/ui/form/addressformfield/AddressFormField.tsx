import { Control, Controller, FieldValues, Path } from 'react-hook-form';
import { ListboxFormField } from '../ListboxFormField';
import {
    LocationFormValue,
    LocationType,
    createEmptyLocationFormValue,
} from '@/types/forms/LocationFormValue';
import { DutchAddressInput } from './DutchAddressInput';

interface AddressFormFieldProps<TFieldValues extends FieldValues> {
    /**
     * Name of the field in the form (path to the location field)
     */
    name: Path<TFieldValues>;

    /**
     * Control object from useFormContext
     */
    control: Control<TFieldValues>;

    /**
     * Label for the location section
     */
    label?: string;

    /**
     * Whether the field is required
     */
    required?: boolean;

    /**
     * Whether the field is disabled
     */
    disabled?: boolean;

    /**
     * Optional test ID for e2e testing
     */
    testId?: string;
}

const locationTypeOptions = [
    {
        value: 'company',
        label: 'Bedrijf',
    },
    {
        value: 'project_location',
        label: 'Project locatie',
    },
    {
        value: 'dutch_address',
        label: 'Handmatig adres',
    },
    {
        value: 'proximity',
        label: 'Nabijheidsbeschrijving',
    },
    {
        value: 'none',
        label: 'Geen locatie',
    },
];

export const AddressFormField = <TFieldValues extends FieldValues>({
    name,
    control,
    label = 'Locatie',
    required = false,
    disabled = false,
    testId = 'address-form-field',
}: AddressFormFieldProps<TFieldValues>) => {
    return (
        <Controller
            control={control}
            name={name}
            rules={{
                required: required ? 'Locatie is verplicht' : undefined,
                validate: (value: LocationFormValue) => {
                    // Basic validation - can be extended
                    if (!value || !value.type) {
                        return 'Locatietype is verplicht';
                    }

                    // Type-specific validation will go here
                    switch (value.type) {
                        case 'dutch_address':
                            // TODO: Implement validation
                            break;
                        case 'company':
                            // TODO: Implement validation
                            break;
                        case 'project_location':
                            // TODO: Implement validation
                            break;
                        case 'proximity':
                            // TODO: Implement validation
                            break;
                        case 'none':
                            // No validation needed
                            break;
                    }

                    return true;
                },
            }}
            render={({ field, fieldState }) => {
                const currentLocation = field.value as LocationFormValue;
                const currentType = currentLocation?.type || 'none';

                const handleTypeChange = (newType: LocationType) => {
                    // When type changes, create a new empty location of that type
                    const newLocation = createEmptyLocationFormValue(newType);
                    field.onChange(newLocation);
                };

                // Helper function for updating individual fields within the location
                // Will be used when implementing the specific field inputs
                // const handleFieldChange = (fieldName: string, fieldValue: any) => {
                //   field.onChange({
                //     ...currentLocation,
                //     [fieldName]: fieldValue,
                //   });
                // };

                return (
                    <div
                        className="flex flex-col items-start self-stretch gap-4 p-4 rounded-radius-md bg-color-surface-secondary"
                        data-testid={testId}
                    >
                        <span className="text-subtitle-1">{label}</span>

                        {/* Location Type Selector */}
                        <div className="w-full">
                            <ListboxFormField
                                title="Type locatie"
                                options={locationTypeOptions}
                                value={currentType}
                                onChange={(value) => handleTypeChange(value as LocationType)}
                                disabled={disabled}
                            />
                        </div>

                        {/* Conditional Fields Based on Location Type */}
                        {currentType === 'dutch_address' && (
                            <DutchAddressInput 
                                name={name}
                            />
                        )}

            {currentType === 'company' && (
                <div className="w-full flex flex-col gap-3">
                    {/* TODO: Implement Company Selector */}
                    <div className="text-sm text-gray-500">
                        Company selector will be implemented here
                    </div>
                </div>
            )}

            {currentType === 'project_location' && (
                <div className="w-full flex flex-col gap-3">
                    {/* TODO: Implement Project Location Fields */}
                    <div className="text-sm text-gray-500">
                        Project location fields will be implemented here
                    </div>
                </div>
            )}

            {currentType === 'proximity' && (
                <div className="w-full flex flex-col gap-3">
                    {/* TODO: Implement Proximity Description Fields */}
                    <div className="text-sm text-gray-500">
                        Proximity description fields will be implemented here
                    </div>
                </div>
            )}

            {currentType === 'none' && (
                <div className="w-full">
                    <div className="text-sm text-gray-500">Geen locatie geselecteerd</div>
                </div>
            )}

            {/* Error Display */}
            {fieldState.error && (
                <div className="text-sm text-red-600">{fieldState.error.message}</div>
            )}
          </div >
        );
      }}
    />
  );
};