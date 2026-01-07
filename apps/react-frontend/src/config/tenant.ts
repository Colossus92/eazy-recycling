/**
 * Tenant configuration for the current application instance.
 * This should match the backend Tenant configuration.
 */
export const TENANT = {
  processorPartyId: '08797',
  companyName: 'WHD Metaalrecycling',
} as const;

/**
 * Check if a processor party ID belongs to the current tenant
 */
export const isCurrentTenant = (processorPartyId: string): boolean => {
  return processorPartyId === TENANT.processorPartyId;
};
