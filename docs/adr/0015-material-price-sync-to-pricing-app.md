# ADR-0015: Material Price Synchronization to External Pricing App

## Status

Proposed

## Context and Problem Statement

The company operates an external customer-facing pricing app (hosted at `whd.vanooapps.nl`) that displays current material prices to customers. Currently, this app is managed separately, requiring manual updates when prices change.

We need to synchronize material prices from Eazy Recycling (the source of truth) to the external pricing app to:

1. Eliminate manual data entry in two systems
2. Ensure customers always see accurate, up-to-date prices
3. Provide admin users with clear control over what gets published

### Current State

#### Eazy Recycling Data Model

**`materials` table:**

| Column | Type | Description |
|--------|------|-------------|
| `id` | bigint | Primary key |
| `code` | text | Internal material code |
| `name` | text | Material name |
| `material_group_id` | bigint | FK to material groups |
| `unit_of_measure` | text | e.g., "kg" |
| `status` | text | ACTIVE/INACTIVE |

**`material_prices` table:**

| Column | Type | Description |
|--------|------|-------------|
| `id` | bigint | Primary key |
| `material_id` | bigint | FK to materials |
| `price` | numeric | Price per unit |
| `currency` | text | e.g., "EUR" |
| `valid_from` | timestamptz | Start of validity period |
| `valid_to` | timestamptz | End of validity period (null = current) |

#### External Pricing App API

The external app provides a REST API with the following operations:

| Operation | Endpoint | Method |
|-----------|----------|--------|
| Login | `/api/v1/login` | POST |
| List products | `/api/v1/products` | GET |
| Create product | `/api/v1/products/create` | POST |
| Update product | `/api/v1/products/{id}/update` | POST |
| Delete product | `/api/v1/products/{id}/delete` | DELETE |

**Product structure in external app:**

```json
{
  "id": 1,
  "name": "Handgepeld koper",
  "price": "8,45",
  "price_status": "1",
  "created_at": "02-10-2024",
  "updated_at": "01-12-2025"
}
```

Where `price_status`:

- `0` = unchanged
- `1` = increased
- `2` = decreased

---

## Considered Options

### Option 1: Automatic Real-Time Sync

Automatically sync to the external app whenever a material price is created or updated.

**Pros:**

- Always up-to-date
- No manual intervention required

**Cons:**

- Risk of publishing incomplete or incorrect data
- No review step before customer visibility
- Complex error handling for failed syncs
- May overwhelm external API during bulk updates

### Option 2: Manual "Sync All" Button with Publish Flag

Admin explicitly triggers sync via a button. Only materials marked for publishing are synced.

**Pros:**

