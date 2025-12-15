# ADR-0021: Catalog Items - Unified Product and Material Strategy

## Status

Proposed

## Context and Problem Statement

The system needs to support billing for various types of sellable items on weight tickets and invoices:

1. **Materials** - Physical goods with weight-based pricing (kg, ton)
2. **Services** - Non-material items like transport hours, container rental, administrative fees

Additionally, business customers have **waste streams** which are regulatory registrations linking materials to compliance data (eural codes, processing methods). These are critical for monthly LMA reporting.

### Key Constraints

1. **Business customers** have waste streams with regulatory requirements (LMA reporting)
2. **Private customers** (future) won't have waste streams - they select materials directly
3. **Invoice lines** should show waste stream numbers for customer reference
4. **Multiple waste streams** can exist for the same material per consignor
5. **Services** don't have waste streams

### The Core Insight

**Waste streams are not catalog items** - they're regulatory/operational entities that *reference* a catalog item (material) for pricing purposes. The financial context (catalog items for invoicing) is separate from the operational context (waste streams for regulatory compliance).

### Why Not Store Waste Streams as Catalog Items?

An alternative approach would be to auto-create a catalog item for each waste stream, eliminating the need for a separate `waste_stream_number` column on weight ticket lines. However, this mixes bounded contexts and creates sync complexity.

The `waste_stream_number` column on weight ticket lines is actually a feature, not a bug - it explicitly captures the regulatory reference needed for LMA reporting. The current design is cleaner because:

- **Catalog items** = what you sell (financial context)
- **Waste streams** = regulatory registration (compliance context)
- **Weight ticket line** = links both when needed, keeping concerns explicit

---

## Decision Outcome

**Catalog Item = the sellable entity (always required)**
**Waste Stream = regulatory metadata (optional, for LMA reporting & customer reference)**

This approach provides:

- **Single source of pricing truth**: All pricing comes from catalog items
- **Clean separation of concerns**: Regulatory data stays with waste streams, financial data with catalog items
- **Support for both customer types**: Business customers use waste streams, private customers use materials directly
- **Minimal normalization**: Only the repository layer needs to join waste_streams → catalog_items

---

## Detailed Design

### 1. Data Model Overview

```text
┌─────────────────────────────────────────────────────────────────────┐
│              CATALOG_ITEM_CATEGORIES (Grouping)                      │
├─────────────────────────────────────────────────────────────────────┤
│  id (PK)                                                            │
│  type: MATERIAL | SERVICE  ← determines which UI tab shows this     │
│  code                                                               │
│  name                                                               │
│  description                                                        │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                   CATALOG_ITEMS (Financial Master Data)              │
├─────────────────────────────────────────────────────────────────────┤
│  id (PK)                                                            │
│  type: MATERIAL | SERVICE                                           │
│  code                                                               │
│  name                                                               │
│  unit_of_measure                                                    │
│  vat_code                                                           │
│  gl_account_code                                                    │
│  category_id (FK → catalog_item_categories)                         │
│  consignor_party_id (nullable)  ← NULL = generic, set = specific    │
│  default_price                                                      │
│  status                                                             │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                  WASTE_STREAMS (Regulatory Registration)             │
├─────────────────────────────────────────────────────────────────────┤
│  number (PK)  ← 12-digit regulatory ID                              │
│  name                                                               │
│  consignor_party_id                                                 │
│  catalog_item_id (FK → catalog_items)  ← links to material          │
│  eural_code                                                         │
│  processing_method_code                                             │
│  pickup_location_id                                                 │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      WEIGHT_TICKET_LINES                             │
├─────────────────────────────────────────────────────────────────────┤
│  catalog_item_id (FK → catalog_items)  ← ALWAYS required            │
│  waste_stream_number (FK → waste_streams)  ← OPTIONAL (for LMA)     │
│  weight_value                                                       │
│  weight_unit                                                        │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         INVOICE_LINES                                │
├─────────────────────────────────────────────────────────────────────┤
│  catalog_item_id (FK)  ← ALWAYS required (for pricing)              │
│  waste_stream_number   ← OPTIONAL (snapshot for customer reference) │
│  quantity                                                           │
│  unit_price                                                         │
│  ... snapshot fields                                                │
└─────────────────────────────────────────────────────────────────────┘
```

