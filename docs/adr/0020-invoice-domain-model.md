# ADR-0020: Invoice Domain Model

## Status

Proposed

## Context and Problem Statement

Eazy Recycling needs to support invoicing functionality for both purchase (inkoop) and sales (verkoop) transactions. The system must:

1. **Create invoices from scratch** or **from completed weight tickets**
2. **Support two invoice types**: 
   - **Inkoop** (purchase): Customer delivers materials to the company
   - **Verkoop** (sales): Company delivers materials to a processor
3. **Support credit notes**: Negative invoices to correct previous invoices
4. **Handle invoice lines**: Catalog items (materials, products, services) from weight tickets or added manually
5. **Comply with Dutch tax authority regulations** for invoice numbering
6. **Support legacy system transition**: Invoices from both legacy and new system must coexist with unique identifiers
7. **Enable future extension** to cash receipts (kasbon)

### Current State

#### Relevant Existing Entities

**Weight Tickets** (`weight_tickets` table):
- `id` (bigint): Primary key, sequential
- `consignor_party_id` (uuid): FK to companies
- `direction`: INBOUND (inkoop) or OUTBOUND (verkoop)
- `status`: DRAFT, COMPLETED, INVOICED, CANCELLED
- Links to `weight_ticket_lines` with waste stream references and weights

**Catalog Items** (`catalog_items` table):
- `id` (bigint): Primary key
- `code`, `name`: Item identification
- `item_type`: MATERIAL, PRODUCT, SERVICE
- `vat_code`: Links to VAT rates
- `unit_of_measure`: kg, ton, uur, stuks, etc.
- `default_price`, `currency`: Default pricing info

**VAT Rates** (`vat_rates` table):
- `vat_code`: Primary key (e.g., "V21", "V0")
- `percentage`: VAT percentage
- `validity`: Time range for rate validity

**Transports** (`transports` table):
- `id` (uuid): Primary key
- `weight_ticket_id` (bigint): Optional link to weight ticket
- `transport_hours`: Duration for billing

**Companies** (`companies` table):
- `id` (uuid): Primary key
- `code`: Customer number
- `name`, address fields: Customer details
- No `btw_nummer` (VAT number) field currently exists

---

## Considered Options

### Option 1: Invoice as Aggregate Root with Polymorphic Lines

Create `Invoice` as a new aggregate root with invoice lines that reference catalog items.

**Structure**:
```
Invoice (Aggregate Root)
├── InvoiceId
├── InvoiceNumber? (assigned on finalization, year-prefixed, e.g., "ER-2025-00001")
├── InvoiceType (PURCHASE / SALE)
├── InvoiceDocumentType (INVOICE / CREDIT_NOTE)
├── InvoiceStatus (DRAFT / FINAL)
├── CustomerSnapshot (denormalized customer data)
├── InvoiceDate
├── WeightTicketIds[] (multiple weight tickets)
├── TransportIds[] (multiple transports)
├── OriginalInvoiceId? (for credit notes)
└── InvoiceLines[]
    └── InvoiceLine (references catalog item + quantity + price)
```

### Option 2: Separate Invoice Types with Shared Base

Create separate `PurchaseInvoice` and `SalesInvoice` aggregates with shared value objects.

### Option 3: Single Invoice Entity with Line Type Discriminator

Single `Invoice` entity where lines reference catalog items directly without polymorphism.

---

## Decision Outcome

**Chosen option: Option 1 - Invoice as Aggregate Root with Polymorphic Lines**

This approach provides:
- Clean separation of concerns
- Unified catalog item reference for all line types (materials, products, services)
- Easy extension for future cash receipts (same structure, different payment method)
- Maintains referential integrity to weight tickets and transports

---

## Detailed Design

### 1. Domain Model

#### Invoice Aggregate

```kotlin
class Invoice(
    val id: InvoiceId,
    var invoiceNumber: InvoiceNumber?,  // Assigned on finalization, null for drafts
    val invoiceType: InvoiceType,
    val documentType: InvoiceDocumentType,
    var status: InvoiceStatus,
    val invoiceDate: LocalDate,
    
    // Customer snapshot (denormalized at invoice creation)
    val customerSnapshot: CustomerSnapshot,
    
    // Note: Weight tickets and transports reference this invoice via their invoice_id FK
    // Use repository queries to find linked documents
    
    // For credit notes: reference to original invoice
    val originalInvoiceId: InvoiceId?,
    
    // Lines
    val lines: MutableList<InvoiceLine>,
    
    // Audit
    val createdAt: Instant,
    val createdBy: String?,
    var updatedAt: Instant?,
    var updatedBy: String?,
    var finalizedAt: Instant?,
    var finalizedBy: String?,
) {
    fun finalize(invoiceNumber: InvoiceNumber) {
        require(status == InvoiceStatus.DRAFT) { "Only draft invoices can be finalized" }
        this.invoiceNumber = invoiceNumber
        this.status = InvoiceStatus.FINAL
        this.finalizedAt = Instant.now()
    }
}

enum class InvoiceType {
    PURCHASE,  // Inkoop - customer delivers materials to us
    SALE       // Verkoop - we deliver materials to processor
}

enum class InvoiceDocumentType {
    INVOICE,      // Regular invoice
    CREDIT_NOTE   // Credit note (negative invoice)
}

enum class InvoiceStatus {
    DRAFT,
    FINAL
}
```