- Full control over when prices are published
- Review step before customer visibility
- Simple error handling (sync or don't sync)
- Clear audit trail of sync operations
- Batch operation is efficient

**Cons:**

- Requires manual action
- Potential for forgetting to sync

### Option 3: Scheduled Sync with Publish Flag

Automatically sync on a schedule (e.g., daily at 6:00 AM) for materials marked for publishing.

**Pros:**

- Automated without real-time complexity
- Predictable sync timing

**Cons:**

- Delay between price change and customer visibility
- Less control over exact timing
- Still needs manual "sync now" option for urgent updates

---

## Decision Outcome

Chosen option: **Option 2 - Manual "Sync All" Button with Publish Flag**

This option provides the best balance of control and simplicity:

1. Admin has explicit control over when prices become visible to customers
2. The `publish_to_pricing_app` flag allows selective publishing
3. Simple implementation with clear user feedback
4. Can be extended to scheduled sync later if needed

---

## Implementation

### 1. Database Schema Extension

Create a **separate table** for pricing app sync metadata to keep the generic `materials` table clean and isolate this company-specific feature:

```sql
-- Separate table for pricing app sync metadata
-- This keeps the generic materials table clean and isolates company-specific features
CREATE TABLE material_pricing_app_sync (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL UNIQUE REFERENCES materials(id) ON DELETE CASCADE,
    publish_to_pricing_app BOOLEAN NOT NULL DEFAULT FALSE,
    external_pricing_app_id INTEGER,
    external_pricing_app_synced_at TIMESTAMPTZ,
    last_synced_price NUMERIC(19, 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_material_pricing_app_sync_material_id ON material_pricing_app_sync(material_id);
CREATE INDEX idx_material_pricing_app_sync_publish ON material_pricing_app_sync(publish_to_pricing_app) 
    WHERE publish_to_pricing_app = TRUE;

COMMENT ON TABLE material_pricing_app_sync IS 'Sync metadata for materials to external pricing app (whd.vanooapps.nl)';
COMMENT ON COLUMN material_pricing_app_sync.material_id IS 'FK to materials table';
COMMENT ON COLUMN material_pricing_app_sync.publish_to_pricing_app IS 'Whether this material should be visible in the customer pricing app';
COMMENT ON COLUMN material_pricing_app_sync.external_pricing_app_id IS 'Product ID in the external pricing app';
COMMENT ON COLUMN material_pricing_app_sync.external_pricing_app_synced_at IS 'Timestamp of last successful sync';
COMMENT ON COLUMN material_pricing_app_sync.last_synced_price IS 'The price that was last synced';
```

**Rationale for separate table:**

- Keeps the generic `materials` table clean and reusable
- Isolates company-specific pricing app integration
- Easy to remove or modify without affecting core material data
- Clear separation of concerns between domain data and integration metadata

Add audit table for sync operations:

```sql
CREATE TABLE material_price_sync_log (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    external_product_id INTEGER,
    action TEXT NOT NULL, -- 'create', 'update', 'delete'
    price_synced NUMERIC,
    price_status_sent INTEGER, -- 0, 1, or 2
    status TEXT NOT NULL, -- 'success', 'error'
    error_message TEXT,
    synced_at TIMESTAMPTZ DEFAULT NOW(),
    synced_by TEXT
);

CREATE INDEX idx_material_price_sync_log_material_id ON material_price_sync_log(material_id);
CREATE INDEX idx_material_price_sync_log_synced_at ON material_price_sync_log(synced_at);
```

### 2. Sync Logic

#### Determining Current Price

For each material, the current price is determined by:

```sql
SELECT mp.price, mp.currency
FROM material_prices mp
WHERE mp.material_id = :materialId
  AND mp.valid_from <= NOW()
  AND (mp.valid_to IS NULL OR mp.valid_to > NOW())
ORDER BY mp.valid_from DESC
LIMIT 1;
```

#### Calculating Price Status

The `price_status` for the external app is calculated by comparing the current price with the last synced price:

```kotlin
fun calculatePriceStatus(currentPrice: BigDecimal, lastSyncedPrice: BigDecimal?): Int {
    if (lastSyncedPrice == null) return 0 // First sync, no change indicator
    return when {
        currentPrice > lastSyncedPrice -> 1 // Increased
        currentPrice < lastSyncedPrice -> 2 // Decreased
        else -> 0 // Unchanged
    }
}
```

#### Sync Flow

```text
+------------------------------------------------------------------+
|                    ADMIN CLICKS "SYNC ALL"                        |
+------------------------------------------------------------------+
                              |
                              v
                    +-------------------+
                    | Authenticate with |
                    | external API      |
                    +-------------------+
                              |
                              v
                    +-------------------+
                    | Fetch sync records|
                    | FROM material_    |
                    | pricing_app_sync  |
                    | WHERE publish_to_ |
                    | pricing_app = true|
                    +-------------------+
                              |
                              v
                    +-------------------+
                    | For each material |
                    +--------+----------+
                             |
              +--------------+--------------+
              |                             |
              v                             v
    +-------------------+         +-------------------+
    | Has external_     |         | No external_      |
    | pricing_app_id?   |         | pricing_app_id    |
    +--------+----------+         +--------+----------+
             |                             |
             v                             v
    +-------------------+         +-------------------+
    | UPDATE product    |         | CREATE product    |
    | in external app   |         | in external app   |
    +--------+----------+         +--------+----------+
             |                             |
             v                             v
    +-------------------+         +-------------------+
    | Update synced_at  |         | Store returned ID |
    +-------------------+         | Update synced_at  |
                                  +-------------------+
```

#### Handling Unpublished Materials

When `publish_to_pricing_app` is set to `false` for a material that was previously synced,  delete the product from the external app

### 3. API Integration

#### Authentication

Store credentials securely in environment variables:

```properties
PRICING_APP_BASE_URL=https://whd.vanooapps.nl/api/v1
PRICING_APP_EMAIL=<configured-email>
PRICING_APP_PASSWORD=<configured-password>
```

#### Token Management

- Authenticate once at the start of sync batch
- Token is valid for the duration of the sync operation
- Re-authenticate if token expires during long sync

### 4. User Interface

#### Material Prices Tab Enhancements

1. **Add "Publiceren" column** to the table showing `publish_to_pricing_app` status
2. **Add "Sync All" button** in the toolbar
3. **Show last sync timestamp** in the UI header
4. **Add sync status indicator** per material (synced/pending/error)

#### Sync Confirmation Dialog

Before syncing, show a confirmation dialog with:

- Number of materials to sync
- Number of new products to create
- Number of existing products to update
- Number of products to delete (if unpublished)

#### Sync Result Feedback

After sync completes, show:

- Success count
- Error count with details
- Link to view sync log

### 5. Backend Service Structure

```kotlin
// Domain service
class MaterialPriceSyncService(
    private val materialRepository: MaterialRepository,
    private val materialPriceRepository: MaterialPriceRepository,
    private val pricingAppClient: PricingAppClient,
    private val syncLogRepository: MaterialPriceSyncLogRepository
) {
    fun syncAllToPricingApp(triggeredBy: String): SyncResult {
        val token = pricingAppClient.authenticate()
        val materialsToSync = materialRepository.findAllWithPublishFlag(true)
        
        val results = materialsToSync.map { material ->
            syncMaterial(material, token, triggeredBy)
        }
        
        return SyncResult(
            successCount = results.count { it.success },
            errorCount = results.count { !it.success },
            details = results
        )
    }
    
    private fun syncMaterial(
        material: Material, 
        token: String, 
        triggeredBy: String
    ): MaterialSyncResult {
        val currentPrice = materialPriceRepository.findCurrentPrice(material.id)
            ?: return MaterialSyncResult.error("No current price found")
        
        val priceStatus = calculatePriceStatus(
            currentPrice.price, 
            material.lastSyncedPrice
        )
        
        return try {
            if (material.externalPricingAppId == null) {
                val response = pricingAppClient.createProduct(
                    token, material.name, currentPrice.price, priceStatus
                )
                material.externalPricingAppId = response.id
            } else {
                pricingAppClient.updateProduct(
                    token, material.externalPricingAppId, 
                    material.name, currentPrice.price, priceStatus
                )
            }
            material.externalPricingAppSyncedAt = Instant.now()
            materialRepository.save(material)
            
            logSync(material, currentPrice.price, priceStatus, "success", null, triggeredBy)
            MaterialSyncResult.success(material.id)
        } catch (e: Exception) {
            logSync(material, currentPrice.price, priceStatus, "error", e.message, triggeredBy)
            MaterialSyncResult.error(e.message)
        }
    }
}
```

---

## Risks and Mitigations

### Risk 1: External API Unavailability

**Risk**: External pricing app API is down during sync.

**Mitigation**:

- Implement retry logic with exponential backoff
- Log failed syncs for manual retry
- Show clear error messages to admin

### Risk 2: Credential Exposure

**Risk**: API credentials could be exposed.

**Mitigation**:

- Store credentials in environment variables
- Never log credentials
- Use HTTPS for all API calls

### Risk 3: Price Mismatch

**Risk**: Price in external app doesn't match Eazy Recycling.

**Mitigation**:

- Log all sync operations with prices sent
- Provide reconciliation report comparing both systems
- Admin can trigger re-sync at any time

### Risk 4: Orphaned Products

**Risk**: Products exist in external app but not in Eazy Recycling.

**Mitigation**:

- Initial setup: map existing external products to materials
- Sync logic handles deletions when `publish_to_pricing_app` is disabled
- Provide reconciliation view showing unlinked products

---

## Consequences

### Positive

- Single source of truth for material prices
- Admin has full control over customer-visible prices
- Clear audit trail of all sync operations
- Eliminates manual data entry errors

### Negative

- Requires manual action to sync (by design)
- External API dependency for customer-facing prices
- Need to maintain API credentials

### Neutral

- Initial setup requires mapping existing external products to materials
- Admin training required for sync workflow

---

## Future Considerations

1. **Scheduled sync**: Add optional daily auto-sync as a fallback
2. **Webhook notifications**: If external app adds webhook support, enable real-time updates
3. **Bidirectional sync**: If prices can be edited in external app, handle conflicts
4. **Bulk operations**: If external API adds batch endpoints, optimize sync performance

---

## References

- External Pricing App API: `https://whd.vanooapps.nl/api/v1`
- Related ADR: [ADR-0012: Exact Online Company Synchronization Strategy](./0012-exact-online-synchronization-strategy.md)
