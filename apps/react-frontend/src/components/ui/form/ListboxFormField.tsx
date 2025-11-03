import { ListboxButton, Listbox, ListboxOption, ListboxOptions } from "@headlessui/react";
import { useState } from "react";
import clsx from "clsx";
import CaretDown from '@/assets/icons/CaretDown.svg?react';
import Check from '@/assets/icons/Check.svg?react';
import Unchecked from '@/assets/icons/Unchecked.svg?react';

interface ListboxFormFieldProps {
    title: string;
    options: Option[];
    /**
     * Controlled value (string value, not the full Option object)
     * If provided, the component works in controlled mode
     */
    value?: string;
    /**
     * Callback when value changes (receives string value, not the full Option object)
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
}

interface Option {
    value: string;
    label: string;
}

export const ListboxFormField = ({ 
    title, 
    options, 
    value: controlledValue, 
    onChange: controlledOnChange,
    disabled = false,
    testId = 'listbox-form-field',
}: ListboxFormFieldProps) => {
    // Internal state for uncontrolled mode
    const [internalSelected, setInternalSelected] = useState<Option | undefined>(undefined);

    // Determine if controlled or uncontrolled
    const isControlled = controlledValue !== undefined;
    
    // Get the currently selected Option object
    const selected = isControlled 
        ? options.find(opt => opt.value === controlledValue)
        : internalSelected;

    // Handle selection change
    const handleChange = (newOption: Option) => {
        if (isControlled && controlledOnChange) {
            controlledOnChange(newOption.value);
        } else {
            setInternalSelected(newOption);
        }
    };

    return (
        <div className="flex flex-col items-start self-stretch gap-1" data-testid={testId}>
            <span className="text-caption-2">{title}</span>
            <Listbox value={selected} onChange={handleChange} disabled={disabled}>
                <ListboxButton
                    className={clsx(
                        'relative flex h-10 items-center self-stretch w-full rounded-md bg-color-surface-primary gap-2 py-2 px-3 text-left text-body-1 text-color-text-secondary',
                        'border border-color-border-primary text-color-text-secondary',
                        'hover:bg-color-brand-light hover:border-color-brand-dark hover:text-color-brand-dark',
                        'focus:not-data-focus:outline-none data-focus:outline-2 data-focus:-outline-offset-2 data-focus:outline-white/25'
                    )}
                >
                    {selected ? <span>{selected.label}</span> : <span className="text-color-text-secondary">Selecteer een optie</span>}
                    <CaretDown
                        className="group pointer-events-none absolute top-2.5 right-2.5 size-5 text-color-text-secondary"
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
        </div>
    )
}