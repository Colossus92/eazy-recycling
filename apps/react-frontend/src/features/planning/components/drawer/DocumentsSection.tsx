import { ReactNode } from "react"


export const DocumentsSection = ({children}: {children: ReactNode}) => {
    return (
        <div className={'flex flex-col items-start self-stretch gap-3'}>
            <span className={'text-subtitle-1'}>Documenten</span>
            <div className={'flex flex-col items-center gap-2 self-stretch'}>
                {children}
            </div>
        </div>
    )
}