#### Invoice Number Value Object

```kotlin
data class InvoiceNumber(val value: String) {
    companion object {
        fun generate(prefix: String, year: Int, sequence: Long): InvoiceNumber {
            val formatted = "$prefix-$year-${sequence.toString().padStart(5, '0')}"
            return InvoiceNumber(formatted)
        }
        
        fun parse(value: String): InvoiceNumber = InvoiceNumber(value)
    }
}
```

#### Customer Snapshot Value Object

```kotlin
data class CustomerSnapshot(
    val companyId: CompanyId,
    val customerNumber: String?,      // companies.code
    val name: String,
    val address: AddressSnapshot,
    val vatNumber: String?,           // BTW nummer (optional)
)

data class AddressSnapshot(
    val streetName: String,
    val buildingNumber: String?,
    val buildingNumberAddition: String?,
    val postalCode: String,
    val city: String,
    val country: String?,
)
```

#### Invoice Line

```kotlin
data class InvoiceLine(
    val id: InvoiceLineId,
    val lineNumber: Int,
    val date: LocalDate,
    val description: String,
    val orderReference: String?,  // Weight ticket number, transport number, etc.
    val vatCode: String,
    val vatPercentage: BigDecimal,  // Snapshot at invoice creation (rates can change)
    val glAccountCode: String?,     // Ledger account code for bookkeeping
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val totalExclVat: BigDecimal,
    
    // Catalog item snapshot (denormalized at invoice creation)
    val catalogItemId: Long,
    val catalogItemCode: String,
    val catalogItemName: String,
    val catalogItemType: CatalogItemType,  // MATERIAL, PRODUCT, SERVICE
    val unitOfMeasure: String,
)

enum class CatalogItemType {
    MATERIAL,  // Physical materials (waste streams)
    PRODUCT,   // Physical products (containers, etc.)
    SERVICE    // Services (transport hours, etc.)
}
```

#### Invoice Totals (Calculated)

**VAT Rounding Rule**: VAT is calculated per line but **rounded to 2 decimals on the total VAT amount**, not per line. This ensures consistency with Exact Online bookkeeping software.

```kotlin
data class InvoiceTotals(
    val totalExclVat: BigDecimal,
    val vatBreakdown: List<VatBreakdownLine>,
    val totalVat: BigDecimal,           // Rounded to 2 decimals
    val totalInclVat: BigDecimal,
)

data class VatBreakdownLine(
    val vatCode: String,
    val vatPercentage: BigDecimal,
    val baseAmount: BigDecimal,
    val vatAmount: BigDecimal,          // Unrounded (full precision)
)

fun Invoice.calculateTotals(): InvoiceTotals {
    val totalExclVat = lines.sumOf { it.totalExclVat }
    
    // Group by VAT code and calculate VAT per group (unrounded)
    val vatBreakdown = lines
        .groupBy { it.vatCode to it.vatPercentage }
        .map { (key, groupLines) ->
            val (vatCode, vatPercentage) = key
            val baseAmount = groupLines.sumOf { it.totalExclVat }
            val vatAmount = baseAmount * vatPercentage / BigDecimal(100)  // No rounding here
            VatBreakdownLine(vatCode, vatPercentage, baseAmount, vatAmount)
        }
    
    // Sum all VAT amounts, then round the total to 2 decimals
    val totalVat = vatBreakdown
        .sumOf { it.vatAmount }
        .setScale(2, RoundingMode.HALF_UP)  // Only round the final total
    val totalInclVat = totalExclVat + totalVat
    
    return InvoiceTotals(totalExclVat, vatBreakdown, totalVat, totalInclVat)
}
```

### 2. Database Schema

