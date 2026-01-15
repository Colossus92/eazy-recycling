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

-- ============================================================================
-- TRANSPORTS TABLE INDEXES
-- These indexes optimize PlanningService.getPlanningByDate() queries
-- ============================================================================

-- Index for date range queries (used in every planning view request)
CREATE INDEX IF NOT EXISTS idx_transports_pickup_date_time
ON transports (pickup_date_time);

-- Index for truck filtering (used when filtering by truck)
CREATE INDEX IF NOT EXISTS idx_transports_truck_id
ON transports (truck_id);

-- Index for driver filtering (used when filtering by driver)
CREATE INDEX IF NOT EXISTS idx_transports_driver_id
ON transports (driver_id);

-- Composite index for the most common query pattern: date range + truck + sequence
CREATE INDEX IF NOT EXISTS idx_transports_planning
ON transports (pickup_date_time, truck_id, sequence_number);
