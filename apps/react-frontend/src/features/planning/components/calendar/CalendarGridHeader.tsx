import { formatWeekday } from '@/utils/dateUtils.ts';

interface CalendarGridHeaderProps {
  dates: Date[];
}

export const CalendarGridHeader = ({ dates }: CalendarGridHeaderProps) => {
  return (
    <div className="w-full grid grid-cols-8 pr-[15px]">
      <div className="p-2 bg-color-surface-primary border-r border-b border-solid border-color-border-primary " />
      {dates.map((date, index) => {
        return (
          <div
            key={`header-${index}`}
            className="p-2 bg-color-surface-primary border-r border-b border-solid border-color-border-primary"
          >
            <div className="flex justify-between items-center">
              <span className="body-2 opacity-70">{formatWeekday(date)}</span>
              <span className="subtitle-1">{date.getDate()}</span>
            </div>
          </div>
        );
      })}
    </div>
  );
};
