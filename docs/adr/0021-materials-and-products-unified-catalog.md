# ADR-0021: Materials and Products - Unified Catalog Strategy

## Status

Proposed

## Context and Problem Statement

The system currently has a `materials` table (289 entries) representing recyclable materials like metals, cables, and demolition waste. With the introduction of invoicing (ADR-0020), we need to support billing for **non-material products** such as:

- Transport hours
- Container rental
- Administrative fees
- Processing fees
- Equipment rental

### The Challenge

When users create **weight ticket lines** or **invoice lines**, they need to select from a combined list of:

1. **Materials**: Physical goods with weight-based pricing (kg, ton)
2. **Products**: Services or fees with time/quantity-based pricing (hours, pieces, fixed)

The UI must present these options in a unified, searchable dropdown while maintaining:

- Clean domain separation
- Query performance
- Type safety
- Maintainability

### Current State

**Materials** (`materials` table):

| Field | Type | Description |
|-------|------|-------------|
| `id` | bigint | Primary key |
| `code` | text | Unique identifier (e.g., "13", "1000") |
| `name` | text | Display name (e.g., "Diverse metalen") |
| `material_group_id` | bigint | FK to material_groups |
| `unit_of_measure` | text | Always "kg" currently |
| `vat_code` | text | VAT rate code (e.g., "HOOG") |
| `gl_account_code` | text | Ledger account for bookkeeping |
| `status` | text | ACTIVE/INACTIVE |

**Material Groups** (`material_groups` table):

| Field | Type | Description |
|-------|------|-------------|
| `id` | bigint | Primary key |
| `code` | text | Group code |
| `name` | text | Group name (e.g., "Ijzer", "Kabel", "Sloop") |

**Waste Streams** (`waste_streams` table):

| Field | Type | Description |
|-------|------|-------------|
| `number` | text | Primary key (12-digit waste stream number) |
| `name` | text | Display name for this waste stream |
| `consignor_party_id` | bigint | FK to companies - the consignor this waste stream belongs to |
| `eural_code` | text | FK to eural codes |
| `processing_method_code` | text | FK to processing methods |
| `pickup_location_id` | bigint | FK to pickup locations |
| `processor_party_id` | bigint | FK to companies - the processor |

**Key Distinction**:

- **Materials** are generic catalog items available to ALL customers (e.g., "Diverse metalen", "Koper")
- **Waste Streams** are consignor-specific registrations that link a material to regulatory requirements (eural code, processing method, pickup location, etc.)

**Weight Ticket Lines** currently reference `WasteStreamNumber`, not materials directly.

---

## Considered Options

### Option 1: Separate Products Table with API-Level Union

Create a new `products` table for non-material items. Combine results at the API level.

```
materials (existing)          products (new)
├── id                        ├── id
├── code                      ├── code
├── name                      ├── name
├── material_group_id         ├── product_category_id
├── unit_of_measure           ├── unit_of_measure
├── vat_code                  ├── vat_code
└── status                    └── status
```

**API Response**: Unified `CatalogItem` DTO combining both sources.

### Option 2: Single Table Inheritance (STI)

Merge materials and products into a single `catalog_items` table with a `type` discriminator.

```
catalog_items
├── id
├── type (MATERIAL | PRODUCT)
├── code
├── name
├── category_id
├── unit_of_measure
├── vat_code
├── status
├── material_group_id (nullable, only for MATERIAL)
└── ... material-specific fields
```

### Option 3: Database View with Union

Keep separate tables but create a database view that unions them for querying.

```sql
CREATE VIEW catalog_items_view AS
SELECT 
    'MATERIAL' as item_type,
    id,
    code,
    name,
    unit_of_measure,
    vat_code,
    status,
    material_group_id as category_id
FROM materials
UNION ALL
SELECT 
    'PRODUCT' as item_type,
    id,
    code,
    name,
    unit_of_measure,
    vat_code,
    status,
    product_category_id as category_id
FROM products;
```

### Option 4: Polymorphic Association with Abstract Base

