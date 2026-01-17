import { useState, useEffect } from 'react';
import { CalendarToolbar } from '@/features/planning/components/calendar/CalendarToolbar.tsx';
import { CalendarGrid } from '@/features/planning/components/calendar/CalendarGrid.tsx';
import { usePlanning } from '@/features/planning/hooks/usePlanning';
import { PlanningFilterParams } from '@/api/services/planningService.ts';

interface CalendarProps {
  filters: PlanningFilterParams;
  initialDate?: Date;
  highlightedTransportId?: string | null;
}

export const Calendar = ({ filters, initialDate, highlightedTransportId }: CalendarProps) => {
  const [date, setDate] = useState(initialDate || new Date());
  const { planning, isLoading, error } = usePlanning(date, filters);
  
  // Update date when initialDate changes
  useEffect(() => {
    if (initialDate) {
      setDate(initialDate);
    }
  }, [initialDate]);

  if (error) {
    throw error;
  }

  return (
    <div
      className="flex flex-col h-full w-full"
      data-testid="calendar-container"
    >
      <CalendarToolbar date={date} setDate={setDate} />
      <div className="flex-1 w-full overflow-hidden">
        <CalendarGrid planning={planning} isLoading={isLoading} highlightedTransportId={highlightedTransportId} />
      </div>
    </div>
  );
};
