/**
 * Sanitizes street name or city: capitalizes the first letter of each word.
 * Example: "HOOFDSTRAAT" -> "Hoofdstraat", "test street" -> "Test Street"
 */
export function sanitizeStreetOrCity(value: string): string {
  if (!value || value.trim() === '') return value;
  const trimmed = value.trim();
  return trimmed
    .toLowerCase()
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Sanitizes postal code to format "1234 AB".
 * Example: "2691HA" -> "2691 HA", "2691 ha" -> "2691 HA"
 */
export function sanitizePostalCode(value: string): string {
  if (!value || value.trim() === '') return value;
  const cleaned = value.replace(/\s/g, '').trim();
  if (cleaned.length !== 6) return value.trim();

  const digits = cleaned.substring(0, 4);
  const letters = cleaned.substring(4, 6).toUpperCase();
  return `${digits} ${letters}`;
}