Create an abstract `CatalogItem` interface in the domain with concrete `Material` and `Product` implementations. Use a discriminated union pattern in persistence.

---

## Decision Outcome

**Chosen option: Option 1 - Separate Products Table with API-Level Union**

This approach provides:

- **Clean domain separation**: Materials and products remain distinct entities with their own business rules
- **No migration risk**: Existing materials table unchanged
- **Flexibility**: Products can have different attributes than materials
- **Performance**: Simple queries on individual tables; union only when needed
- **Type safety**: Strong typing in domain model with sealed classes

---

## Detailed Design

### 1. Domain Model

#### Product Entity (New)

```kotlin
data class Product(
    val id: ProductId,
    val code: String,
    val name: String,
    val categoryId: ProductCategoryId?,
    val unitOfMeasure: UnitOfMeasure,
    val vatCode: String,
    val glAccountCode: String?,  // Ledger account for bookkeeping integration
    val status: ProductStatus,
    val defaultPrice: BigDecimal?,
    val description: String?,
    val createdAt: Instant? = null,
    val createdBy: String? = null,
    val updatedAt: Instant? = null,
    val updatedBy: String? = null,
)

data class ProductId(val value: Long)

data class ProductCategoryId(val value: Long)

enum class ProductStatus {
    ACTIVE,
    INACTIVE
}

enum class UnitOfMeasure {
    KG,         // Kilogram (materials)
    TON,        // Metric ton
    HOUR,       // Transport hours
    PIECE,      // Individual items
    FIXED,      // Fixed fee (quantity = 1)
    DAY,        // Daily rental
    WEEK,       // Weekly rental
    MONTH       // Monthly rental
}
```

#### Product Category Entity (New)

```kotlin
data class ProductCategory(
    val id: ProductCategoryId,
    val code: String,
    val name: String,
    val description: String?,
)
```

#### Catalog Item (Unified View)

The `CatalogItem` sealed class provides a unified view for UI dropdowns. Each item retains its own numeric ID and includes an `itemType` discriminator to identify the source table.

```kotlin
sealed class CatalogItem {
    abstract val id: Long             // Native ID from source table
    abstract val code: String
    abstract val name: String
    abstract val unitOfMeasure: String
    abstract val vatCode: String
    abstract val glAccountCode: String?  // Ledger account for bookkeeping
    abstract val categoryName: String?
    abstract val itemType: CatalogItemType
    
    data class MaterialItem(
        override val id: Long,
        override val code: String,
        override val name: String,
        override val unitOfMeasure: String,
        override val vatCode: String,
        override val glAccountCode: String?,
        override val categoryName: String?,
        val materialGroupId: Long?,
    ) : CatalogItem() {
        override val itemType = CatalogItemType.MATERIAL
    }
    
    data class ProductItem(
        override val id: Long,
        override val code: String,
        override val name: String,
        override val unitOfMeasure: String,
        override val vatCode: String,
        override val glAccountCode: String?,
        override val categoryName: String?,
        val productCategoryId: Long?,
        val defaultPrice: BigDecimal?,
    ) : CatalogItem() {
        override val itemType = CatalogItemType.PRODUCT
    }
}

enum class CatalogItemType {
    MATERIAL,
    PRODUCT
}
```

### 2. Database Schema

#### Products Table

```sql
CREATE TABLE product_categories (
    id BIGSERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_by TEXT
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    product_category_id BIGINT REFERENCES product_categories(id),
    unit_of_measure TEXT NOT NULL,
    vat_code TEXT NOT NULL,
    gl_account_code TEXT,              -- Ledger account for bookkeeping
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    default_price NUMERIC(15,4),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by TEXT,
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_by TEXT
);

CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_category ON products(product_category_id);
```

#### Seed Data for Common Products

