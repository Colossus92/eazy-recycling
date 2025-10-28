import { format } from 'date-fns';

/**
 * Formats a date string from ISO format to a custom format
 * @param dateString - ISO date string (e.g., "2025-05-01T11:55:12.045350Z")
 * @returns Formatted date string (e.g., "01-05-2025 11:55:12")
 */
export const formatDateString = (
  dateString: string | null | undefined
): string => {
  if (!dateString) return '';

  try {
    const date = new Date(dateString);

    // Check if date is valid
    if (isNaN(date.getTime())) {
      return '';
    }

    // Format day, month, and year with leading zeros where needed
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0'); // Month is 0-indexed
    const year = date.getFullYear();

    // Format hours, minutes, and seconds with leading zeros
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');

    // Return the formatted date string
    return `${day}-${month}-${year} ${hours}:${minutes}:${seconds}`;
  } catch (error) {
    console.error('Error formatting date:', error);
    return '';
  }
};

export const formatWeekday = (date: Date) =>
  new Intl.DateTimeFormat('nl-NL', { weekday: 'long' }).format(date);

/**
 * Converts a datetime string to a JavaScript Date object
 * @param dateString - Datetime string (YYYY-MM-DDThh:mm)
 * @returns Date object or null if invalid
 */
export const instantToDate = (dateString: string | null | undefined): Date | null => {
  if (!dateString) {
    return null;
  }
  
  const date = new Date(dateString);
  return isNaN(date.getTime()) ? null : date;
};

/**
 * Formats a datetime string as a date string in CET timezone
 * @param dateString - Datetime string (YYYY-MM-DDThh:mm)
 * @param formatPattern - Format pattern (e.g., 'dd-MM-yyyy', 'dd-MM-yyyy HH:mm')
 * @returns Formatted date string in CET timezone
 */
export const formatInstantInCET = (
  dateString: string | null | undefined,
  formatPattern: 'dd-MM-yyyy' | 'dd-MM-yyyy HH:mm' | 'HH:mm' = 'dd-MM-yyyy HH:mm'
): string => {
  const date = instantToDate(dateString);
  if (!date) return '';

  try {
    return format(date, formatPattern);
  } catch (error) {
    console.error('Error formatting instant in CET:', error);
    return '';
  }
};