```sql
-- Invoice number tracking table (single sequence per year for all document types)
CREATE TABLE invoice_number_sequences (
    year INT NOT NULL PRIMARY KEY,
    last_sequence BIGINT NOT NULL DEFAULT 0
);

-- Main invoice table
CREATE TABLE invoices (
    id BIGINT PRIMARY KEY,
    
    -- Invoice identification (single formatted column, null until finalized)
    invoice_number TEXT UNIQUE,  -- e.g., "ER-2025-00001", assigned on finalization
    invoice_type TEXT NOT NULL,  -- PURCHASE, SALE
    document_type TEXT NOT NULL,  -- INVOICE, CREDIT_NOTE
    status TEXT NOT NULL,
    invoice_date DATE NOT NULL,
    
    -- Customer snapshot (denormalized)
    customer_company_id UUID NOT NULL REFERENCES companies(id),
    customer_number TEXT,
    customer_name TEXT NOT NULL,
    customer_street_name TEXT NOT NULL,
    customer_building_number TEXT,
    customer_building_number_addition TEXT,
    customer_postal_code TEXT NOT NULL,
    customer_city TEXT NOT NULL,
    customer_country TEXT,
    customer_vat_number TEXT,
    
    -- For credit notes: reference to original invoice
    original_invoice_id BIGINT REFERENCES invoices(id),
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_by TEXT,
    finalized_at TIMESTAMPTZ,
    finalized_by TEXT
);

-- Add invoice reference to weight_tickets (one invoice can have multiple WTs)
ALTER TABLE weight_tickets ADD COLUMN invoice_id BIGINT REFERENCES invoices(id);
CREATE INDEX idx_weight_tickets_invoice ON weight_tickets(invoice_id) WHERE invoice_id IS NOT NULL;

-- Add invoice reference to transports (one invoice can have multiple transports)
ALTER TABLE transports ADD COLUMN invoice_id BIGINT REFERENCES invoices(id);
CREATE INDEX idx_transports_invoice ON transports(invoice_id) WHERE invoice_id IS NOT NULL;

-- Invoice lines table
CREATE TABLE invoice_lines (
    id BIGINT PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    line_number INT NOT NULL,
    
    -- Common fields
    line_date DATE NOT NULL,
    description TEXT NOT NULL,
    order_reference TEXT,
    vat_code TEXT NOT NULL,
    vat_percentage NUMERIC(5,2) NOT NULL,  -- Snapshot of VAT rate at invoice creation
    gl_account_code TEXT,                   -- Ledger account for bookkeeping
    quantity NUMERIC(15,4) NOT NULL,
    unit_price NUMERIC(15,4) NOT NULL,
    total_excl_vat NUMERIC(15,2) NOT NULL,
    unit_of_measure TEXT NOT NULL,
    
    -- Catalog item snapshot (denormalized)
    catalog_item_id BIGINT REFERENCES catalog_items(id),
    catalog_item_code TEXT NOT NULL,
    catalog_item_name TEXT NOT NULL,
    catalog_item_type TEXT NOT NULL,  -- MATERIAL, PRODUCT, SERVICE
    
    UNIQUE (invoice_id, line_number)
);

-- Indexes for invoice queries
CREATE INDEX idx_invoices_customer ON invoices(customer_company_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_date ON invoices(invoice_date);
```

### 3. Company Schema Enhancement

Add VAT number to companies table:

```sql
ALTER TABLE companies ADD COLUMN vat_number TEXT;
```

### 4. Invoice Number Generation Strategy

**Important**: Invoice numbers are only assigned when an invoice is **finalized**, not at creation. This ensures no gaps in the sequence for draft invoices that are never completed.

**Single sequence per year**: Both regular invoices and credit notes share the same sequence to ensure continuous numbering for Dutch tax compliance.

| Prefix | Source | Example |
|--------|--------|--------|
| `ER` | Eazy Recycling (invoices and credit notes) | ER-2025-00001 |
| `LEG` | Legacy system imports | LEG-2025-00500 |

**Implementation**:

```kotlin
@Service
class InvoiceNumberGenerator(
    private val invoiceNumberSequenceRepository: InvoiceNumberSequenceRepository
) {
    @Transactional
    fun nextNumber(prefix: String = "ER"): InvoiceNumber {
        val year = LocalDate.now().year
        val sequence = invoiceNumberSequenceRepository.incrementAndGet(year)
        return InvoiceNumber.generate(prefix, year, sequence)
    }
}
```

### 5. Use Cases

#### Create Invoice from Scratch

```kotlin
interface CreateInvoice {
    fun handle(cmd: CreateInvoiceCommand): InvoiceResult
}

data class CreateInvoiceCommand(
    val invoiceType: InvoiceType,
    val customerId: CompanyId,
    val invoiceDate: LocalDate,
    val lines: List<InvoiceLineCommand>,
)

data class InvoiceLineCommand(
    val date: LocalDate,
    val catalogItemId: Long,
    val description: String?,  // Optional override of catalog item name
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,  // Can be overridden from catalog item default
    val orderReference: String?,
)
```

