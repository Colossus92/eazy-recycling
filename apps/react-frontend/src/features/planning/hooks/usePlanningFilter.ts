import { useState } from 'react';
import { PlanningFilterParams } from '@/api/services/planningService';
import { DriverPlanningItemStatusEnum } from '@/api/client';
import { TransportPlanningFormValues } from '@/features/planning/components/filter/PlanningFilterForm';

const defaultFilters: PlanningFilterParams = {
  driverId: undefined,
  truckId: undefined,
  statuses: undefined,
};

export const usePlanningFilter = () => {
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [filters, setFilters] = useState<PlanningFilterParams>(defaultFilters);
  const [formValues, setFormValues] = useState<TransportPlanningFormValues>({
    driverId: '',
    truckId: '',
    isPlanned: false,
    isFinished: false,
    isUnplanned: false,
    isInvoiced: false,
  });

  const applyFilterFormValues = (values: TransportPlanningFormValues) => {
    setFormValues(values);
    const statuses: string[] = [];

    if (values.isPlanned) statuses.push(DriverPlanningItemStatusEnum.Planned);
    if (values.isFinished) statuses.push(DriverPlanningItemStatusEnum.Finished);
    if (values.isUnplanned) statuses.push(DriverPlanningItemStatusEnum.Unplanned);
    if (values.isInvoiced) statuses.push(DriverPlanningItemStatusEnum.Invoiced);

    setFilters({
      driverId: values.driverId || undefined,
      truckId: values.truckId || undefined,
      statuses: statuses.length > 0 ? statuses : undefined,
    });
  };

  return {
    filters,
    formValues,
    applyFilterFormValues,
    isDrawerOpen,
    setIsDrawerOpen,
  };
};
