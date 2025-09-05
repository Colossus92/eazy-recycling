import { useState } from 'react';
import { PlanningFilterParams } from '@/api/planningService.ts';
import { Status } from '@/features/planning/hooks/usePlanning';
import { TransportPlanningFormValues } from '@/features/planning/components/filter/PlanningFilterForm';

const defaultFilters: PlanningFilterParams = {
  driverId: undefined,
  truckId: undefined,
  statuses: undefined,
};

export const usePlanningFilter = () => {
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [filters, setFilters] = useState<PlanningFilterParams>(defaultFilters);

  const applyFilterFormValues = (values: TransportPlanningFormValues) => {
    const statuses: string[] = [];

    if (values.isPlanned) statuses.push(Status.PLANNED);
    if (values.isFinished) statuses.push(Status.FINISHED);
    if (values.isUnplanned) statuses.push(Status.UNPLANNED);
    if (values.isInvoiced) statuses.push(Status.INVOICED);

    setFilters({
      driverId: values.driverId || undefined,
      truckId: values.truckId || undefined,
      statuses: statuses.length > 0 ? statuses : undefined,
    });
  };

  return {
    filters,
    applyFilterFormValues,
    isDrawerOpen,
    setIsDrawerOpen,
  };
};
