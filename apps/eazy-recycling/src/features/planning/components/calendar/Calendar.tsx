import { useState } from 'react';
import { CalendarToolbar } from '@/features/planning/components/calendar/CalendarToolbar.tsx';
import { CalendarGrid } from '@/features/planning/components/calendar/CalendarGrid.tsx';
import { usePlanning } from '@/features/planning/hooks/usePlanning';
import { PlanningFilterParams } from '@/api/planningService.ts';

interface CalendarProps {
  filters: PlanningFilterParams;
}

export const Calendar = ({ filters }: CalendarProps) => {
  const [date, setDate] = useState(new Date());
  const { planning, isLoading, error } = usePlanning(date, filters);

  if (error) {
    throw error;
  }

  return (
    <div className="flex flex-col h-full w-full">
      <CalendarToolbar date={date} setDate={setDate} />
      <div className="flex-1 w-full overflow-hidden">
        <CalendarGrid planning={planning} isLoading={isLoading} />
      </div>
    </div>
  );
};
