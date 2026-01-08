# External Pricing App Synchronization

## Status

Accepted

## Context and Problem Statement

The first customer operates an external standalone application (whd.vanooapps.nl) that displays material prices to their end customers. This external app maintains its own data and requires synchronization with our system's material prices.

**Key Requirements:**
1. Materials can be optionally marked for publishing to the external app
2. Users can preview changes before syncing (diff view showing price changes with up/down/equal indicators)
3. Sync operations should be durable (using JobRunr for reliability)
4. This feature is customer-specific and should be loosely coupled for easy removal/disabling for other tenants

**External API Endpoints:**

| Operation | Endpoint | Method | Auth |
|-----------|----------|--------|------|
| Login | `POST /api/v1/login` | POST | None (returns bearer token) |
| List products | `GET /api/v1/products` | GET | Bearer token |
| Create product | `POST /api/v1/products/create` | POST | Bearer token |
| Update product | `POST /api/v1/products/{id}/update` | POST | Bearer token |
| Delete product | `DELETE /api/v1/products/{id}/delete` | DELETE | Bearer token |

**Price Status Values:**
- `0` = Unchanged (onveranderd)
- `1` = Increased (gestegen)
- `2` = Decreased (gedaald)

## Considered Options

1. **Direct synchronization on each price change** - Immediate sync when material price is updated
2. **Manual batch synchronization with preview** - User reviews diff and approves sync
3. **Scheduled automatic synchronization** - Background job syncs periodically

## Decision Outcome

**Chosen option: Manual batch synchronization with preview and JobRunr durability**

This approach provides:
- User control over when changes are pushed
- Visual diff to review changes before committing
- Durable job execution ensuring sync completes even if server restarts
- Loose coupling via separate sync table and configuration

## Implementation Design

### 1. Database Schema

```sql
-- Separate table for pricing app sync metadata (isolates customer-specific feature)
CREATE TABLE material_pricing_app_sync (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL UNIQUE REFERENCES materials(id) ON DELETE CASCADE,
    publish_to_pricing_app BOOLEAN NOT NULL DEFAULT FALSE,
    external_pricing_app_id INTEGER,           -- ID in external app
    external_pricing_app_synced_at TIMESTAMPTZ,
    last_synced_price NUMERIC(19, 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit log for sync operations
CREATE TABLE material_price_sync_log (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materials(id),
    external_product_id INTEGER,
    action TEXT NOT NULL,                       -- 'create', 'update', 'delete'
    price_synced NUMERIC(19, 4),
    price_status_sent INTEGER,                  -- 0, 1, or 2
    status TEXT NOT NULL,                       -- 'success', 'error'
    error_message TEXT,
    synced_at TIMESTAMPTZ DEFAULT NOW(),
    synced_by TEXT
);
```

### 2. Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Frontend                                   │
│  MaterialPriceForm (toggle sync + dropdown) → MaterialPriceTab      │
│                                  ↓                                   │
│                        "Push naar app" button                        │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    MaterialPriceSyncController                       │
│                                                                      │
│  GET  /material-prices/sync/preview    → Generate diff preview       │
│  POST /material-prices/sync/execute    → Approve and execute sync   │
│  GET  /material-prices/sync/products   → List external products     │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    MaterialPriceSyncService                          │
│                                                                      │
│  generateSyncPreview()  → Returns toCreate, toUpdate, toDelete      │
│  executeSync()          → Enqueues JobRunr jobs for each operation  │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    MaterialPriceSyncJobService                       │
│                    (JobRunr-based durability)                        │
│                                                                      │
│  @Job syncCreateProduct(materialId, name, price, priceStatus)       │
│  @Job syncUpdateProduct(externalId, name, price, priceStatus)       │
│  @Job syncDeleteProduct(externalId)                                 │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       PricingAppAdapter                              │
│                    (REST client implementation)                      │
│                                                                      │
│  Implements PricingAppSync port                                     │
│  Uses RestTemplate with Bearer token authentication                 │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
                    ┌─────────────────────────┐
                    │  External Pricing App   │
                    │  whd.vanooapps.nl       │
                    └─────────────────────────┘
```

### 3. Domain Port

```kotlin
interface PricingAppSync {
    fun fetchExternalProducts(): List<ExternalProduct>
    fun createProduct(request: CreateProductRequest): Int
    fun updateProduct(externalId: Int, request: UpdateProductRequest)
    fun deleteProduct(externalId: Int)
}

