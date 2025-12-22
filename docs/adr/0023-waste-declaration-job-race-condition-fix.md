# Waste Declaration Job Race Condition and Period Filtering

## Context and Problem Statement

The waste declaration system processes two types of jobs for monthly LMA (Dutch waste management authority) declarations:

1. **FIRST_RECEIVALS**: Declares waste streams that have never been declared before
2. **MONTHLY_RECEIVALS**: Declares waste streams that were previously declared and have new activity

Both jobs run independently via a scheduled processor (`WasteDeclarationJobProcessScheduler`) every 10 minutes. A race condition existed where the execution order of these jobs could cause incorrect behavior:

- If `FIRST_RECEIVALS` ran first for period `112025`, it would create `lma_declarations` records for that period
- When `MONTHLY_RECEIVALS` ran afterward for the same period `112025`, it would incorrectly include those waste streams because they now had declaration records (even though they were just created moments ago for the current period)
- This violated the business logic: `MONTHLY_RECEIVALS` should only process waste streams with declarations from **previous periods**, not the current period

Additionally, the system needed to handle legacy waste stream imports - waste streams that were declared in a legacy system but have no declaration records in the current system. Without proper handling, these would incorrectly appear as "first receival" waste streams.

## Considered Options

### Option 1: Add `declared_before` Boolean Column to `waste_streams` Table

Add a column to explicitly track whether a waste stream has ever been declared.

**Pros:**

- Simple and explicit flag
- Fast query performance (can be indexed)
- Easy to set during legacy imports

**Cons:**

- Redundant data (duplicates information in `lma_declarations`)
- Requires maintenance to keep in sync
- Migration complexity for backfilling existing data

### Option 2: Add Job Execution Locking/Ordering

Ensure `FIRST_RECEIVALS` always runs before `MONTHLY_RECEIVALS` using locks or explicit ordering.

**Pros:**

- Guarantees execution order
- No schema changes

**Cons:**

- Adds complexity and coupling between jobs
- Reduces system resilience (jobs can't run independently)
- Doesn't solve the fundamental query logic issue

### Option 3: Filter Declarations by Period in Monthly Receival Query

Modify the `MonthlyReceivalWasteStreamQuery` to only consider declarations from periods **before** the current period being processed.

**Pros:**

- Makes jobs truly independent (execution order doesn't matter)
- No schema changes required
- Correct business logic: "monthly receivals" means "declared before, active now"
- Simple and maintainable

**Cons:**

- Requires careful period comparison logic

### Option 4: Store Legacy Declarations as Special Records

When importing legacy waste streams, create corresponding `lma_declarations` records with a special status/period marker.

**Pros:**

- Maintains referential integrity (all declarations in one place)
- Provides audit trail of legacy imports
- No schema changes to `waste_streams` table
- Works seamlessly with existing query logic

**Cons:**

- Requires creating declaration records during import

## Decision Outcome

Chosen options: **Option 3 (Period Filtering) + Option 4 (Legacy Declaration Records)**, because:

1. **Option 3** solves the race condition by making job execution order independent
2. **Option 4** handles legacy imports cleanly without schema changes
3. Together they provide a complete solution with minimal complexity
4. Both leverage existing database structures without redundant data

### Implementation Details

#### 1. Period Filtering in Monthly Receival Query

Modified `MonthlyReceivalWasteStreamQueryAdapter.kt`:

```kotlin
LEFT JOIN lma_declarations d ON d.waste_stream_number = ws.number
  AND d.period < :currentPeriod  -- Critical: only previous periods
WHERE proc.processor_id = '${Tenant.processorPartyId.number}'
  AND d.id IS NOT NULL  -- Must have previous declaration
```

**Key change:** Added `AND d.period < :currentPeriod` to the join condition.

**Effect:**

- Waste streams with declarations ONLY in the current period are excluded
- Waste streams with declarations in previous periods are included (even if they also have current period declarations)
- Jobs can now run in any order without affecting results

#### 2. Legacy Declaration Records

When importing waste streams from legacy systems:

```kotlin
fun importLegacyWasteStream(wasteStream: WasteStream, legacyPeriod: String) {
    // Save waste stream
    wasteStreamRepository.save(wasteStream)
    
    // Create legacy declaration marker
    lmaDeclarationRepository.save(
        LmaDeclaration(
            wasteStreamNumber = wasteStream.number,
            period = legacyPeriod, // e.g., "102024" or "LEGACY"
            status = DeclarationStatus.LEGACY_IMPORT,
            totalWeight = 0,
            totalShipments = 0,
            transporters = emptyList()
        )
    )
}
```

**Effect:**

- Legacy waste streams have declaration records in the database
- `FirstReceivalWasteStreamQuery` correctly excludes them (they have `d.id IS NOT NULL`)
- `MonthlyReceivalWasteStreamQuery` correctly includes them (they have `d.period < currentPeriod`)

### Test Coverage

Added comprehensive tests in `MonthlyReceivalWasteStreamQueryAdapterTest.kt`:

1. **Race condition test**: Verifies waste streams with declarations ONLY in the current period are excluded
2. **Previous period test**: Verifies waste streams with previous period declarations are included
3. **Mixed period test**: Verifies waste streams with both previous and current period declarations are still included

## Pros and Cons of the Options

### Option 3: Period Filtering (Chosen)

**Pros:**

- ✅ Eliminates race condition completely
- ✅ Jobs are truly independent
- ✅ Correct business semantics
- ✅ No schema changes
- ✅ Simple to understand and maintain

**Cons:**

- ⚠️ Requires period comparison logic (minimal complexity)

### Option 4: Legacy Declaration Records (Chosen)

**Pros:**

- ✅ No schema changes to `waste_streams`
- ✅ Maintains referential integrity
- ✅ Provides audit trail
- ✅ Works with existing queries
- ✅ Can store additional metadata (period, source system)

**Cons:**

- ⚠️ Requires creating records during import (acceptable overhead)

## More Information

### Related Components

- `FirstReceivalWasteStreamQueryAdapter.kt`: Finds waste streams with NO declarations
- `MonthlyReceivalWasteStreamQueryAdapter.kt`: Finds waste streams with PREVIOUS period declarations
- `WasteDeclarationJobProcessScheduler.kt`: Processes both job types independently
- `lma_declarations` table: Stores all declaration records (current and legacy)

### Period Format

Periods are stored as strings in format `MMYYYY` (e.g., `112025` for November 2025). The comparison `d.period < :currentPeriod` works correctly because:

- String comparison is lexicographic
- Format ensures chronological ordering (e.g., `102025` < `112025`)

### Job Independence

After this fix, the system guarantees:

- `FIRST_RECEIVALS` for period `P` processes waste streams with NO declarations in ANY period
- `MONTHLY_RECEIVALS` for period `P` processes waste streams with declarations in periods `< P`
- Jobs can run in any order, multiple times, or concurrently without conflicts
