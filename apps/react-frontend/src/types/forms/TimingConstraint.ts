/**
 * Enum representing the timing mode for transport scheduling.
 * Used for VRPTW (Vehicle Routing Problem with Time Windows) constraints.
 */
export type TimingMode = 'DATE_ONLY' | 'WINDOW' | 'FIXED';

/**
 * Represents a timing constraint for pickup or delivery scheduling.
 * Supports three modes:
 * - DATE_ONLY: Only a date is specified, no time window
 * - WINDOW: A date with a time window (start and end times)
 * - FIXED: A fixed appointment time (start equals end)
 */
export interface TimingConstraint {
  /**
   * The date in ISO format (YYYY-MM-DD)
   */
  date: string;

  /**
   * The timing mode
   */
  mode: TimingMode;

  /**
   * Start time of the window in HH:mm format.
   * Required when mode is WINDOW or FIXED.
   */
  windowStart: string | null;

  /**
   * End time of the window in HH:mm format.
   * Required when mode is WINDOW or FIXED.
   */
  windowEnd: string | null;
}

/**
 * Creates an empty TimingConstraint with DATE_ONLY mode
 */
export const createEmptyTimingConstraint = (): TimingConstraint => ({
  date: '',
  mode: 'DATE_ONLY',
  windowStart: null,
  windowEnd: null,
});

/**
 * Creates a TimingConstraint for a specific date without time window
 */
export const createDateOnlyConstraint = (date: string): TimingConstraint => ({
  date,
  mode: 'DATE_ONLY',
  windowStart: null,
  windowEnd: null,
});

/**
 * Creates a TimingConstraint with a time window
 */
export const createWindowConstraint = (
  date: string,
  windowStart: string,
  windowEnd: string
): TimingConstraint => ({
  date,
  mode: 'WINDOW',
  windowStart,
  windowEnd,
});

/**
 * Creates a TimingConstraint for a fixed appointment
 */
export const createFixedConstraint = (
  date: string,
  time: string
): TimingConstraint => ({
  date,
  mode: 'FIXED',
  windowStart: time,
  windowEnd: time,
});

/**
 * Determines the mode based on the window times
 */
export const determineTimingMode = (
  windowStart: string | null,
  windowEnd: string | null
): TimingMode => {
  if (!windowStart || !windowEnd) {
    return 'DATE_ONLY';
  }
  if (windowStart === windowEnd) {
    return 'FIXED';
  }
  return 'WINDOW';
};

/**
 * Validates a TimingConstraint
 */
export const isValidTimingConstraint = (
  constraint: TimingConstraint
): boolean => {
  if (!constraint.date) {
    return false;
  }

  if (constraint.mode === 'DATE_ONLY') {
    return true;
  }

  if (!constraint.windowStart || !constraint.windowEnd) {
    return false;
  }

  // Parse times and compare
  const [startHour, startMin] = constraint.windowStart.split(':').map(Number);
  const [endHour, endMin] = constraint.windowEnd.split(':').map(Number);
  const startMinutes = startHour * 60 + startMin;
  const endMinutes = endHour * 60 + endMin;

  return startMinutes <= endMinutes;
};

/**
 * Converts a legacy datetime string (YYYY-MM-DDTHH:mm) to TimingConstraint
 */
export const legacyDateTimeToTimingConstraint = (
  dateTime: string | undefined
): TimingConstraint => {
  if (!dateTime) {
    return createEmptyTimingConstraint();
  }

  const [datePart, timePart] = dateTime.split('T');
  if (!timePart) {
    return createDateOnlyConstraint(datePart);
  }

  // Use fixed mode for legacy datetime values (single point in time)
  return createFixedConstraint(datePart, timePart.substring(0, 5));
};

/**
 * Converts a TimingConstraint to legacy datetime string format
 * For backward compatibility with existing API
 */
export const timingConstraintToLegacyDateTime = (
  constraint: TimingConstraint
): string | undefined => {
  if (!constraint.date) {
    return undefined;
  }

  if (constraint.mode === 'DATE_ONLY' || !constraint.windowStart) {
    // Default to start of day for DATE_ONLY mode
    return `${constraint.date}T00:00`;
  }

  // Use windowStart as the datetime
  return `${constraint.date}T${constraint.windowStart}`;
};