```sql
INSERT INTO product_categories (code, name, description) VALUES
    ('TRANSPORT', 'Transport', 'Transport-gerelateerde diensten'),
    ('RENTAL', 'Verhuur', 'Container en materieel verhuur'),
    ('ADMIN', 'Administratie', 'Administratieve kosten'),
    ('PROCESSING', 'Verwerking', 'Verwerkingskosten');

INSERT INTO products (code, name, product_category_id, unit_of_measure, vat_code, default_price) VALUES
    ('TRANS-HOUR', 'Transporturen', 1, 'HOUR', 'HOOG', 85.00),
    ('TRANS-KM', 'Transportkilometers', 1, 'PIECE', 'HOOG', 1.50),
    ('CONT-RENT-DAY', 'Containerhuur per dag', 2, 'DAY', 'HOOG', 15.00),
    ('CONT-RENT-WEEK', 'Containerhuur per week', 2, 'WEEK', 'HOOG', 75.00),
    ('ADMIN-FEE', 'Administratiekosten', 3, 'FIXED', 'HOOG', 25.00),
    ('WEIGH-FEE', 'Weegkosten', 3, 'FIXED', 'HOOG', 10.00);
```

### 3. Query Service for Unified Catalog

```kotlin
interface CatalogQueryService {
    fun searchCatalogItems(
        query: String?,
        itemTypes: Set<CatalogItemType>? = null,
        categoryIds: Set<Long>? = null,
        limit: Int = 50
    ): List<CatalogItem>
    
    fun getMaterialById(id: Long): CatalogItem.MaterialItem?
    fun getProductById(id: Long): CatalogItem.ProductItem?
    fun getCatalogItemByTypeAndId(itemType: CatalogItemType, id: Long): CatalogItem?
}

@Service
class CatalogQueryServiceImpl(
    private val materialRepository: MaterialRepository,
    private val productRepository: ProductRepository,
    private val materialGroupRepository: MaterialGroupRepository,
    private val productCategoryRepository: ProductCategoryRepository,
) : CatalogQueryService {
    
    override fun searchCatalogItems(
        query: String?,
        itemTypes: Set<CatalogItemType>?,
        categoryIds: Set<String>?,
        limit: Int
    ): List<CatalogItem> {
        val results = mutableListOf<CatalogItem>()
        
        // Include materials if not filtered out
        if (itemTypes == null || itemTypes.contains(CatalogItemType.MATERIAL)) {
            val materials = materialRepository.search(query, limit)
            results.addAll(materials.map { it.toCatalogItem() })
        }
        
        // Include products if not filtered out
        if (itemTypes == null || itemTypes.contains(CatalogItemType.PRODUCT)) {
            val products = productRepository.search(query, limit)
            results.addAll(products.map { it.toCatalogItem() })
        }
        
        // Sort combined results by relevance/name
        return results
            .sortedBy { it.name }
            .take(limit)
    }
    
    fun getMaterialById(id: Long): CatalogItem.MaterialItem? {
        return materialRepository.findById(id)?.toCatalogItem()
    }
    
    fun getProductById(id: Long): CatalogItem.ProductItem? {
        return productRepository.findById(id)?.toCatalogItem()
    }
    
    fun getCatalogItemByTypeAndId(itemType: CatalogItemType, id: Long): CatalogItem? {
        return when (itemType) {
            CatalogItemType.MATERIAL -> getMaterialById(id)
            CatalogItemType.PRODUCT -> getProductById(id)
        }
    }
}
```

### 4. REST API

```kotlin
@RestController
@RequestMapping("/api/catalog")
class CatalogController(
    private val catalogQueryService: CatalogQueryService
) {
    
    @GetMapping("/items")
    fun searchItems(
        @RequestParam query: String?,
        @RequestParam types: Set<CatalogItemType>?,
        @RequestParam limit: Int = 50
    ): List<CatalogItemView> {
        return catalogQueryService.searchCatalogItems(query, types, limit)
            .map { it.toView() }
    }
    
    @GetMapping("/materials/{id}")
    fun getMaterial(@PathVariable id: Long): CatalogItemView? {
        return catalogQueryService.getMaterialById(id)?.toView()
    }
    
    @GetMapping("/products/{id}")
    fun getProduct(@PathVariable id: Long): CatalogItemView? {
        return catalogQueryService.getProductById(id)?.toView()
    }
}

data class CatalogItemView(
    val id: Long,
    val code: String,
    val name: String,
    val displayName: String,  // "code - name"
    val unitOfMeasure: String,
    val vatCode: String,
    val categoryName: String?,
    val itemType: CatalogItemType,  // MATERIAL or PRODUCT
    val defaultPrice: BigDecimal?,
)
```

