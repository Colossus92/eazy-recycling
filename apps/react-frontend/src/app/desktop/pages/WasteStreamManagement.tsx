import { WasteStreamListView } from '@/api/client/models/waste-stream-list-view';
import BxRecycle from '@/assets/icons/BxRecycle.svg?react';
import Plus from '@/assets/icons/Plus.svg?react';
import { ContentContainer } from '@/components/layouts/ContentContainer';
import { Button } from '@/components/ui/button/Button';
import { Column, ContentTable } from '@/features/crud/ContentTable.tsx';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import { useWasteStreamCrud } from '@/features/wasteStream/useWasteStreamCrud.ts';
import { fallbackRender } from '@/utils/fallbackRender';
import { ErrorBoundary } from 'react-error-boundary';
import { ClipLoader } from 'react-spinners';

export const WasteStreamManagement = () => {
  const {
    displayedWasteStreams,
    setQuery,
    error,
    isLoading,
  } = useWasteStreamCrud();

  const columns: Column<WasteStreamListView>[] = [
    { key: 'wasteStreamNumber', label: 'Afvalstroomnummer' },
    { key: 'wasteName', label: 'Gebruikelijke benaming' },
    { key: 'consignorPartyName', label: 'Afzender' },
    { key: 'pickupLocationPostalCode', label: 'Herkomstlocatie' },
    { key: 'deliveryLocationPostalCode', label: 'Bestemmingslocatie' },
  ];

  return (
    <>
      <ContentContainer title={"Afvalstroomnummers"}>
        <div className="flex-1 flex flex-col items-start self-stretch pt-4 gap-4 border border-solid rounded-radius-xl border-color-border-primary bg-color-surface-primary overflow-hidden">
          <ContentTitleBar setQuery={setQuery}>
            <Button
              variant={'primary'}
              icon={Plus}
              label={'Voeg toe'}
              onClick={() => {}}
            />
          </ContentTitleBar>
          {displayedWasteStreams.length === 0 && !error ? (
            isLoading ? (
              <div className="flex justify-center items-center h-24 w-full">
                <ClipLoader
                  size={20}
                  color={'text-color-text-invert-primary'}
                  aria-label="Laad spinner"
                />
              </div>
            ) : (
              <EmptyState
                icon={BxRecycle}
                text="Geen afvalstromen gevonden"
                onClick={() => {}}
              />
            )
          ) : (
            <ErrorBoundary fallbackRender={fallbackRender} onReset={() => {}}>
              <ContentTable<WasteStreamListView>
                data={{
                  items: displayedWasteStreams,
                  columns,
                  setQuery,
                }}
                onEdit={() => {}}
                onDelete={() => {}}
                isLoading={isLoading}
              />
            </ErrorBoundary>
          )}
        </div>
      </ContentContainer>
    </>
  );
};

export default WasteStreamManagement;
