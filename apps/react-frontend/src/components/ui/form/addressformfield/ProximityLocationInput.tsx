import { FieldValues, Path, useFormContext } from "react-hook-form";
import { NumberFormField } from "../NumberFormField";
import { TextAreaFormField } from "../TextAreaFormField";
import { TextFormField } from "../TextFormField";

interface ProximityLocationInputProps<TFieldValues extends FieldValues> {
    name: Path<TFieldValues>;
    disabled?: boolean;
}

export const ProximityLocationInput = <TFieldValues extends FieldValues>({ name, disabled = false }: ProximityLocationInputProps<TFieldValues>) => {
    const { register, formState: { errors } } = useFormContext<TFieldValues>();
    return (
        <div className="flex flex-col gap-4">
                <TextAreaFormField
                    formHook={{
                        register,
                        errors,
                        name: `${String(name)}.description` as Path<TFieldValues>,
                        rules: {
                            required: 'Beschrijving is verplicht',
                            maxLength: {
                                value: 200,
                                message: 'Beschrijving mag maximaal 200 tekens bevatten'
                            }
                        },
                    }}
                    title="Nabijheidsbeschrijving"
                    placeholder="Beschrijving van de locatie"
                />
            <div className="flex items-start gap-4 self-stretch">
                <TextFormField
                    title={'Plaats'}
                    placeholder={'Vul Plaats in'}
                    disabled={disabled}
                    formHook={{
                        register,
                        name: `${String(name)}.city` as Path<TFieldValues>,
                        rules: {
                            required: 'Plaats is verplicht',
                            maxLength: {
                                value: 24,
                                message: 'Plaats mag maximaal 24 tekens bevatten'
                            }
                        },
                        errors,
                    }}
                />
                <NumberFormField
                    title={'Postcode (alleen cijfers)'}
                    placeholder={'Vier vcijfers'}
                    step={1}
                    disabled={disabled}
                    formHook={{
                        register,
                        name: `${String(name)}.postalCodeDigits` as Path<TFieldValues>,
                        rules: {
                            required: 'Postcode is verplicht',
                            min: {
                                value: 1000,
                                message: 'Postcode moet minimaal 1000 zijn'
                            },
                            max: {
                                value: 9999,
                                message: 'Postcode moet maximaal 9999 zijn'
                            }
                        },
                        errors,
                    }}
                />
            </div>
        </div>
    );
};