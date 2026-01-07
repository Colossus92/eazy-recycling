import Plus from '@/assets/icons/Plus.svg?react';
import { ReactNode } from 'react';

interface LineItemSectionProps {
  title: string;
  onAddItem: () => void;
  addButtonTitle: string;
  addButtonDisabledTitle?: string;
  isAddDisabled: boolean;
  emptyMessage?: string;
  showEmptyMessage?: boolean;
  children: ReactNode;
}

/**
 * Generic section component for displaying line items with an add button.
 * Used for both weight ticket lines (materials) and product lines.
 */
export const LineItemSection = ({
  title,
  onAddItem,
  addButtonTitle,
  addButtonDisabledTitle,
  isAddDisabled,
  emptyMessage,
  showEmptyMessage = false,
  children,
}: LineItemSectionProps) => {
  return (
    <div className="flex flex-col items-start self-stretch gap-4 p-4 bg-color-surface-secondary rounded-radius-md">
      <div className="flex justify-between items-center self-stretch">
        <span className="text-subtitle-1">{title}</span>
        <button
          type="button"
          onClick={onAddItem}
          disabled={isAddDisabled}
          className="flex items-center justify-center w-8 h-8 rounded-radius-sm bg-color-primary text-color-on-primary hover:bg-color-primary-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          title={isAddDisabled ? addButtonDisabledTitle : addButtonTitle}
        >
          <Plus className="w-5 h-5" />
        </button>
      </div>

      {showEmptyMessage && emptyMessage && (
        <div className="flex items-center max-w-96">
          <span className="text-body-2 text-color-text-secondary whitespace-normal break-words">
            {emptyMessage}
          </span>
        </div>
      )}

      {children}
    </div>
  );
};
