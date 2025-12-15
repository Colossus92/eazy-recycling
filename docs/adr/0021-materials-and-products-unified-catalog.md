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
| `status` | text | ACTIVE/INACTIVE |

**Material Groups** (`material_groups` table):

| Field | Type | Description |
|-------|------|-------------|
| `id` | bigint | Primary key |
| `code` | text | Group code |
| `name` | text | Group name (e.g., "Ijzer", "Kabel", "Sloop") |

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
    abstract val categoryName: String?
    abstract val itemType: CatalogItemType
    
    data class MaterialItem(
        override val id: Long,
        override val code: String,
        override val name: String,
        override val unitOfMeasure: String,
        override val vatCode: String,
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

### 6. Impact on Weight Tickets

Weight tickets serve as **order-like documents** that can include both materials and products. This requires a significant change to the weight ticket line structure.

#### Current State

Weight ticket lines currently reference `WasteStreamNumber`, which indirectly links to materials.

#### New Design

Weight ticket lines should reference **catalog items** (materials OR products) directly:

```kotlin
data class WeightTicketLine(
    val catalogItemId: Long,
    val catalogItemType: CatalogItemType,  // MATERIAL or PRODUCT
    val quantity: BigDecimal,              // Weight in kg for materials, hours/pieces for products
    val unitOfMeasure: String,
    
    // Snapshot fields (denormalized at creation time)
    val catalogItemCode: String,
    val catalogItemName: String,
    val wasteStreamNumber: String?,        // Only for materials linked to waste streams
)
```

#### Database Schema Change

```sql
-- Updated weight_ticket_lines table
ALTER TABLE weight_ticket_lines ADD COLUMN catalog_item_id BIGINT;
ALTER TABLE weight_ticket_lines ADD COLUMN catalog_item_type TEXT;  -- 'MATERIAL' or 'PRODUCT'
ALTER TABLE weight_ticket_lines ADD COLUMN catalog_item_code TEXT;
ALTER TABLE weight_ticket_lines ADD COLUMN catalog_item_name TEXT;
ALTER TABLE weight_ticket_lines ADD COLUMN unit_of_measure TEXT;

-- waste_stream_number is RETAINED for materials (regulatory/traceability purposes)
-- Migration: Populate catalog_item_id/type from existing waste_stream_number -> material mapping
```

#### Benefits

- **Unified line handling**: Both materials and products in same line structure
- **Direct pricing**: Can look up prices directly from catalog item
- **Flexibility**: Add transport hours, fees, etc. directly to weight ticket
- **Invoice generation**: Direct mapping from weight ticket lines to invoice lines

#### Migration Strategy

1. Add new columns to `weight_ticket_lines`
2. Create mapping from `waste_stream_number` to `material_id`
3. Backfill existing lines with catalog item references
4. Update UI to use catalog item selection (while retaining `waste_stream_number` for materials)

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

### Phase 1: Products Infrastructure

1. Create `product_categories` table
2. Create `products` table
3. Seed common product categories and products
4. Implement Product domain model and repository

### Phase 2: Catalog Query Service

1. Implement `CatalogQueryService`
2. Create REST endpoint `/api/catalog/items`
3. Add OpenAPI documentation

### Phase 3: Frontend Integration

1. Create `CatalogItemSelect` component
2. Integrate into invoice form
3. Add product management UI in master data section

### Phase 4: Invoice Integration

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
