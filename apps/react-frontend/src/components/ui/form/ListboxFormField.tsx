import { ListboxButton, Listbox, ListboxOption, ListboxOptions } from "@headlessui/react";
import clsx from "clsx";
import CaretDown from '@/assets/icons/CaretDown.svg?react';
import Check from '@/assets/icons/Check.svg?react';
import Unchecked from '@/assets/icons/Unchecked.svg?react';
import { formInputClasses } from '@/styles/formInputClasses.ts';
import { Control, Controller, FieldPath, FieldValues, RegisterOptions } from "react-hook-form";
import { RequiredMarker } from './RequiredMarker';
import { getFieldError } from '@/utils/formErrorUtils';

interface Option {
    value: string;
    label: string;
}

interface ListboxFormFieldProps<TFieldValues extends FieldValues = any> {
    title: string;
    options: Option[];
    /**
     * Optional value override. Use this if the form field value matches a complex object
     * but you want to control the selected option based on a derived value.
     */
    value?: string;
    /**
     * Optional change handler override. Use this if you want to handle the change manually
     * (e.g. to update a complex object) instead of directly setting the field value to the option value.
     */
    onChange?: (value: string) => void;
    /**
     * Whether the field is disabled
     */
    disabled?: boolean;
    /**
     * Optional test ID for e2e testing
     */
    testId?: string;
    /**
     * React Hook Form integration
     */
    formHook: {
        control: Control<TFieldValues>;
        name: FieldPath<TFieldValues>;
        rules?: RegisterOptions<TFieldValues>;
    };
}

export const ListboxFormField = <TFieldValues extends FieldValues = any>({ 
    title, 
    options, 
    value: valueOverride,
    onChange: onChangeOverride,
    disabled = false,
    testId = 'listbox-form-field',
    formHook,
}: ListboxFormFieldProps<TFieldValues>) => {
    const { control, name, rules } = formHook;
    const error = getFieldError(control._formValues, name);

    return (
        <Controller
            control={control}
            name={name}
            rules={rules}
            render={({ field }) => {
                // Use valueOverride if provided, otherwise use field.value
                const currentValue = valueOverride !== undefined ? valueOverride : field.value;
                const selected = options.find(opt => opt.value === currentValue);
                
                const handleChange = (value: string) => {
                    if (onChangeOverride) {
                        onChangeOverride(value);
                    } else {
                        field.onChange(value);
                    }
                };

                const textColorClasses = disabled
                    ? formInputClasses.text.disabled
                    : formInputClasses.text.default;
                const borderColorClasses = disabled
                    ? formInputClasses.border.disabled
                    : error
                        ? formInputClasses.border.error
                        : formInputClasses.border.default;
                const backgroundClasses = disabled
                    ? formInputClasses.background.disabled
                    : formInputClasses.background.hover;

                return (
                    <div className="flex flex-col items-start self-stretch gap-1" data-testid={testId}>
                        <span className="text-caption-2">
                            {title}
                            <RequiredMarker required={rules?.required} />
                        </span>
                        <Listbox value={selected} onChange={(option) => handleChange(option.value)} disabled={disabled}>
                            <ListboxButton
                                className={clsx(
                                    'relative flex h-10 items-center self-stretch gap-2 text-left text-body-1',
                                    formInputClasses.base,
                                    formInputClasses.padding.default,
                                    textColorClasses,
                                    borderColorClasses,
                                    backgroundClasses,
                                    'focus:not-data-focus:outline-none data-focus:outline-2 data-focus:-outline-offset-2 data-focus:outline-white/25'
                                )}
                            >
                                {selected ? <span>{selected.label}</span> : <span className={textColorClasses}>Selecteer een optie</span>}
                                <CaretDown
                                    className={clsx(
                                        "group pointer-events-none absolute top-2.5 right-2.5 size-5",
                                        textColorClasses
                                    )}
                                    aria-hidden="true"
                                />
                            </ListboxButton>
                            <ListboxOptions
                                anchor="bottom"
                                transition
                                className={clsx(
                                    'w-[--button-width] flex flex-col rounded-md border border-color-border-primary bg-color-surface-primary p-2 gap-2 [--anchor-gap:--spacing(1)] focus:outline-none',
                                    'text-body-2',
                                    'transition duration-100 ease-in data-leave:data-closed:opacity-0'
                                )}
                            >
                                {options.map((option) => (
                                    <ListboxOption key={option.value} value={option} className="group flex cursor-default items-center gap-2 rounded-sm px-2 py-1.5 select-none text-color-text-primary hover:bg-color-brand-light">
                                        {selected?.value === option.value ? <Check /> : <Unchecked />}
                                        <span>{option.label}</span>
                                    </ListboxOption>
                                ))}
                            </ListboxOptions>
                        </Listbox>
                        {error && (
                            <span className="text-caption-1 text-color-status-error-dark">
                                {error}
                            </span>
                        )}
                    </div>
                );
            }}
        />
    );
}