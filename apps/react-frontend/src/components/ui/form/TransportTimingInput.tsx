import { useState, useEffect } from 'react';
import clsx from 'clsx';
import {
  TimingConstraint,
  createEmptyTimingConstraint,
} from '@/types/forms/TimingConstraint';
import { RequiredMarker } from './RequiredMarker';

interface TransportTimingInputProps {
  label: string;
  value: TimingConstraint;
  onChange: (value: TimingConstraint) => void;
  required?: boolean;
  disabled?: boolean;
  testId?: string;
  error?: string;
}

export const TransportTimingInput = ({
  label,
  value,
  onChange,
  required = false,
  disabled = false,
  testId,
  error,
}: TransportTimingInputProps) => {
  const [showTimeInputs, setShowTimeInputs] = useState(
    value.mode !== 'DATE_ONLY' && (!!value.windowStart || !!value.windowEnd)
  );

  useEffect(() => {
    if (value.mode !== 'DATE_ONLY' && (value.windowStart || value.windowEnd)) {
      setShowTimeInputs(true);
    }
  }, [value.mode, value.windowStart, value.windowEnd]);

  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange({
      ...value,
      date: e.target.value,
    });
  };

  const handleStartTimeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const timeStr = e.target.value || null;
    const newMode =
      timeStr && value.windowEnd
        ? timeStr === value.windowEnd
          ? 'FIXED'
          : 'WINDOW'
        : 'DATE_ONLY';
    onChange({
      ...value,
      windowStart: timeStr,
      mode: newMode,
    });
  };

  const handleEndTimeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const timeStr = e.target.value || null;
    const newMode =
      value.windowStart && timeStr
        ? value.windowStart === timeStr
          ? 'FIXED'
          : 'WINDOW'
        : 'DATE_ONLY';
    onChange({
      ...value,
      windowEnd: timeStr,
      mode: newMode,
    });
  };

  const handleAddTime = () => {
    setShowTimeInputs(true);
  };

  const handleClearTime = () => {
    setShowTimeInputs(false);
    onChange({
      ...value,
      mode: 'DATE_ONLY',
      windowStart: null,
      windowEnd: null,
    });
  };

  const baseInputClasses =
    'h-10 rounded-radius-md border border-solid bg-color-surface-primary px-3 py-2 text-body-1 w-full';
  const textColorClasses = disabled
    ? 'text-color-text-disabled'
    : 'text-color-text-secondary';
  const borderColorClasses = error
    ? 'border-color-status-error-dark'
    : 'border-color-border-primary';
  const hoverClasses = disabled
    ? 'cursor-not-allowed'
    : 'hover:bg-color-brand-light hover:border-color-brand-dark focus:border-color-brand-dark';

  return (
    <div
      className="flex flex-col items-start self-stretch gap-1"
      data-testid={testId}
    >
      <span className="text-caption-2">
        {label}
        <RequiredMarker required={required} />
      </span>

      <div className="flex flex-col items-start self-stretch gap-2">
        <div className="flex items-center gap-3 w-full">
          <input
            type="date"
            value={value.date || ''}
            onChange={handleDateChange}
            disabled={disabled}
            className={clsx(
              baseInputClasses,
              textColorClasses,
              borderColorClasses,
              hoverClasses,
              'flex-1'
            )}
            data-testid={testId ? `${testId}-date` : undefined}
          />

          {!showTimeInputs && !disabled && (
            <button
              type="button"
              onClick={handleAddTime}
              className="text-caption-1 text-color-brand-primary hover:text-color-brand-dark whitespace-nowrap"
              data-testid={testId ? `${testId}-add-time` : undefined}
            >
              + Tijd toevoegen
            </button>
          )}
        </div>

        {showTimeInputs && (
          <div className="flex items-center gap-3 w-full">
            <div className="flex flex-col gap-1 flex-1">
              <span className="text-caption-2 text-color-text-secondary">
                Begintijd
              </span>
              <input
                type="time"
                value={value.windowStart || ''}
                onChange={handleStartTimeChange}
                disabled={disabled}
                className={clsx(
                  baseInputClasses,
                  textColorClasses,
                  borderColorClasses,
                  hoverClasses
                )}
                data-testid={testId ? `${testId}-start-time` : undefined}
              />
            </div>

            <div className="flex flex-col gap-1 flex-1">
              <span className="text-caption-2 text-color-text-secondary">
                Eindtijd
              </span>
              <input
                type="time"
                value={value.windowEnd || ''}
                onChange={handleEndTimeChange}
                disabled={disabled}
                className={clsx(
                  baseInputClasses,
                  textColorClasses,
                  borderColorClasses,
                  hoverClasses
                )}
                data-testid={testId ? `${testId}-end-time` : undefined}
              />
            </div>

            {!disabled && (
              <button
                type="button"
                onClick={handleClearTime}
                className="self-end h-10 px-2 text-color-text-secondary hover:text-color-status-error-dark"
                aria-label="Tijd wissen"
                data-testid={testId ? `${testId}-clear-time` : undefined}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            )}
          </div>
        )}

        {showTimeInputs && value.windowStart && value.windowEnd && (
          <div className="text-caption-2 text-color-text-secondary">
            {value.mode === 'FIXED' && (
              <span className="text-color-brand-primary">
                Vast afspraakmoment
              </span>
            )}
            {value.mode === 'WINDOW' && (
              <span className="text-color-brand-primary">
                Tijdvenster: {value.windowStart} - {value.windowEnd}
              </span>
            )}
          </div>
        )}
      </div>

      {error && (
        <span className="text-caption-1 text-color-status-error-dark">
          {error}
        </span>
      )}
    </div>
  );
};

export { createEmptyTimingConstraint };
