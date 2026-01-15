-- Migration: Add indexes to improve company search performance
-- These indexes optimize the search query in CompanyRepository.searchPaginated()

-- Index for soft delete filter (used in every query)
-- Even if no companies are deleted, this partial index is very small and helps the query planner
CREATE INDEX IF NOT EXISTS idx_companies_deleted_at 
ON companies (deleted_at) 
WHERE deleted_at IS NULL;

-- Index for name search (case-insensitive LIKE queries)
CREATE INDEX IF NOT EXISTS idx_companies_name_lower 
ON companies (LOWER(name));

-- Index for city search (case-insensitive LIKE queries)  
CREATE INDEX IF NOT EXISTS idx_companies_city_lower
ON companies (LOWER(city));

-- Composite index for common search pattern: active companies sorted by code/name
-- This covers the default sort order and the deleted_at filter together
CREATE INDEX IF NOT EXISTS idx_companies_active_code_name
ON companies (code, name)
WHERE deleted_at IS NULL;