### 5. Frontend Integration

#### Async Select Component Usage

```typescript
// CatalogItemSelect.tsx
interface CatalogItemSelectProps {
  value: CatalogItemView | null;
  onChange: (item: CatalogItemView | null) => void;
  itemTypes?: ('MATERIAL' | 'PRODUCT')[];
  placeholder?: string;
}

const CatalogItemSelect: React.FC<CatalogItemSelectProps> = ({
  value,
  onChange,
  itemTypes,
  placeholder = "Zoek materiaal of product..."
}) => {
  const loadOptions = async (inputValue: string) => {
    const params = new URLSearchParams();
    if (inputValue) params.set('query', inputValue);
    if (itemTypes) itemTypes.forEach(t => params.append('types', t));
    
    const response = await catalogApi.searchItems(params);
    return response.map(item => ({
      value: item.id,
      label: item.displayName,
      data: item
    }));
  };
  
  return (
    <AsyncSelect
      loadOptions={loadOptions}
      value={value ? { value: value.id, label: value.displayName, data: value } : null}
      onChange={(option) => onChange(option?.data ?? null)}
      placeholder={placeholder}
      isClearable
    />
  );
};
```

### 6. Waste Stream to Material Linking

#### The Problem

When creating weight ticket lines, users need to select from a list of items. Currently, they select from **waste streams** which are consignor-specific. However:

- **Materials** are generic catalog items (e.g., "Diverse metalen") available to all customers
- **Waste Streams** are consignor-specific registrations with regulatory data (eural code, processing method, etc.)
- **Products** are non-material items (transport hours, fees) available to all customers

For weight ticket lines, we need to show:

1. **Waste streams** belonging to the selected consignor (for materials with regulatory requirements)
2. **Products** available to all customers (for non-material items like transport fees)

#### Solution: Link Waste Streams to Materials

Add a foreign key from `waste_streams` to `materials`:

```sql
ALTER TABLE waste_streams 
ADD COLUMN material_id BIGINT REFERENCES materials(id);

COMMENT ON COLUMN waste_streams.material_id IS 
  'Links this waste stream to a generic material. The waste stream name overrides the material name for display purposes.';
```

This creates a relationship where:

- A **Material** is a generic catalog item (e.g., "Diverse metalen")
- A **Waste Stream** is a consignor-specific instance that references a material and adds regulatory data
- The **waste stream name** is used for display (not the material name)

#### Updated Domain Model

```kotlin
// Waste Stream now references a Material
data class WasteStream(
    val wasteStreamNumber: String,
    val name: String,                    // Display name (overrides material name)
    val materialId: Long?,               // FK to materials table
    val consignorPartyId: Long,
    val euralCode: String,
    val processingMethodCode: String,
    // ... other fields
)
```

#### Catalog Item Types for Weight Ticket Lines

For the weight ticket lines dropdown, we need three types of catalog items:

