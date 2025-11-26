import { useExactSyncConflicts } from '../hooks/useExactSyncConflicts';
import { SyncConflictsTable } from './SyncConflictsTable';
import { Button } from '@/components/ui/button/Button';
import ArrowCounterClockwise from '@/assets/icons/ArrowCounterClockwise.svg?react';
import { ErrorBoundary } from 'react-error-boundary';
import { fallbackRender } from '@/utils/fallbackRender';
import { ErrorThrowingComponent } from '@/components/ErrorThrowingComponent';

export const ExactSyncConflictsTab = () => {
  const {
    conflicts,
    pendingReviews,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useExactSyncConflicts();

  const totalConflicts = conflicts.length + pendingReviews.length;

  return (
    <>
      {/* Header with stats and refresh button */}
      <div className="flex items-center justify-between w-full px-4">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <span className="text-subtitle-1">Conflicten</span>
            {totalConflicts > 0 && (
              <span className="px-2 py-0.5 bg-color-error-light text-color-error rounded-full text-caption font-medium">
                {totalConflicts}
              </span>
            )}
          </div>
          <div className="flex items-center gap-4 text-body-2 text-color-text-secondary">
            <span>
              <strong className="text-color-error">{conflicts.length}</strong>{' '}
              conflicten
            </span>
            <span>
              <strong className="text-color-warning">
                {pendingReviews.length}
              </strong>{' '}
              handmatige reviews
            </span>
          </div>
        </div>
        <Button
          variant="secondary"
          icon={ArrowCounterClockwise}
          label={isFetching ? 'Vernieuwen...' : 'Vernieuwen'}
          onClick={() => refetch()}
          disabled={isFetching}
        />
      </div>

      {/* Guidance message */}
      {totalConflicts > 0 && (
        <div className="mx-4 p-4 bg-color-warning-light rounded-radius-md border border-color-warning">
          <p className="text-body-2 text-color-text-primary">
            <strong>Conflicten oplossen:</strong> Pas de gegevens aan via{' '}
            <strong>Relaties</strong> in dit systeem of corrigeer de data in{' '}
            <strong>Exact Online</strong>. Na de volgende synchronisatie worden
            opgeloste conflicten automatisch verwijderd.
          </p>
        </div>
      )}

      {/* Table */}
      <ErrorBoundary fallbackRender={fallbackRender}>
        <ErrorThrowingComponent error={error as Error | null} />
        <SyncConflictsTable
          conflicts={conflicts}
          pendingReviews={pendingReviews}
          isLoading={isLoading}
        />
      </ErrorBoundary>
    </>
  );
};
