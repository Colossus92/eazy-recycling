-- Add is_tenant_company column to companies table
-- This flag identifies which company is the tenant/processor company
-- Used for multi-tenancy preparation (see ADR-0024)

ALTER TABLE "public"."companies" 
ADD COLUMN "is_tenant_company" boolean DEFAULT false NOT NULL;

-- Create index for efficient filtering
CREATE INDEX idx_companies_is_tenant ON companies(is_tenant_company) WHERE is_tenant_company = TRUE;

-- Add comment for documentation
COMMENT ON COLUMN "public"."companies"."is_tenant_company" IS 'Indicates if this company is the tenant/processor company. Only one company per tenant should have this flag set to true.';