```kotlin
sealed class CatalogItem {
    abstract val id: Long
    abstract val code: String
    abstract val name: String
    abstract val unitOfMeasure: String
    abstract val vatCode: String
    abstract val glAccountCode: String?
    abstract val categoryName: String?
    abstract val itemType: CatalogItemType
    
    // Generic material (available to all customers)
    data class MaterialItem(
        override val id: Long,
        override val code: String,
        override val name: String,
        override val unitOfMeasure: String,
        override val vatCode: String,
        override val glAccountCode: String?,
        override val categoryName: String?,
        val materialGroupId: Long?,
    ) : CatalogItem() {
        override val itemType = CatalogItemType.MATERIAL
    }
    
    // Waste stream (consignor-specific, linked to a material)
    data class WasteStreamItem(
        override val id: Long,              // Use material_id for consistency
        override val code: String,          // waste_stream_number
        override val name: String,          // waste stream name (display name)
        override val unitOfMeasure: String, // From linked material
        override val vatCode: String,       // From linked material
        override val glAccountCode: String?, // From linked material
        override val categoryName: String?, // From linked material group
        val wasteStreamNumber: String,
        val materialId: Long,
        val consignorPartyId: Long,
        val euralCode: String,
        val processingMethodCode: String,
    ) : CatalogItem() {
        override val itemType = CatalogItemType.WASTE_STREAM
    }
    
    // Product (available to all customers)
    data class ProductItem(
        override val id: Long,
        override val code: String,
        override val name: String,
        override val unitOfMeasure: String,
        override val vatCode: String,
        override val glAccountCode: String?,
        override val categoryName: String?,
        val productCategoryId: Long?,
        val defaultPrice: BigDecimal?,
    ) : CatalogItem() {
        override val itemType = CatalogItemType.PRODUCT
    }
}

enum class CatalogItemType {
    MATERIAL,      // Generic material (all customers)
    WASTE_STREAM,  // Consignor-specific waste stream (linked to material)
    PRODUCT        // Non-material product (all customers)
}
```

#### API Endpoint for Weight Ticket Lines

```kotlin
@GetMapping("/catalog/items/for-weight-ticket")
fun getCatalogItemsForWeightTicket(
    @RequestParam consignorPartyId: Long,
    @RequestParam query: String?,
    @RequestParam itemTypes: Set<CatalogItemType>? = null,
    @RequestParam limit: Int = 50
): List<CatalogItemResponse> {
    // Returns:
    // 1. WasteStreamItems for the given consignor (filtered by consignorPartyId)
    // 2. ProductItems (available to all)
    // 3. Materials (available to all)
}
```

#### Weight Ticket Line Structure

Weight ticket lines continue to reference `wasteStreamNumber` for materials (for regulatory compliance), but can now also reference products:

```kotlin
data class WeightTicketLine(
    // For waste streams (materials with regulatory requirements)
    val wasteStreamNumber: String?,
    
    // For products (non-material items)
    val productId: Long?,
    
    // Common fields
    val quantity: BigDecimal,
    val unitOfMeasure: String,
    
    // Snapshot fields (denormalized at creation time)
    val catalogItemName: String,
)
```

#### Database Schema for Weight Ticket Lines

```sql
-- Weight ticket lines can reference either a waste stream OR a product
ALTER TABLE weight_ticket_lines 
ADD COLUMN product_id BIGINT REFERENCES products(id);

-- Constraint: must have either waste_stream_number OR product_id, not both
ALTER TABLE weight_ticket_lines 
ADD CONSTRAINT chk_catalog_item_reference 
CHECK (
    (waste_stream_number IS NOT NULL AND product_id IS NULL) OR
    (waste_stream_number IS NULL AND product_id IS NOT NULL)
);
```

#### Benefits

- **Regulatory compliance**: Waste streams retain all regulatory data (eural code, processing method)
- **Consignor filtering**: Only waste streams for the selected consignor are shown
- **Material linking**: Waste streams inherit VAT code, GL account from linked material
- **Product support**: Non-material items can be added to weight tickets
- **Display name flexibility**: Waste stream name is used for display, not material name

#### Migration Strategy

1. Add `material_id` column to `waste_streams` table
2. Add material select field to WasteStreamForm in frontend
3. Use material name as waste stream name (snapshot in `name` field)
4. Update `CatalogQueryService` to return all three types for weight ticket context
5. Update frontend to use new catalog endpoint with consignor filtering

### 7. Impact on Invoice Lines (ADR-0020 Update)

The `InvoiceLine` from ADR-0020 should reference catalog items using the same pattern:

