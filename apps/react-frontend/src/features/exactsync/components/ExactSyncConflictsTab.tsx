import { ErrorThrowingComponent } from '@/components/ErrorThrowingComponent';
import { fallbackRender } from '@/utils/fallbackRender';
import { ErrorBoundary } from 'react-error-boundary';
import { useExactSyncConflicts } from '../hooks/useExactSyncConflicts';
import { SyncConflictsTable } from './SyncConflictsTable';

export const ExactSyncConflictsTab = () => {
  const { conflicts, isLoading, error } = useExactSyncConflicts();

  return (
    <>
      {/* Header with stats and refresh button */}
      <div className="flex items-center justify-between w-full px-4">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            {conflicts.length > 0 && (
              <span className="px-2 py-0.5 bg-color-error-light text-color-error rounded-full text-caption font-medium">
                {conflicts.length}{' '}
                {conflicts.length === 1 ? 'conflict' : 'conflicten'}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Guidance message */}
      {conflicts.length > 0 && (
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
        <SyncConflictsTable conflicts={conflicts} isLoading={isLoading} />
      </ErrorBoundary>
    </>
  );
};
