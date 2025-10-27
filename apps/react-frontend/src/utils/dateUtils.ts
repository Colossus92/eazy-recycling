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
 * Converts an Instant object to a JavaScript Date object
 * @param instant - Instant object with epochSeconds property
 * @returns Date object or null if invalid
 */
export const instantToDate = (instant: { epochSeconds: number } | null | undefined): Date | null => {
  if (!instant || typeof instant.epochSeconds !== 'number') {
    return null;
  }
  
  // Convert epoch seconds to milliseconds
  return new Date(instant.epochSeconds * 1000);
};

/**
 * Formats an Instant as a date string in CET timezone
 * @param instant - Instant object with epochSeconds property
 * @param formatPattern - Format pattern (e.g., 'dd-MM-yyyy', 'dd-MM-yyyy HH:mm')
 * @returns Formatted date string in CET timezone
 */
export const formatInstantInCET = (
  instant: { epochSeconds: number } | null | undefined,
  formatPattern: 'dd-MM-yyyy' | 'dd-MM-yyyy HH:mm' | 'HH:mm' = 'dd-MM-yyyy HH:mm'
): string => {
  const date = instantToDate(instant);
  if (!date) return '';

  try {
    const options: Intl.DateTimeFormatOptions = {
      timeZone: 'Europe/Amsterdam', // CET/CEST timezone
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    };

    // Add time parts if format includes time
    if (formatPattern.includes('HH:mm')) {
      options.hour = '2-digit';
      options.minute = '2-digit';
      options.hour12 = false;
    }

    const formatter = new Intl.DateTimeFormat('nl-NL', options);
    const parts = formatter.formatToParts(date);
    
    // Extract parts
    const day = parts.find(p => p.type === 'day')?.value || '';
    const month = parts.find(p => p.type === 'month')?.value || '';
    const year = parts.find(p => p.type === 'year')?.value || '';
    const hour = parts.find(p => p.type === 'hour')?.value || '';
    const minute = parts.find(p => p.type === 'minute')?.value || '';

    // Format according to pattern
    if (formatPattern === 'dd-MM-yyyy') {
      return `${day}-${month}-${year}`;
    } else if (formatPattern === 'HH:mm') {
      return `${hour}:${minute}`;
    } else {
      return `${day}-${month}-${year} ${hour}:${minute}`;
    }
  } catch (error) {
    console.error('Error formatting instant in CET:', error);
    return '';
  }
};