### 2. Domain Model

#### Catalog Item

```kotlin
data class CatalogItemCategory(
    val id: CatalogItemCategoryId,
    val type: CatalogItemType,  // Determines which UI tab shows this category
    val code: String,
    val name: String,
    val description: String?,
)

@JvmInline
value class CatalogItemCategoryId(val value: Long)

data class CatalogItem(
    val id: CatalogItemId,
    val type: CatalogItemType,
    val code: String,
    val name: String,
    val unitOfMeasure: UnitOfMeasure,
    val vatCode: String,
    val glAccountCode: String?,
    val categoryId: CatalogItemCategoryId?,
    val consignorPartyId: CompanyId?,  // NULL = generic, available to all
    val defaultPrice: BigDecimal?,
    val status: CatalogItemStatus,
)

@JvmInline
value class CatalogItemId(val value: Long)

enum class CatalogItemType {
    MATERIAL,  // Physical goods (weight-based)
    SERVICE    // Non-physical items (time/quantity-based)
}

enum class UnitOfMeasure {
    KG, TON,           // Weight-based (materials)
    HOUR, DAY, WEEK,   // Time-based (services)
    PIECE, FIXED       // Quantity-based (services)
}

enum class CatalogItemStatus {
    ACTIVE, INACTIVE
}
```

#### Waste Stream (Updated)

```kotlin
data class WasteStream(
    val number: WasteStreamNumber,
    val name: String,
    val consignorPartyId: CompanyId,
    val catalogItemId: CatalogItemId,  // Links to material for pricing
    val euralCode: String,
    val processingMethodCode: String,
    val pickupLocationId: Long?,
    val processorPartyId: CompanyId?,
)
```

#### Weight Ticket Line

```kotlin
data class WeightTicketLine(
    val catalogItemId: CatalogItemId,           // Always required
    val wasteStreamNumber: WasteStreamNumber?,  // Optional (for LMA reporting)
    val weight: Weight,
)
```

#### Invoice Line

```kotlin
data class InvoiceLine(
    val id: InvoiceLineId,
    val lineNumber: Int,
    val date: LocalDate,
    
    // Catalog item reference (for pricing)
    val catalogItemId: CatalogItemId,
    
    // Waste stream reference (for customer reference, optional)
    val wasteStreamNumber: WasteStreamNumber?,
    
    // Snapshot fields (denormalized at invoice creation)
    val catalogItemCode: String,
    val catalogItemName: String,
    val description: String?,
    val vatCode: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val unitOfMeasure: String,
    val totalExclVat: BigDecimal,
)
```

### 3. Database Schema

#### Catalog Item Categories Table (Replaces material_groups and product_categories)

```sql
CREATE TABLE catalog_item_categories (
    id BIGSERIAL PRIMARY KEY,
    type TEXT NOT NULL,  -- 'MATERIAL' or 'SERVICE' (determines UI tab)
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_by TEXT
);

CREATE INDEX idx_catalog_item_categories_type ON catalog_item_categories(type);
```

#### Catalog Items Table (Replaces materials and products tables)

```sql
CREATE TABLE catalog_items (
    id BIGSERIAL PRIMARY KEY,
    type TEXT NOT NULL,  -- 'MATERIAL' or 'SERVICE'
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    unit_of_measure TEXT NOT NULL,
    vat_code TEXT NOT NULL,
    gl_account_code TEXT,
    category_id BIGINT REFERENCES catalog_item_categories(id),
    consignor_party_id UUID REFERENCES companies(party_id),  -- NULL = generic
    default_price NUMERIC(15, 4),
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_by TEXT,
);

CREATE INDEX idx_catalog_items_type ON catalog_items(type);
CREATE INDEX idx_catalog_items_status ON catalog_items(status);
CREATE INDEX idx_catalog_items_consignor ON catalog_items(consignor_party_id);
CREATE INDEX idx_catalog_items_category ON catalog_items(category_id);
```


#### Waste Streams Table (Updated)

```sql
ALTER TABLE waste_streams 
ADD COLUMN catalog_item_id BIGINT REFERENCES catalog_items(id);

COMMENT ON COLUMN waste_streams.catalog_item_id IS 
  'Links this waste stream to a catalog item (material) for pricing. 
   The waste stream inherits VAT code, GL account, and pricing from the catalog item.';
```