#### Create Invoice from Weight Ticket

```kotlin
interface CreateInvoiceFromWeightTicket {
    fun handle(cmd: CreateInvoiceFromWeightTicketCommand): InvoiceResult
}

data class CreateInvoiceFromWeightTicketCommand(
    val weightTicketId: WeightTicketId,
    val invoiceDate: LocalDate,
    val additionalLines: List<InvoiceLineCommand> = emptyList(),  // e.g., transport hours
)
```

#### Finalize Invoice

```kotlin
interface FinalizeInvoice {
    fun handle(cmd: FinalizeInvoiceCommand): InvoiceResult
}

data class FinalizeInvoiceCommand(
    val invoiceId: InvoiceId,
)
```

When finalized:
1. **Invoice number is assigned** (from sequence)
2. Status changes to FINAL
3. Invoice PDF is generated
4. Email is sent to bookkeeping and customer
5. Linked weight ticket(s) status changes to INVOICED

### 6. Frontend Structure

New feature module under `apps/react-frontend/src/features/invoices/`:

```
invoices/
├── components/
│   ├── InvoiceForm.tsx
│   ├── InvoiceLineForm.tsx
│   ├── InvoiceList.tsx
│   ├── InvoiceDetails.tsx
│   └── InvoiceTotals.tsx
├── hooks/
│   └── useInvoiceForm.ts
├── services/
│   └── invoiceService.ts
└── types/
    └── invoice.types.ts
```

Sidebar menu addition: **"Financieel"** section with "Facturen" (Invoices) submenu.

### 7. Future Extension: Cash Receipts (Kasbon)

The design supports future cash receipt functionality by:

1. Adding `PaymentMethod` enum: `INVOICE`, `CASH`
2. Adding `paymentMethod` field to Invoice entity
3. Cash receipts use same structure but with immediate payment recording
4. Different PDF template for cash receipts

```kotlin
enum class PaymentMethod {
    INVOICE,    // Standard invoice, payment later
    CASH        // Kasbon, paid immediately
}
```

---

## Pros and Cons of the Options

### Option 1: Invoice as Aggregate Root with Catalog Item Lines (Chosen)

**Pros**:
- Single aggregate for all invoice types
- Unified catalog item reference simplifies line handling
- Clean extension path for cash receipts
- Matches existing DDD patterns in codebase

**Cons**:
- Requires catalog items to be set up before invoicing

### Option 2: Separate Invoice Types

**Pros**:
- Clear separation between purchase and sales

**Cons**:
- Code duplication
- Harder to query across invoice types
- More complex when mixing line types

### Option 3: Single Entity with Discriminator

**Pros**:
- Simpler persistence

**Cons**:
- Less type safety
- Nullable fields for type-specific data

---

## Implementation Phases

### Phase 1: Core Invoice Domain
1. Add `vat_number` to companies table
2. Create invoice and invoice_lines tables
3. Create invoice number sequence table
4. Implement Invoice aggregate and value objects
5. Implement CreateInvoice use case

### Phase 2: Weight Ticket Integration
1. Implement CreateInvoiceFromWeightTicket use case (supports multiple weight tickets)
2. Update WeightTicket status to INVOICED on finalization
3. Add bidirectional navigation in UI

### Phase 2b: Credit Notes
1. Implement CreateCreditNote use case
2. Link credit note to original invoice
3. Credit note specific PDF template

### Phase 3: Finalization & Notifications
1. Implement FinalizeInvoice use case
2. PDF generation (Supabase Edge Function)
3. Email notifications

### Phase 4: Frontend
1. Create invoice feature module
2. Add "Financieel" menu section
3. Invoice list, form, and details views
4. Integration with weight ticket and transport views

### Phase 5: Future - Cash Receipts
1. Add PaymentMethod to Invoice
2. Cash receipt specific workflows
3. Different PDF template

---

## More Information

### Related ADRs
- ADR-0012: Exact Online Synchronization Strategy (for future invoice sync)
- ADR-0010: Denormalize Transport/Wastestream Data (pattern for customer snapshot)

### Dutch Tax Authority Requirements
- Invoice numbers must be sequential per year
- No gaps allowed in sequence (use prefix to separate systems)
- Invoice must contain: date, number, seller/buyer details, VAT breakdown

### Open Questions
1. What email template/service to use for invoice notifications?
2. Should invoice PDF generation happen synchronously or async?
