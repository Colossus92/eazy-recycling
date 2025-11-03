import { Control, Controller, FieldValues, Path } from 'react-hook-form';
import { ListboxFormField } from '../ListboxFormField';
import {
    LocationFormValue,
    LocationType,
    createEmptyLocationFormValue,
} from '@/types/forms/LocationFormValue';
import { DutchAddressInput } from './DutchAddressInput';
import { CompanyLocationInput } from './CompanyLocationInput';
import { ProximityLocationInput } from './ProximityLocationInput';

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

    /**
     * Whether the 'Geen locatie' option is allowed
     */
    isNoLocationAllowed?: boolean;
}

const locationTypeOptions = [
    {
        value: 'company',
        label: 'Bedrijf',
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
    isNoLocationAllowed = false,
}: AddressFormFieldProps<TFieldValues>) => {
    const filteredLocationTypeOptions = locationTypeOptions.filter(
        (option) => option.value !== 'none' || isNoLocationAllowed
    );

    return (
        <Controller
            control={control}
            name={name}
            rules={{
                required: required ? 'Locatie is verplicht' : undefined,
            }}
            render={({ field, fieldState }) => {
                const currentLocation = field.value as LocationFormValue;
                const currentType = currentLocation?.type || 'company';

                const handleTypeChange = (newType: LocationType) => {
                    const newLocation = createEmptyLocationFormValue(newType);
                    field.onChange(newLocation);
                };

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
                                options={filteredLocationTypeOptions}
                                value={currentType === 'project_location' ? 'company' : currentType}
                                onChange={(value) => handleTypeChange(value as LocationType)}
                                disabled={disabled}
                            />
                        </div>

                        {currentType === 'dutch_address' && (
                            <DutchAddressInput
                                name={name}
                            />
                        )}

                        {(currentType === 'company' || currentType === 'project_location') && (
                            <CompanyLocationInput
                                name={name}
                            />
                        )}

                        {currentType === 'proximity' && (
                            <div className="w-full flex flex-col gap-3">
                                <ProximityLocationInput
                                    name={name}
                                />
                            </div>
                        )}

                        {currentType === 'none' && (
                            <div className="w-full">
                                <div className="text-sm text-gray-500">Geen locatie geselecteerd</div>
                            </div>
                        )}

                        {/* Error Display */}
                        {fieldState.error && (
                            <span className="text-caption-1 text-color-status-error-dark">{fieldState.error.message}</span>
                        )}
                    </div >
                );
            }}
        />
    );
};