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
