import { endOfWeek, parseISO, startOfWeek } from 'date-fns';
import { useEffect, useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { useAuth } from '@/components/auth/useAuthHook';
import { MobileHeader } from '@/components/layouts/header/MobileHeader';
import { MobilePlanningCalendar } from '@/features/mobile/planning/MobilePlanningCalendar';
import { MobilePlanningItems } from '@/features/mobile/planning/MobilePlanningItems';
import { useDriverPlanning } from '@/features/planning/hooks/useDriverPlanning';
import { fallbackRender } from '@/utils/fallbackRender';

const MobilePlanningPage = () => {
  const { userId } = useAuth();
  const refresh = () => {
    window.location.reload();
  };
  const storedDate = sessionStorage.getItem('eazy-recycling-selected-date');
  const initialDate = storedDate ? parseISO(storedDate) : new Date();
  const [currentDate, setCurrentDate] = useState<Date>(initialDate);
  const [weekStart, setWeekStart] = useState<Date>(
    startOfWeek(initialDate, { weekStartsOn: 1 })
  );
  const [weekEnd, setWeekEnd] = useState<Date>(
    endOfWeek(initialDate, { weekStartsOn: 1 })
  );
  const [selectedDate, setSelectedDate] = useState<Date>(initialDate);

  useEffect(() => {
    const start = startOfWeek(currentDate, { weekStartsOn: 1 });
    const end = endOfWeek(currentDate, { weekStartsOn: 1 });
    setWeekStart(start);
    setWeekEnd(end);
  }, [currentDate]);

  const { driverPlanning, isLoading, error } = useDriverPlanning({
    driverId: userId || '',
    startDate: weekStart,
    endDate: weekEnd,
  });

  if (!userId) {
    return (
      <div className="flex flex-col items-center justify-center h-screen p-4">
        <p className="text-color-text-error mb-4">
          Gebruikers-ID niet gevonden. Log opnieuw in.
        </p>
      </div>
    );
  }

  return (
    <>
      <MobileHeader />
      <MobilePlanningCalendar
        weekStart={weekStart}
        weekEnd={weekEnd}
        selectedDate={selectedDate}
        driverPlanning={driverPlanning || null}
        setSelectedDate={setSelectedDate}
        setCurrentDate={setCurrentDate}
      />
      <ErrorBoundary fallbackRender={fallbackRender} onReset={refresh}>
        <MobilePlanningItems
          driverPlanning={driverPlanning || null}
          isLoading={isLoading}
          error={error}
          selectedDate={selectedDate}
        />
      </ErrorBoundary>
    </>
  );
};

export default MobilePlanningPage;
