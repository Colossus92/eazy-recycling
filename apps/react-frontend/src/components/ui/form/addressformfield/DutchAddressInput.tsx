import { TextFormField } from "../TextFormField";
import { Path, useFormContext } from "react-hook-form";
import { TFieldValues } from "./types";
import { NumberFormField } from "../NumberFormField";
import { PostalCodeFormField } from "../PostalCodeFormField";

interface DutchAddressInputProps {
    /**
     * Name of the parent location field (e.g., "pickupLocation")
     */
    name: Path<TFieldValues>;
    disabled?: boolean;
}

export const DutchAddressInput = ({ name, disabled = false}: DutchAddressInputProps) => {
    const { register, setValue, formState: { errors } } = useFormContext<TFieldValues>();
    return (
        <div className="w-full flex flex-col gap-3">
            <div className="flex items-start self-stretch gap-4">
                <div className="flex items-start flex-grow w-1/2">
                <PostalCodeFormField
                    register={register}
                    setValue={setValue}
                    errors={errors}
                    name={`${String(name)}.postalCode` as Path<TFieldValues>}
                    />
                </div>

                <div className="flex items-start gap-4 w-1/2">
                    <NumberFormField
                        title={'Nummer'}
                        placeholder={''}
                        step={1}
                        formHook={{
                            register,
                            name: `${String(name)}.buildingNumber` as Path<TFieldValues>,
                            rules: { 
                                required: 'Huisnummer is verplicht', 
                                min: {
                                    value: 1,
                                    message: 'Ongeldig'
                                },
                                maxLength: {
                                    value: 10,
                                    message: 'Huisnummer mag maximaal 10 tekens bevatten'
                                }
                            
                            },
                            errors,
                        }}
                        disabled={disabled}
                    />

                    <TextFormField
                        title={'Toevoeging'}
                        placeholder={''}
                        formHook={{
                            register,
                            name: `${String(name)}.buildingNumberAddition` as Path<TFieldValues>,
                            rules: { 
                                maxLength: {
                                    value: 6,
                                    message: 'Toevoeging mag maximaal 6 tekens bevatten'
                                }
                            },
                            errors,
                        }}
                        disabled={disabled}
                    />
                </div>
            </div>

            <div className="flex items-start self-stretch gap-4">
                    <TextFormField
                        title={'Straat'}
                        placeholder={'Vul straatnaam in'}
                        formHook={{
                            register,
                            name: `${String(name)}.streetName` as Path<TFieldValues>,
                            rules: { 
                                required: 'Straat is verplicht', 
                                maxLength: {
                                    value: 43,
                                    message: 'Straatnaam mag maximaal 43 tekens bevatten'
                                }
                            },
                            errors,
                        }}
                        disabled={disabled}
                    />
            </div>
            <div className="flex items-start self-stretch gap-4">

                <TextFormField
                    title={'Plaats'}
                    placeholder={'Vul Plaats in'}
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
                    disabled={disabled}
                />
            </div>
        </div>
    );
}