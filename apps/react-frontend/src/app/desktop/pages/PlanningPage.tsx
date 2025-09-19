import { useState } from 'react';
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
import { WasteTransportForm } from '@/features/planning/forms/wastetransportform/WasteTransportForm.tsx';
import { usePlanningFilter } from '@/features/planning/hooks/usePlanningFilter.ts';
import { fallbackRender } from '@/utils/fallbackRender';

export const PlanningPage = () => {
  const { filters, applyFilterFormValues, isDrawerOpen, setIsDrawerOpen } =
    usePlanningFilter();
  const [isContainerTransportFormOpen, setIsContainerTransportFormOpen] =
    useState(false);
  const [isWasteTransportFormOpen, setIsWasteTransportFormOpen] =
    useState(false);
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
              <Calendar filters={filters} />
            </ErrorBoundary>
          </div>
        </div>
      </ContentContainer>
      <ContainerTransportForm
        isOpen={isContainerTransportFormOpen}
        setIsOpen={setIsContainerTransportFormOpen}
      />
      <WasteTransportForm
        isOpen={isWasteTransportFormOpen}
        setIsOpen={setIsWasteTransportFormOpen}
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
    </>
  );
};

export default PlanningPage;
