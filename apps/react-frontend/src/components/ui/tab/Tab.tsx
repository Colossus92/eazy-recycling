import { Tab as TabHeadless } from "@headlessui/react";
import { Fragment } from "react";
import clsx from "clsx";

interface TabProps {
    label: string;
    disabled?: boolean;
}

export const Tab = ({ label, disabled = false }: TabProps) => {

    return (
        <TabHeadless as={Fragment} disabled={disabled}>
            {({ hover, selected }) => (
                <button 
                    className={clsx(
                        "h-10 py-3 px-5 transition-colors text-button font-semibold rounded-t-radius-lg",
                        selected && [
                            "border-x border-t border-b border-t-color-border-primary border-x-color-border-primary",
                            "border-b-color-surface-primary",
                            "text-color-brand-primary",
                            "bg-color-surface-primary",
                        ],
                        hover && !disabled && !selected && [
                            "text-color-brand-primary",
                            "bg-color-brand-light-hover"
                        ],
                        disabled && [
                            "text-color-text-disabled",
                            "cursor-not-allowed"
                        ],
                        
                        !selected && !hover && !disabled && "text-color-text-secondary"
                    )}
                    disabled={disabled}
                >
                    {label}
                </button>
            )}
        </TabHeadless>
    )
}