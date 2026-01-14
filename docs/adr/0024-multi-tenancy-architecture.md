# Multi-Tenancy Architecture with Row-Level Security

## Context and Problem Statement

The application needs to evolve from a single-tenant system (hardcoded `Tenant` object with WHD Metaalrecycling as the only organization) to a multi-tenant SaaS platform supporting approximately 100 organizations with ~1000 total users. Each organization (tenant) must have complete data isolation - they should only see and maintain their own data with zero possibility of cross-tenant data access.

Key requirements:

- Support for 100 organizations with ~10 users each on average
- Strict data isolation between tenants
- Tenant company should appear naturally in company lists (tenant IS a company with PROCESSOR role)
- Integration with external systems (Exact Online) where each tenant has separate credentials
- Maintain existing Supabase infrastructure
- User authentication via external provider (Clerk) that integrates with Supabase

## Considered Options

- **Schema-per-tenant**: Each tenant gets a dedicated PostgreSQL schema
- **Database-per-tenant**: Each tenant gets a dedicated database instance
- **Row-level tenancy with RLS**: Single schema with `tenant_id` column and PostgreSQL Row-Level Security policies
- **Row-level tenancy with application-layer filtering**: Single schema with application-enforced tenant filtering

## Decision Outcome

Chosen option: **Row-level tenancy with RLS and session variables**, because:

- Scales efficiently to 100+ organizations (schema-per-tenant becomes unwieldy beyond ~50)
- Leverages PostgreSQL Row-Level Security as defense-in-depth
- Simpler migrations (single schema)
- Works natively with Supabase RLS
- Better resource utilization (shared connection pools)
- Session variable approach allows backend to control tenant context without requiring JWT tokens

### Implementation Strategy

#### 1. Tenant Data Model

```sql
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    processor_party_id TEXT UNIQUE NOT NULL,
    exact_division_id INTEGER,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- The tenant IS a company - marked with flag
ALTER TABLE companies ADD COLUMN is_tenant_company BOOLEAN DEFAULT FALSE;
CREATE INDEX idx_companies_tenant ON companies(is_tenant_company) WHERE is_tenant_company = TRUE;
```

**Status**: ✅ `is_tenant_company` column already implemented

#### 2. Tenant Context via PostgreSQL Session Variables

Instead of using repository-layer filtering, we set a PostgreSQL session variable at the start of each transaction:

```kotlin
// TenantContext.kt
object TenantContext {
    private val current = ThreadLocal<TenantId>()
    
    fun setTenant(tenantId: TenantId) = current.set(tenantId)
    fun getTenant(): TenantId = current.get() 
        ?: throw IllegalStateException("No tenant in context")
    fun clear() = current.remove()
}

// TenantAwareTransactionManager.kt
@Component
class TenantAwareTransactionManager : PlatformTransactionManager {
    
    @PersistenceContext
    private lateinit var entityManager: EntityManager
    
    override fun getTransaction(definition: TransactionDefinition): TransactionStatus {
        val status = super.getTransaction(definition)
        
        // Set current_tenant session variable
        val tenantId = TenantContext.getTenant()
        entityManager.createNativeQuery(
            "SET LOCAL app.current_tenant = :tenantId"
        ).setParameter("tenantId", tenantId.value.toString())
         .executeUpdate()
        
        return status
    }
}
```

#### 3. Row-Level Security Policies

RLS policies read from the session variable, not from JWT token:

```sql
-- Enable RLS on all tenant-scoped tables
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE waste_streams ENABLE ROW LEVEL SECURITY;
ALTER TABLE weight_tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;
-- ... all other tables

-- Policy using session variable
CREATE POLICY tenant_isolation_policy ON companies
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

CREATE POLICY tenant_isolation_policy ON waste_streams
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- Repeat for all tenant-scoped tables
```

**Benefits of session variable approach:**

- Backend controls tenant context (no dependency on JWT structure)
- Works for background jobs and scheduled tasks
- RLS acts as defense-in-depth (even if application logic fails)
- Simpler testing (set variable directly in test setup)

#### 4. Authentication with Clerk

**Chosen provider**: Clerk

- Built-in Organizations feature maps directly to tenants
- Official Supabase integration template
- JWT includes `org_id` claim
- Price-effective for 100 orgs (~$50/mo)

**JWT Structure:**

```json
{
  "sub": "user_uuid",
  "org_id": "tenant_uuid",
  "org_role": "admin",
  "user_roles": ["ADMIN", "PLANNER"],
  "iss": "https://clerk.eazy-recycling.app"
}
```

**Request Flow:**

```text
Request → TenantFilter → Extract org_id from JWT 
        → TenantContext.setTenant() 
        → Transaction begins
        → SET LOCAL app.current_tenant 
        → RLS enforces isolation
        → Transaction commits
        → TenantContext.clear()
```

