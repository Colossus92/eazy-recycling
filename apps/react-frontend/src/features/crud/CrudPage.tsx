import type { UseMutateFunction } from '@tanstack/react-query';
import { ReactNode } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { ClipLoader } from 'react-spinners';
import { Column, ContentTable, ExpandableConfig } from './ContentTable.tsx';
import { ContentTitleBar } from './ContentTitleBar.tsx';
import { AdditionalAction } from './ActionMenu.tsx';
import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog.tsx';
import { ContentContainer } from '@/components/layouts/ContentContainer.tsx';
import { Button } from '@/components/ui/button/Button.tsx';
import { fallbackRender } from '@/utils/fallbackRender';
import Plus from '@/assets/icons/Plus.svg?react';

export type DeleteResponse = { success: boolean };

interface CrudPageProps<T, S = T> {
  title: string;
  data: CrudDataProps<T>;
  actions: CrudActionProps<T>;
  renderForm: (
    onClose: () => void,
    onSubmit: (item: T) => void,
    itemToEdit?: T
  ) => ReactNode;
  renderEmptyState: (onClick: () => void) => ReactNode;
  dialogs: CrudDialogProps<T>;
  additionalActions?: AdditionalAction<T>[];
  expandableConfig?: ExpandableConfig<T, S>;
  error?: Error | null;
  onReset?: () => void;
  isLoading?: boolean;
}

export interface CrudDataProps<T> {
  items: T[];
  columns: Column<T>[];
  setQuery: (value: string) => void;
}

export interface CrudDialogProps<T> {
  add: {
    open: boolean;
    onClose: () => void;
    onSave: UseMutateFunction<T, Error, Omit<T, 'id'>, unknown>;
  };
  update: {
    open: boolean;
    item: T | undefined;
    onClose: () => void;
    onSave: UseMutateFunction<T, Error, T, unknown>;
  };
  delete: {
    title: string;
    description: string;
    item: T | undefined;
    onClose: () => void;
    onConfirm: UseMutateFunction<DeleteResponse, Error, T, unknown>;
    open: boolean;
  };
}

export interface CrudActionProps<T> {
  onAdd: (value: boolean) => void;
  onEdit: (item: T | undefined) => void;
  onDelete: (item: T | undefined) => void;
}

const ContentTableWrapper = <T, S = T>({
  data,
  onEdit,
  onDelete,
  additionalActions,
  expandableConfig,
  error,
  isLoading,
}: {
  data: CrudDataProps<T>;
  onEdit: (item: T | undefined) => void;
  onDelete: (item: T | undefined) => void;
  additionalActions?: AdditionalAction<T>[];
  expandableConfig?: ExpandableConfig<T, S>;
  error?: Error | null;
  isLoading?: boolean;
}) => {
  // Throw error to trigger ErrorBoundary
  if (error) {
    throw error;
  }

  return (
    <ContentTable<T, S>
      data={data}
      onEdit={onEdit}
      onDelete={onDelete}
      additionalActions={additionalActions}
      expandableConfig={expandableConfig}
      isLoading={isLoading}
    />
  );
};

export const CrudPage = <T, S = T>({
  title,
  data,
  dialogs,
  actions,
  renderForm,
  renderEmptyState,
  additionalActions,
  expandableConfig,
  error,
  onReset,
  isLoading = false,
}: CrudPageProps<T, S>) => {
  return (
    <>
      <ContentContainer title={title}>
        <div className="flex-1 flex flex-col items-start self-stretch pt-4 gap-4 border border-solid rounded-radius-xl border-color-border-primary bg-color-surface-primary overflow-hidden">
          <ContentTitleBar setQuery={data.setQuery}>
            <Button
              variant={'primary'}
              icon={Plus}
              label={'Voeg toe'}
              onClick={() => actions.onAdd(true)}
            />
          </ContentTitleBar>
          {data.items.length === 0 && !error ? (
            isLoading ? (
              <div className="flex justify-center items-center h-24 w-full">
                <ClipLoader
                  size={20}
                  color={'text-color-text-invert-primary'}
                  aria-label="Laad spinner"
                />
              </div>
            ) : (
              renderEmptyState(() => actions.onAdd(true))
            )
          ) : (
            <ErrorBoundary fallbackRender={fallbackRender} onReset={onReset}>
              <ContentTableWrapper<T, S>
                data={data}
                onEdit={actions.onEdit}
                onDelete={actions.onDelete}
                additionalActions={additionalActions}
                expandableConfig={expandableConfig}
                error={error}
                isLoading={isLoading}
              />
            </ErrorBoundary>
          )}
        </div>
      </ContentContainer>
      {
        <FormDialog isOpen={dialogs.add.open} setIsOpen={dialogs.add.onClose}>
          {renderForm(dialogs.add.onClose, dialogs.add.onSave, undefined)}
        </FormDialog>
      }
      {
        <FormDialog
          isOpen={dialogs.update.open}
          setIsOpen={dialogs.update.onClose}
        >
          {renderForm(
            dialogs.update.onClose,
            dialogs.update.onSave,
            dialogs.update.item
          )}
        </FormDialog>
      }
      {
        <DeleteDialog
          isOpen={dialogs.delete.open}
          setIsOpen={dialogs.delete.onClose}
          onDelete={() =>
            dialogs?.delete?.item &&
            dialogs.delete.onConfirm(dialogs.delete.item)
          }
          title={dialogs.delete.title}
          description={dialogs.delete.description}
        />
      }
    </>
  );
};
