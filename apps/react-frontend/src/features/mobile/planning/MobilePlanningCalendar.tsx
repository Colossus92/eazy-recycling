import { Button } from '@headlessui/react';
import { format, isSameDay, addWeeks, subWeeks } from 'date-fns';
import { nl } from 'date-fns/locale';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import { DriverPlanningItem } from '@/api/client/models/driver-planning-item';

interface MobilePlanningCalendarProps {
  weekStart: Date;
  weekEnd: Date;
  selectedDate: Date;
  driverPlanning: Record<string, Record<string, DriverPlanningItem[]>> | null;
  setSelectedDate: (date: Date) => void;
  setCurrentDate: (date: Date) => void;
}

export const MobilePlanningCalendar = ({
  weekStart,
  weekEnd,
  selectedDate,
  driverPlanning,
  setSelectedDate,
  setCurrentDate,
}: MobilePlanningCalendarProps) => {
  const dayNames = ['Ma', 'Di', 'Wo', 'Do', 'Vr', 'Za', 'Zo'];
  const weekDates = [];
  const day = new Date(weekStart);
  while (day <= weekEnd) {
    weekDates.push(new Date(day));
    day.setDate(day.getDate() + 1);
  }
  const goToPreviousWeek = () => {
    setCurrentDate(subWeeks(weekStart, 1));
  };

  const goToNextWeek = () => {
    setCurrentDate(addWeeks(weekStart, 1));
  };

  return (
    <div className="flex items-start self-stretch p-4 gap-4 border-b border-solid border-color-border-primary">
      <div className="flex flex-col items-strat gap-3 flex-1">
        <div className="flex items-center self-stretch py-1 gap-3">
          <Button className={'size-8 p-1.5'} onClick={goToPreviousWeek}>
            <CaretLeft className={'text-color-text-secondary'} />
          </Button>
          <span className={'flex-1 text-center subtitle-1'}>
            {format(weekStart, 'MMMM yyyy', { locale: nl })}
          </span>
          <Button className={'size-8 p-1.5'} onClick={goToNextWeek}>
            <CaretRight className={'text-color-text-secondary'} />
          </Button>
        </div>
        <div className="grid grid-cols-7 gap-1 w-full">
          {dayNames.map((day) => (
            <div
              key={day}
              className="flex justify-center items-center py-2.5 px-2"
            >
              <span className="subtitle-2">{day}</span>
            </div>
          ))}
          {weekDates.map((date, index) => (
            <div
              key={index}
              className="flex justify-center items-center min-h-[40px]"
              onClick={() => {
                sessionStorage.setItem(
                  'eazy-recycling-selected-date',
                  date.toISOString()
                );
                setSelectedDate(date);
              }}
            >
              {!isSameDay(date, selectedDate) ? (
                <div className="flex flex-col justify-center items-center p-2 cursor-pointer">
                  <span className="text-body-2">{date.getDate()}</span>
                  {driverPlanning &&
                    format(date, 'yyyy-MM-dd') in driverPlanning && (
                      <div className="w-[5px] h-[5px] rounded-full bg-color-brand-primary"></div>
                    )}
                </div>
              ) : (
                <div className="flex flex-col justify-center items-center p-2 min-w-[40px] min-h-[40px] border border-color-brand-primary rounded-full bg-color-brand-primary cursor-pointer">
                  <span className="text-body-2 text-color-text-invert-primary">
                    {date.getDate()}
                  </span>
                  {driverPlanning &&
                    format(date, 'yyyy-MM-dd') in driverPlanning && (
                      <div className="w-[5px] h-[5px] rounded-full bg-color-surface-primary"></div>
                    )}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