#### Weight Ticket Lines Table (Updated)

```sql
CREATE TABLE weight_ticket_lines (
    weight_ticket_id BIGINT NOT NULL REFERENCES weight_tickets(id),
    catalog_item_id BIGINT NOT NULL REFERENCES catalog_items(id),
    waste_stream_number TEXT REFERENCES waste_streams(number),
    weight_value NUMERIC(10, 2) NOT NULL,
    weight_unit TEXT NOT NULL
);
```

### 4. Usage Scenarios

#### Scenario 1: Business Customer with Waste Stream

```text
1. User selects consignor (ACME BV)
2. Dropdown shows waste streams for ACME BV:
   - "180101-001 - Diverse Metalen" (Afvalstroom)
   - "180102-001 - Koper" (Afvalstroom)
   Plus generic services:
   - "Transport uren" (Dienst)

3. User selects waste stream "180101-001"
   → System looks up: waste_streams.catalog_item_id → catalog_items.id = 42

4. Weight Ticket Line saved:
   - catalog_item_id: 42
   - waste_stream_number: "180101-001"  ← For LMA reporting

5. Invoice Line created:
   - catalog_item_id: 42 (pricing from catalog item)
   - waste_stream_number: "180101-001"  ← Customer reference on invoice
```

#### Scenario 2: Private Customer (No Waste Stream)

```text
1. User selects private customer (no consignor)
2. Dropdown shows generic catalog items:
   - "Diverse Metalen" (Materiaal)
   - "Koper" (Materiaal)
   - "Transport uren" (Dienst)

3. User selects "Diverse Metalen"

4. Weight Ticket Line saved:
   - catalog_item_id: 42
   - waste_stream_number: NULL  ← No LMA reporting needed

5. Invoice Line created:
   - catalog_item_id: 42
   - waste_stream_number: NULL
```

#### Scenario 3: Service (Never Has Waste Stream)

```text
1. User selects "Transport uren" (service)

2. Weight Ticket Line saved:
   - catalog_item_id: 99 (Transport uren)
   - waste_stream_number: NULL  ← Services don't have waste streams

3. Invoice Line created:
   - catalog_item_id: 99
   - waste_stream_number: NULL
```

### 5. UI Flow for Weight Ticket Lines

```text
┌──────────────────────────────────────────────────────────────┐
│  Select item                                          ▼      │
├──────────────────────────────────────────────────────────────┤
│  ── Afvalstromen (ACME BV) ──                                │
│  180101-001 - Diverse Metalen                                │
│  180102-001 - Koper                                          │
│  ── Materialen ──                                            │
│  Diverse Metalen                                             │
│  Koper                                                       │
│  ── Diensten ──                                              │
│  Transport uren                                              │
│  Container huur                                              │
└──────────────────────────────────────────────────────────────┘

For business customers: Show waste streams (grouped) + generic items
For private customers: Show only generic materials + services
```

### 6. API Endpoints

#### Get Catalog Items for Weight Ticket

```kotlin
@GetMapping("/api/catalog/items/for-weight-ticket")
fun getCatalogItemsForWeightTicket(
    @RequestParam consignorPartyId: UUID?,
    @RequestParam query: String?,
    @RequestParam limit: Int = 50
): List<CatalogItemForWeightTicketResponse>

data class CatalogItemForWeightTicketResponse(
    val catalogItemId: Long,
    val catalogItemType: CatalogItemType,
    val catalogItemName: String,
    val wasteStreamNumber: String?,  // NULL for generic items and services
    val displayName: String,         // For dropdown label
    val unitOfMeasure: String,
)
```

**Response structure:**

- For waste streams: `wasteStreamNumber` is set, `displayName` = waste stream name
- For generic materials: `wasteStreamNumber` is NULL, `displayName` = material name
- For services: `wasteStreamNumber` is NULL, `displayName` = service name

### 7. Migration Strategy

#### Phase 1: Create New Tables

1. Create `catalog_item_categories` table
2. Migrate existing `material_groups` to `catalog_item_categories` with type = 'MATERIAL'
3. Migrate existing `product_categories` to `catalog_item_categories` with type = 'SERVICE'
4. Create `catalog_items` table
5. Migrate existing `materials` to `catalog_items` with type = 'MATERIAL'
6. Migrate existing `products` to `catalog_items` with type = 'SERVICE'

