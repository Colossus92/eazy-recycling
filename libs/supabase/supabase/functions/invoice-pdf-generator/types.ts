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
  quantity: number;
  unit: string;
  vatCode: string;
  vatPercentage: number | 'G'; // 'G' = geen BTW (no VAT)
  isReverseCharge: boolean;
  pricePerUnit: number;
  totalAmount: number;
}

export interface VatBreakdownLine {
  vatPercentage: number;
  amount: number;
}

export interface InvoiceTotals {
  totalExclVat: number;
  vatBreakdown: VatBreakdownLine[];
  vatAmount: number;
  totalInclVat: number;
}

export interface MaterialTotal {
  material: string;
  totalWeight: number;
  unit: string;
  totalAmount: number;
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
 * Format a number as currency (EUR)
 */
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('nl-NL', {
    style: 'currency',
    currency: 'EUR',
  }).format(amount);
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