#### 5. Migration Strategy

**Phase 1: Infrastructure (Already Done)**

- ✅ Add `is_tenant_company` column to companies table

**Phase 2: Tenant Table (Future)**

- Create `tenants` table
- Migrate existing hardcoded tenant (WHD Metaalrecycling)

**Phase 3: Add tenant_id Columns (Future)**

```sql
-- Add nullable column first
ALTER TABLE companies ADD COLUMN tenant_id UUID REFERENCES tenants(id);
ALTER TABLE waste_streams ADD COLUMN tenant_id UUID REFERENCES tenants(id);
-- ... all other tables

-- Backfill with current tenant
UPDATE companies SET tenant_id = (SELECT id FROM tenants LIMIT 1);
-- ... all other tables

-- Make non-nullable
ALTER TABLE companies ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX idx_companies_tenant_id ON companies(tenant_id);
```

**Phase 4: Enable RLS (Future)**

- Enable RLS on all tables
- Create policies using session variable
- Test thoroughly

**Phase 5: Replace Tenant Object (Future)**

```kotlin
// Before:
WHERE proc.processor_id = '${Tenant.processorPartyId.number}'

// After:
WHERE ws.tenant_id = :tenantId  // From TenantContext.getTenant()
```

#### 6. External System Integration (Exact Online)

Each tenant has separate Exact Online credentials:

```sql
ALTER TABLE exact_tokens ADD COLUMN tenant_id UUID REFERENCES tenants(id);
CREATE UNIQUE INDEX idx_exact_tokens_tenant ON exact_tokens(tenant_id);

ALTER TABLE companies_sync ADD COLUMN tenant_id UUID REFERENCES tenants(id);
CREATE INDEX idx_companies_sync_tenant ON companies_sync(tenant_id);
```

Sync jobs run per-tenant with tenant context set.

## Pros and Cons of the Options

### Schema-per-tenant

**Pros:**

- Maximum isolation at database level
- Easy to backup/restore individual tenants
- Can set different PostgreSQL parameters per tenant

**Cons:**

- Complex migrations (must run on N schemas)
- Poor scalability beyond ~50 tenants
- Connection pool management complexity
- Search paths and cross-schema queries problematic
- Not suitable for Supabase RLS patterns

### Database-per-tenant

**Pros:**

- Ultimate isolation
- Can geographically distribute tenants
- Different PostgreSQL versions possible

**Cons:**

- Massive operational overhead
- Very expensive (separate database instances)
- Overkill for 100 organizations
- Complex cross-tenant reporting
- Not compatible with single Supabase project

### Row-level with application filtering only

**Pros:**

- Simple to implement initially
- No RLS overhead

**Cons:**

- **No defense-in-depth**: Single bug = data leak
- No protection against SQL injection
- Manual filtering required everywhere
- Easy to forget filtering in new code
- Not recommended for SaaS

### Row-level with RLS and session variables (Chosen)

**Pros:**

- ✅ Scales to 100+ organizations efficiently
- ✅ Defense-in-depth: RLS prevents bugs from leaking data
- ✅ Single schema = simple migrations
- ✅ Native Supabase RLS support
- ✅ Backend controls tenant context (no JWT dependency)
- ✅ Works for background jobs and scheduled tasks
- ✅ Excellent performance with proper indexes
- ✅ Easy testing (set session variable directly)

**Cons:**

- Requires careful index design (`tenant_id` must be first in composite indexes)
- Session variable must be set for every transaction
- RLS policies must be created for every tenant-scoped table
- Initial migration effort to add tenant_id columns

## More Information

### Related ADRs

- ADR-0004: Choose DigitalOcean and Supabase
- ADR-0012: Exact Online synchronization strategy

### References

- [PostgreSQL Row-Level Security](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Supabase RLS Documentation](https://supabase.com/docs/guides/auth/row-level-security)
- [Clerk Organizations](https://clerk.com/docs/organizations/overview)
- [Session Variables in PostgreSQL](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADMIN-SET)

### Implementation Timeline

- **Now**: ✅ `is_tenant_company` flag on companies table
- **Phase 2** (Future): Set up Clerk authentication + create tenants table
- **Phase 3** (Future): Add tenant_id columns incrementally
- **Phase 4** (Future): Enable RLS policies
- **Phase 5** (Future): Replace all `Tenant` object usages

### Security Considerations

1. **Always validate org_id from JWT** before setting TenantContext
2. **RLS is mandatory** - never rely solely on application filtering
3. **Session variable must be set** - transaction should fail if not set
4. **Indexes must include tenant_id** as first column for performance
5. **Test cross-tenant access** thoroughly in integration tests