data class ExternalProduct(
    val id: Int,
    val name: String,
    val price: BigDecimal,
    val priceStatus: Int
)
```

### 4. Sync Preview Logic

```kotlin
fun generateSyncPreview(): SyncPreview {
    // 1. Get materials marked for publishing
    val syncRecords = syncRepository.findAllForPublishing()
    
    // 2. Fetch current external products
    val externalProducts = pricingAppSync.fetchExternalProducts()
    
    // 3. Categorize:
    //    - toCreate: materials with no external ID
    //    - toUpdate: materials where price or name differs from external
    //    - toDelete: external products not in our publish list
    //    - unchanged: materials matching external state
    
    // 4. Calculate price status for each update (0=same, 1=up, 2=down)
}
```

### 5. JobRunr Integration

Each sync operation is enqueued as a separate job for durability:

```kotlin
@Job(name = "Sync Create Product", retries = 3)
fun syncCreateProduct(materialId: Long, name: String, price: BigDecimal, priceStatus: Int) {
    val externalId = pricingAppSync.createProduct(CreateProductRequest(name, price, priceStatus))
    syncRepository.updateSyncMetadata(materialId, externalId, price)
    logSyncOperation(materialId, externalId, "create", price, priceStatus, "success", null)
}

@Job(name = "Sync Update Product", retries = 3)
fun syncUpdateProduct(materialId: Long, externalId: Int, name: String, price: BigDecimal, priceStatus: Int) {
    pricingAppSync.updateProduct(externalId, UpdateProductRequest(name, price, priceStatus))
    syncRepository.updateSyncMetadata(materialId, externalId, price)
    logSyncOperation(materialId, externalId, "update", price, priceStatus, "success", null)
}

@Job(name = "Sync Delete Product", retries = 3)
fun syncDeleteProduct(externalId: Int) {
    pricingAppSync.deleteProduct(externalId)
    logSyncOperation(null, externalId, "delete", null, null, "success", null)
}
```

### 6. Configuration

```yaml
pricing-app:
  base-url: https://whd.vanooapps.nl/api/v1
  bearer-token: ${PRICING_APP_BEARER_TOKEN}
```

### 7. Loose Coupling Strategy

The feature is isolated through:
1. **Separate table**: `material_pricing_app_sync` - no changes to core `materials` table
2. **Configuration-based**: Feature requires `pricing-app.bearer-token` to be set
3. **Optional in UI**: Toggle in MaterialPriceForm only shows when feature is enabled
4. **Separate adapter**: `PricingAppAdapter` can be easily removed or replaced with a no-op

For other tenants, simply:
- Don't configure `pricing-app.bearer-token`
- Hide the sync UI elements via feature flag (future enhancement)

## API Responses

### Preview Response

```json
{
  "toCreate": [
    {
      "materialId": 123,
      "materialName": "Koper",
      "currentPrice": 8.50,
      "lastSyncedPrice": null,
      "externalProductId": null,
      "priceStatus": 0,
      "priceStatusLabel": "Onveranderd"
    }
  ],
  "toUpdate": [
    {
      "materialId": 456,
      "materialName": "Messing",
      "currentPrice": 4.80,
      "lastSyncedPrice": 4.00,
      "externalProductId": 100,
      "priceStatus": 1,
      "priceStatusLabel": "Gestegen"
    }
  ],
  "toDelete": [
    {
      "externalProductId": 200,
      "productName": "Old Product"
    }
  ],
  "unchanged": [],
  "summary": {
    "totalToCreate": 1,
    "totalToUpdate": 1,
    "totalToDelete": 1,
    "totalUnchanged": 0
  }
}
```

### Execute Response

```json
{
  "created": 1,
  "updated": 1,
  "deleted": 1,
  "failed": 0,
  "success": true,
  "errors": []
}
```

## Pros and Cons

### Chosen Approach: Manual Batch Sync with Preview

**Pros:**
- User has full control over what gets pushed
- Visual diff prevents accidental sync errors
- JobRunr ensures reliability even if server restarts mid-sync
- Loose coupling allows easy removal for other tenants
- Audit log provides traceability

**Cons:**
- Requires manual action to sync (not real-time)
- User must remember to sync after price changes

### Alternative: Real-time Sync (Rejected)

**Pros:**
- Prices always in sync
- No manual intervention needed

**Cons:**
- No review before publishing
- Higher API call volume
- Harder to debug sync issues
- Tighter coupling with core price management

## More Information

### Future Enhancements

1. **Feature flag per tenant**: Add tenant-level configuration to enable/disable
2. **Scheduled sync option**: Allow configuring automatic daily sync
3. **Retry dashboard**: UI to view and retry failed sync jobs
4. **Webhook support**: If external app adds webhooks, could enable real-time sync

### Security Considerations

- Bearer token stored as environment variable
- Token should be rotated periodically
- All API calls logged for audit purposes