```kotlin
data class InvoiceLine(
    val id: InvoiceLineId,
    val lineNumber: Int,
    val date: LocalDate,
    
    // Catalog item reference
    val catalogItemId: Long,
    val catalogItemType: CatalogItemType,  // MATERIAL or PRODUCT
    
    // Snapshot fields (denormalized at invoice creation)
    val catalogItemCode: String,
    val catalogItemName: String,
    val description: String,
    val orderReference: String?,           // Weight ticket number, transport number
    val vatCode: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val unitOfMeasure: String,
    val totalExclVat: BigDecimal,
)
```

When saving an invoice line:

- Store `catalogItemId` + `catalogItemType` to identify the source
- Snapshot the relevant fields (code, name, price) at invoice creation time
- This allows the invoice to remain accurate even if catalog items change later

---

## Pros and Cons of the Options

### Option 1: Separate Tables with API Union (Chosen)

**Pros**:

- No changes to existing materials table
- Clean separation of concerns
- Each entity can evolve independently
- Simple queries on individual tables
- Easy to add material-specific or product-specific fields

**Cons**:

- Union logic in application layer
- Two queries instead of one (mitigated by parallel execution)
- Need to store `itemType` alongside `id` for disambiguation

### Option 2: Single Table Inheritance

**Pros**:

- Single query for all items
- Simpler persistence layer

**Cons**:

- Nullable columns for type-specific fields
- Migration risk for existing materials
- Less flexibility for divergent requirements
- Violates single responsibility

### Option 3: Database View

**Pros**:

- Single query point
- No application-level union logic

**Cons**:

- Views can be slower for complex queries
- Harder to maintain indexes
- Limited flexibility for filtering
- Database-specific syntax

### Option 4: Polymorphic Association

**Pros**:

- Strong domain modeling

**Cons**:

- Complex persistence mapping
- Harder to query efficiently
- Over-engineering for this use case

---

## Implementation Phases

### Phase 1: Products Infrastructure (Completed)

1. Add `gl_account_code` column to existing `materials` table ✓
2. Create `product_categories` table ✓
3. Create `products` table (includes `gl_account_code`) ✓
4. Seed common product categories and products
5. Implement Product domain model and repository ✓

### Phase 2: Catalog Query Service (Completed)

1. Implement `CatalogQueryService` ✓
2. Create REST endpoint `/api/catalog/items` ✓
3. Add OpenAPI documentation ✓

### Phase 3: Waste Stream to Material Linking (New)

1. Add `material_id` column to `waste_streams` table
2. Update `WasteStream` domain model to include `materialId`
3. Create data migration to link existing waste streams to materials
4. Add `WasteStreamItem` to `CatalogItem` sealed class
5. Create `/api/catalog/items/for-weight-ticket` endpoint with consignor filtering
6. Update `CatalogQueryService` to return waste streams for weight ticket context

### Phase 4: Frontend Weight Ticket Integration

1. Update `WeightTicketLinesTab` to fetch catalog items instead of waste streams
2. Use new endpoint with `consignorPartyId` parameter
3. Display waste stream name in dropdown (not material name)
4. Retain `wasteStreamNumber` in form values for regulatory compliance

### Phase 5: Product Support in Weight Tickets (Future)

1. Add `product_id` column to `weight_ticket_lines` table
2. Update weight ticket line form to support product selection
3. Add constraint to ensure either `waste_stream_number` OR `product_id` is set

### Phase 6: Invoice Integration (Future)

1. Update invoice line creation to use catalog items
2. Ensure proper snapshotting of item details

---

## More Information

### Related ADRs

- ADR-0020: Invoice Domain Model (uses catalog items for invoice lines)

### Performance Considerations

- Materials table: ~289 rows (small, cacheable)
- Products table: Expected <100 rows (small, cacheable)
- Combined search with limit 50 is fast
- Consider caching catalog items with short TTL

### Open Questions

1. Should products have time-bound pricing like materials (`material_prices` table)?
2. Should we add a `products_prices` table for consistency?
3. Do we need product-specific fields beyond the common ones?
