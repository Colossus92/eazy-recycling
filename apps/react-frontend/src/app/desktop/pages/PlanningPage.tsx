import { useEffect, useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { ContentContainer } from '@/components/layouts/ContentContainer.tsx';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar.tsx';
import ShippingContainer from '@/assets/icons/ShippingContainer.svg?react';
import BxRecycle from '@/assets/icons/BxRecycle.svg?react';
import { Button } from '@/components/ui/button/Button.tsx';
import { Calendar } from '@/features/planning/components/calendar/Calendar.tsx';
import { ContainerTransportForm } from '@/features/planning/forms/containertransportform/ContainerTransportForm.tsx';
import { Drawer } from '@/components/ui/drawer/Drawer.tsx';
import { PlanningFilterForm } from '@/features/planning/components/filter/PlanningFilterForm';
import { usePlanningFilter } from '@/features/planning/hooks/usePlanningFilter.ts';
import { fallbackRender } from '@/utils/fallbackRender';
import { WasteStreamTransportForm } from '@/features/wastestreams/components/wastetransportform/WasteStreamTransportForm.tsx';
import { useSearchParams } from 'react-router-dom';
import { TransportDetailsDrawer } from '@/features/planning/components/drawer/TransportDetailsDrawer';
import { transportService } from '@/api/services/transportService';
import { toastService } from '@/components/ui/toast/toastService';
import { useQueryClient } from '@tanstack/react-query';
import { useCreateWeightTicketFromTransport } from '@/features/weighttickets/hooks/useCreateWeightTicketFromTransport';

export const PlanningPage = () => {
  const { filters, applyFilterFormValues, isDrawerOpen, setIsDrawerOpen } =
    usePlanningFilter();
  const [isContainerTransportFormOpen, setIsContainerTransportFormOpen] =
    useState(false);
  const [isWasteTransportFormOpen, setIsWasteTransportFormOpen] =
    useState(false);
  const [selectedTransportId, setSelectedTransportId] = useState<
    string | undefined
  >();
  const [calendarDate, setCalendarDate] = useState<Date | undefined>();
  const [isTransportDetailsDrawerOpen, setIsTransportDetailsDrawerOpen] =
    useState(false);
  const [highlightedTransportId, setHighlightedTransportId] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const { createWeightTicket } = useCreateWeightTicketFromTransport();

  // Handle URL params for opening transport details drawer from weight ticket
  useEffect(() => {
    const transportId = searchParams.get('transportId');
    const highlightTransportId = searchParams.get('highlightTransportId');
    const dateParam = searchParams.get('date');

    if (transportId) {
      setSelectedTransportId(transportId);
      setIsTransportDetailsDrawerOpen(true);

      // Set calendar date if provided
      if (dateParam) {
        const parsedDate = new Date(dateParam);
        if (!isNaN(parsedDate.getTime())) {
          setCalendarDate(parsedDate);
        }
      }

      // Clear the URL params after opening
      setSearchParams({});
    } else if (highlightTransportId) {
      // Handle highlight param - just highlight the card, don't open drawer
      setHighlightedTransportId(highlightTransportId);

      // Set calendar date if provided
      if (dateParam) {
        const parsedDate = new Date(dateParam);
        if (!isNaN(parsedDate.getTime())) {
          setCalendarDate(parsedDate);
        }
      }

      // Clear the URL params after setting highlight
      setSearchParams({});

      // Auto-clear highlight after animation completes (7 seconds for 7 pulses)
      setTimeout(() => {
        setHighlightedTransportId(null);
      }, 7000);
    }
  }, [searchParams, setSearchParams]);

  const handleDeleteTransport = async () => {
    if (!selectedTransportId) return;

    try {
      await transportService.deleteTransport(selectedTransportId);
      toastService.success('Transport verwijderd');
      await queryClient.invalidateQueries({ queryKey: ['planning'] });
      setIsTransportDetailsDrawerOpen(false);
      setSelectedTransportId(undefined);
    } catch (error) {
      console.error('Error deleting transport:', error);
      toastService.error('Fout bij het verwijderen van transport');
    }
  };

  const handleEditTransport = () => {
    if (!selectedTransportId) return;

    // Open the waste transport form for editing
    setIsWasteTransportFormOpen(true);
  };

  const handleCreateWeightTicket = async () => {
    if (!selectedTransportId) return;

    await createWeightTicket(selectedTransportId);
    setIsTransportDetailsDrawerOpen(false);
  };

  return (
    <>
      <ContentContainer title={'Planning'}>
        <div className="flex-1 flex flex-col items-start self-stretch pt-4 gap-4 border border-solid rounded-radius-xl border-color-border-primary bg-color-surface-primary overflow-hidden">
          <ContentTitleBar
            setIsFilterOpen={setIsDrawerOpen}
            setQuery={() => {}}
            hideSearchBar
          >
            <Button
              variant={'secondary'}
              icon={ShippingContainer}
              label={'Nieuw Container Transport'}
              onClick={() => setIsContainerTransportFormOpen(true)}
              data-testid="new-container-transport-button"
            />
            <Button
              variant={'primary'}
              icon={BxRecycle}
              label={'Nieuw Afval Transport'}
              onClick={() => setIsWasteTransportFormOpen(true)}
              data-testid="new-waste-transport-button"
            />
          </ContentTitleBar>
          <div className="flex-1 flex flex-col items-start self-stretch h-full">
            <ErrorBoundary fallbackRender={fallbackRender}>
              <Calendar filters={filters} initialDate={calendarDate} highlightedTransportId={highlightedTransportId} />
            </ErrorBoundary>
          </div>
        </div>
      </ContentContainer>
      <ContainerTransportForm
        isOpen={isContainerTransportFormOpen}
        setIsOpen={setIsContainerTransportFormOpen}
      />
      <WasteStreamTransportForm
        isOpen={isWasteTransportFormOpen}
        setIsOpen={(value) => {
          setIsWasteTransportFormOpen(value);
          if (!value) {
            setSelectedTransportId(undefined);
          }
        }}
        transportId={selectedTransportId}
      />
      <Drawer
        title={'Transport planning'}
        isOpen={isDrawerOpen}
        setIsOpen={setIsDrawerOpen}
      >
        <PlanningFilterForm
          onSubmit={applyFilterFormValues}
          closeDialog={() => setIsDrawerOpen(false)}
        />
      </Drawer>
      {selectedTransportId && (
        <TransportDetailsDrawer
          isDrawerOpen={isTransportDetailsDrawerOpen}
          setIsDrawerOpen={(value) => {
            setIsTransportDetailsDrawerOpen(value);
            if (!value) {
              setSelectedTransportId(undefined);
            }
          }}
          transportId={selectedTransportId}
          onDelete={handleDeleteTransport}
          onEdit={handleEditTransport}
          onCreateWeightTicket={handleCreateWeightTicket}
        />
      )}
    </>
  );
};

export default PlanningPage;
