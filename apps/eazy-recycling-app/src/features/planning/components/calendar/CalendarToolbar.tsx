import '@/styles/dayPickerStyles.css';
import { Popover, PopoverButton, PopoverPanel } from '@headlessui/react';
import { addDays, format, subDays } from 'date-fns';
import { nl } from 'date-fns/locale';
import { DayPicker } from 'react-day-picker';
import { Button } from '@/components/ui/button/Button';
import CaretDown from '@/assets/icons/CaretDown.svg?react';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';

interface CalendarToolbarProps {
  date: Date;
  setDate: (value: Date) => void;
}

function formatMonthYear(date: Date): string {
  const month = format(date, 'MMMM', { locale: nl });
  return (
    month.charAt(0).toUpperCase() + month.slice(1) + ' ' + format(date, 'yyyy')
  );
}

export const CalendarToolbar = ({ date, setDate }: CalendarToolbarProps) => {
  return (
    <div
      className={
        'flex items-center self-stretch py-2 px-4 border-y border-solid border-color-border-primary gap-3'
      }
    >
      <Popover className="relative">
        {({ open }) => (
          <>
            <PopoverButton
              as="button"
              className={
                'subtitle-1 cursor-pointer hover:text-color-primary-500 flex items-center gap-1 min-w-[140px]'
              }
            >
              {formatMonthYear(date)}
              <CaretDown className="h-4 w-4 text-color-brand-primary" />
            </PopoverButton>

            {open && (
              <PopoverPanel className="absolute z-10 mt-2 bg-white shadow-lg rounded-lg p-4 border border-color-border-primary">
                <DayPicker
                  mode="single"
                  selected={date}
                  locale={nl}
                  required
                  onSelect={setDate}
                  formatters={{
                    formatCaption: (date) => formatMonthYear(date),
                  }}
                />
              </PopoverPanel>
            )}
          </>
        )}
      </Popover>
      <span className={'subtitle-1'}></span>
      <div className={'flex items-center gap-2'}>
        <Button
          variant="icon"
          icon={CaretLeft}
          showText={false}
          onClick={() => setDate(subDays(date, 7))}
        />

        <Button
          variant="icon"
          icon={CaretRight}
          showText={false}
          onClick={() => setDate(addDays(date, 7))}
        />
      </div>
    </div>
  );
};
