import clsx from 'clsx';

export enum TagColor {
    GREEN = 'GREEN',
    RED = 'RED',
    YELLOW = 'YELLOW',
    GRAY = 'GRAY',
    BLUE = 'BLUE',
}

interface TagProps {
    color: TagColor;
    text: string;
}

export const Tag = ({ color, text }: TagProps) => {
    const baseClasses = 'inline-flex py-1 px-2 justify-center items-center gap-1 rounded-radius-xs text-subtitle-2 font-urbanist';
    const statusClasses = {
        GREEN: 'bg-color-status-success-light text-color-status-success-primary',
        RED: 'bg-color-status-error-light text-color-status-error-dark',
        YELLOW: 'bg-color-status-warning-light text-color-status-warning-primary',
        GRAY: 'bg-[#EAEDF2] text-color-text-secondary',
        BLUE: 'bg-color-brand-light-hover text-color-status-info-dark',
    }[color];

    return <div className={clsx(baseClasses, statusClasses)}>{text}</div>;
};