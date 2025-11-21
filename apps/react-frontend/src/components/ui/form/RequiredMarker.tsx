import { ValidationRule } from "react-hook-form"

export const RequiredMarker = ({required}: {required: string | ValidationRule<boolean> | undefined}) => {
    return (
        required && <span className="text-color-status-error-dark ml-1">*</span>
    )
}