#### Phase 2: Link Waste Streams

1. Add `catalog_item_id` column to `waste_streams`
2. Data migration: Link waste streams to appropriate catalog items
3. Add material select field to WasteStreamForm in frontend

#### Phase 3: Update Weight Ticket Lines

1. Add `catalog_item_id` column to `weight_ticket_lines`
2. Data migration: Populate catalog_item_id from waste_stream → catalog_item link
3. Update frontend to use new API endpoint

#### Phase 4: Support Generic Materials (Private Customers)

1. Make `waste_stream_number` nullable in weight_ticket_lines
2. Update frontend to support both flows

#### Phase 5: Remove Legacy Tables

1. Drop `materials` table (after verifying all data migrated)
2. Drop `products` table
3. Drop `material_groups` table
4. Drop `product_categories` table

---

## Benefits

| Concern | How it's addressed |
|---------|-------------------|
| Private customers | Select catalog item directly, waste_stream_number = NULL |
| Business customers | Select waste stream → auto-resolves catalog item |
| LMA reporting | waste_stream_number on weight ticket line |
| Invoice reference | waste_stream_number snapshotted on invoice line |
| Multiple waste streams per material | Each waste stream links to same catalog_item_id |
| Services | catalog_item with type = SERVICE, no waste stream |
| Pricing | Always from catalog_item (single source) |
| Normalization | Only repository joins waste_stream → catalog_item |
| Separate UI tabs | Filter catalog_item_categories by type for Materials vs Services tabs |

---

## Domain Model Strategy: Shared Kernel Pattern

The `catalog_items` table serves as **shared infrastructure** between two bounded contexts, each with its own domain model:

### Context 1: Pricing (Material Domain)

```kotlin
// Used for: Materials UI tab, third-party pricing sync
data class Material(
    val id: Long?,
    val code: String,
    val name: String,
    val materialGroupId: Long?,
    val unitOfMeasure: String,
    val vatCode: String,
    val purchaseAccountNumber: String?,
    val salesAccountNumber: String?,
    val status: String,
)
```

- **Ownership**: Materials UI tab owns CRUD for `type = 'MATERIAL'` records
- **Use case**: Maintain pricing data for third-party integrations
- **Repository**: `MaterialRepository` reads/writes `catalog_items WHERE type = 'MATERIAL'`

### Context 2: Invoicing (CatalogItem Domain)

```kotlin
// Used for: Weight tickets, invoices
data class CatalogItem(
    val id: CatalogItemId,
    val type: CatalogItemType,  // MATERIAL or SERVICE
    val code: String,
    val name: String,
    val unitOfMeasure: UnitOfMeasure,
    val vatCode: String,
    // ... invoicing-specific fields
)
```

- **Ownership**: Read-only access to all catalog items for operational use
- **Use case**: Select items for weight ticket lines, generate invoice lines
- **Repository**: `CatalogItemRepository` reads all `catalog_items`

### Why This Is Valid (DDD Perspective)

1. **Different Invariants**: Materials have pricing rules; CatalogItems have invoicing rules
2. **Different Lifecycles**: Materials maintained by pricing team; CatalogItems used operationally
3. **Clear Write Ownership**: Only one context writes to each subset of records
4. **Shared Identity**: Both models share the same ID, enabling cross-context references

```text
┌─────────────────────────────┐     ┌─────────────────────────────┐
│   Pricing Context           │     │   Invoicing Context         │
│   ─────────────────         │     │   ─────────────────         │
│   Material (domain)         │     │   CatalogItem (domain)      │
│   MaterialRepository        │     │   CatalogItemRepository     │
│   (read/write MATERIAL)     │     │   (read-only, all types)    │
└─────────────────────────────┘     └─────────────────────────────┘
              │                                   │
              └───────────┬───────────────────────┘
                          ▼
                 ┌─────────────────┐
                 │  catalog_items  │
                 │  (database)     │
                 └─────────────────┘
```

This is the **Shared Kernel** pattern - the database table is shared infrastructure, but each context maintains its own domain model with its own rules and behaviors.

---

## Related ADRs

- ADR-0020: Invoice Domain Model (uses catalog items for invoice lines)
