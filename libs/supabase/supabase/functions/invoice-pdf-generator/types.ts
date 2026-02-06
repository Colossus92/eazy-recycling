/**
 * Invoice types for PDF generation
 */

export type InvoiceType = 'INKOOPFACTUUR' | 'VERKOOPFACTUUR' | 'CREDITFACTUUR';

export interface Address {
  street: string;
  buildingNumber: string;
  postalCode: string;
  city: string;
  country?: string;
}

export interface TenantInfo {
  name: string;
  address: Address;
  phone: string;
  email: string;
  website: string;
  kvkNumber: string;
  ibanNumber: string;
  vatNumber: string;
  logoUrl?: string;
}

export interface CustomerInfo {
  name: string;
  address: Address;
  creditorNumber: string;
  vatNumber?: string;
}

export interface InvoiceLine {
  date: string;
  description: string[]; // Array of strings, each string is a line in the description
  orderNumber?: string;
  quantity: string;
  unit: string;
  vatCode: string;
  vatPercentage: number | 'G'; // 'G' = geen BTW (no VAT)
  isReverseCharge: boolean;
  pricePerUnit: string;
  totalAmount: string;
}

export interface VatBreakdownLine {
  vatPercentage: number;
  amount: string;
}

export interface InvoiceTotals {
  totalExclVat: string;
  vatBreakdown: VatBreakdownLine[];
  vatAmount: string;
  totalInclVat: string;
}

export interface MaterialTotal {
  material: string;
  totalWeight: string;
  unit: string;
  totalAmount: string;
}

export interface InvoiceData {
  invoiceType: InvoiceType;
  invoiceNumber: string;
  invoiceDate: string;
  creditedInvoiceNumber?: string;
  paymentTermDays: number;
  companyCode: string;
  tenant: TenantInfo;
  customer: CustomerInfo;
  lines: InvoiceLine[];
  materialTotals: MaterialTotal[];
  totals: InvoiceTotals;
}

/**
 * Format a date string to Dutch format
 */
export function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('nl-NL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}
