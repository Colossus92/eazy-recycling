# Late and Corrective Waste Declarations

## Context and Problem Statement

The current monthly waste declaration system triggers jobs on the 20th of each month to declare all completed/invoiced weight tickets from the previous month. However, this approach has limitations:

1. **Late completions**: Weight tickets may be completed after the 20th, missing the declaration window for their month
2. **Corrections after declaration**: Weight tickets may be modified (e.g., material weights adjusted) after they have already been declared, requiring corrective declarations
3. **Multi-month backlog**: Late weight tickets could span multiple previous months, not just the immediately preceding one

The LMA (Landelijk Meldpunt Afvalstoffen) system accepts both initial declarations and corrections. Corrections can be positive (additional weight) or negative (weight reduction). The challenge is tracking which weight tickets have been declared and detecting changes that require corrective declarations.

### Current Architecture

- `weight_tickets` table: Contains weight ticket data with `status` (DRAFT, COMPLETED, INVOICED, CANCELLED), `weighted_at` timestamp
- `weight_ticket_lines` table: Contains material lines with `waste_stream_number` and `weight_value`
- `lma_declarations` table: Stores declaration records per waste stream/period with `total_weight`, `total_shipments`
- `monthly_waste_declaration_jobs` table: Tracks job execution (FIRST_RECEIVALS, MONTHLY_RECEIVALS)
- Declaration queries filter by `weighted_at` within month boundaries and `status IN (COMPLETED, INVOICED)`

## Considered Options

### Option 1: Declaration Status on Weight Ticket

Add a `declaration_status` column to `weight_tickets` table with values:

- `NOT_DECLARED`: Not yet included in any declaration
- `DECLARED`: Successfully declared
- `NEEDS_CORRECTION`: Modified after declaration, requires corrective declaration

**Pros:**

- Simple to query undeclared tickets
- Clear status visibility per ticket

**Cons:**

- Doesn't capture the declared values, making delta calculation impossible
- Cannot determine if correction is positive or negative
- Loses declaration history

### Option 2: Declaration Snapshot Table

Create a new `weight_ticket_declaration_snapshots` table that stores the declared state of each weight ticket line:

```sql
CREATE TABLE weight_ticket_declaration_snapshots (
  id BIGSERIAL PRIMARY KEY,
  weight_ticket_id BIGINT NOT NULL REFERENCES weight_tickets(id),
  weight_ticket_line_id BIGINT NOT NULL REFERENCES weight_ticket_lines(id),
  waste_stream_number TEXT NOT NULL,
  declared_weight_value NUMERIC NOT NULL,
  declaration_id TEXT NOT NULL REFERENCES lma_declarations(id),
  declared_at TIMESTAMPTZ NOT NULL,
  declaration_period TEXT NOT NULL -- format: MMYYYY
);
```

**Pros:**

- Full audit trail of what was declared
- Easy delta calculation: `current_weight - declared_weight`
- Supports multiple corrections over time
- Can reconstruct declaration history

**Cons:**

- Additional storage overhead
- More complex queries
- Need to maintain snapshot consistency

### Option 3: Hybrid - Status Column + Snapshot for Corrections

Combine Option 1 and 2:

- Add `declaration_status` to `weight_tickets` for quick filtering
- Create snapshot table only for declared tickets to enable delta calculation

**Pros:**

- Fast queries for undeclared tickets via status column
- Full correction capability via snapshots
- Balance between simplicity and completeness

**Cons:**

- Two mechanisms to maintain
- Potential for status/snapshot inconsistency

### Option 4: Event Sourcing on Weight Ticket Lines

Track all weight changes as events, derive declaration state from event history.

**Pros:**

- Complete audit trail
- Supports complex correction scenarios

**Cons:**

- Significant architectural change
- Over-engineering for the current requirement
- Complex implementation

## Decision Outcome

**Chosen option: Option 2 - Declaration Snapshot Table**, because:

1. It provides the necessary data to calculate deltas for corrective declarations
2. Maintains a complete audit trail of declarations
3. Supports the requirement without over-engineering
4. Aligns with the existing pattern of storing declaration records in `lma_declarations`

### Implementation Approach

#### 1. Database Schema Changes

```sql
-- Snapshot of weight ticket lines at declaration time
CREATE TABLE weight_ticket_declaration_snapshots (
  id BIGSERIAL PRIMARY KEY,
  weight_ticket_id BIGINT NOT NULL,
  weight_ticket_line_id BIGINT NOT NULL,
  waste_stream_number TEXT NOT NULL,
  declared_weight_value NUMERIC NOT NULL,
  declaration_id TEXT NOT NULL,
  declared_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  declaration_period TEXT NOT NULL,
  
  CONSTRAINT fk_weight_ticket FOREIGN KEY (weight_ticket_id) REFERENCES weight_tickets(id),
  CONSTRAINT fk_weight_ticket_line FOREIGN KEY (weight_ticket_line_id) REFERENCES weight_ticket_lines(id),
  CONSTRAINT fk_declaration FOREIGN KEY (declaration_id) REFERENCES lma_declarations(id)
);

CREATE INDEX idx_declaration_snapshots_weight_ticket ON weight_ticket_declaration_snapshots(weight_ticket_id);
CREATE INDEX idx_declaration_snapshots_waste_stream ON weight_ticket_declaration_snapshots(waste_stream_number);
CREATE INDEX idx_declaration_snapshots_period ON weight_ticket_declaration_snapshots(declaration_period);
```

#### 2. New Job Types

Extend `MonthlyWasteDeclarationJob.JobType`:

- `LATE_FIRST_RECEIVALS`: For undeclared weight tickets from any previous month (waste streams without prior declarations)
- `LATE_MONTHLY_RECEIVALS`: For undeclared weight tickets from any previous month (waste streams with prior declarations)
- `CORRECTIVE_DECLARATIONS`: For weight tickets that changed after declaration

#### 3. Job Scheduling

| Job Type | Schedule | Scope |
|----------|----------|-------|
| FIRST_RECEIVALS | 20th of month | Previous month only |
| MONTHLY_RECEIVALS | 20th of month | Previous month only |
| LATE_FIRST_RECEIVALS | Daily | All previous months |
| LATE_MONTHLY_RECEIVALS | Daily | All previous months |
| CORRECTIVE_DECLARATIONS | Daily | All declared tickets |

#### 4. Query Logic

**Late Declarations Query:**

```sql
-- Find weight ticket lines not yet declared
SELECT wtl.*, wt.weighted_at
FROM weight_ticket_lines wtl
JOIN weight_tickets wt ON wt.id = wtl.weight_ticket_id
LEFT JOIN weight_ticket_declaration_snapshots snap 
  ON snap.weight_ticket_line_id = wtl.id
WHERE wt.status IN ('COMPLETED', 'INVOICED')
  AND wt.weighted_at < :startOfCurrentMonth  -- Only previous months
  AND snap.id IS NULL  -- Not yet declared
```

**Corrective Declarations Query:**

```sql
-- Find weight ticket lines that changed after declaration
SELECT 
  wtl.*,
  snap.declared_weight_value,
  (wtl.weight_value - snap.declared_weight_value) as weight_delta
FROM weight_ticket_lines wtl
JOIN weight_tickets wt ON wt.id = wtl.weight_ticket_id
JOIN weight_ticket_declaration_snapshots snap 
  ON snap.weight_ticket_line_id = wtl.id
WHERE wt.status IN ('COMPLETED', 'INVOICED')
  AND wtl.weight_value != snap.declared_weight_value
  AND snap.declared_at = (
    SELECT MAX(s2.declared_at) 
    FROM weight_ticket_declaration_snapshots s2 
    WHERE s2.weight_ticket_line_id = wtl.id
  )
```

#### 5. Declaration Flow

1. **Initial Declaration** (First/Monthly Receivals):
   - Query undeclared weight ticket lines for the period
   - Aggregate by waste stream
   - Submit to LMA
   - Create snapshot records for each declared line

2. **Corrective Declaration**:
   - Query lines where current weight differs from latest snapshot
   - Calculate delta (positive or negative)
   - Submit correction to LMA with delta values
   - Create new snapshot record with current values

#### 6. Edge Cases

- **Cancelled weight tickets**: If a declared ticket is cancelled, treat as correction with negative weight equal to declared amount
- **Line removal**: If a line is deleted after declaration, detect via missing line with existing snapshot
- **Line addition**: New lines on existing tickets are treated as late declarations

### Domain Model Changes

```kotlin
// New aggregate for tracking declaration state
data class WeightTicketDeclarationSnapshot(
  val id: Long,
  val weightTicketId: WeightTicketId,
  val weightTicketLineId: Long,
  val wasteStreamNumber: WasteStreamNumber,
  val declaredWeightValue: BigDecimal,
  val declarationId: String,
  val declaredAt: Instant,
  val declarationPeriod: YearMonth
)

// New port for snapshot persistence
interface WeightTicketDeclarationSnapshots {
  fun save(snapshot: WeightTicketDeclarationSnapshot)
  fun findLatestByWeightTicketLineId(lineId: Long): WeightTicketDeclarationSnapshot?
  fun findUndeclaredLines(beforeMonth: YearMonth): List<UndeclaredWeightTicketLine>
  fun findLinesNeedingCorrection(): List<WeightTicketLineCorrection>
}

data class WeightTicketLineCorrection(
  val weightTicketLine: WeightTicketLine,
  val previouslyDeclaredWeight: BigDecimal,
  val currentWeight: BigDecimal,
  val delta: BigDecimal
)
```

## Pros and Cons of the Options

### Option 1: Declaration Status on Weight Ticket

**Pros:**

- Minimal schema change
- Simple status-based queries
- Low storage overhead

**Cons:**

- Cannot calculate correction deltas
- No audit trail of declared values
- Requires additional logic to detect what changed

### Option 2: Declaration Snapshot Table (Chosen)

**Pros:**

- Complete audit trail
- Enables accurate delta calculation
- Supports multiple corrections
- Clear separation of concerns
- Queryable declaration history

**Cons:**

- Additional table and storage
- More complex queries
- Need to ensure snapshot creation on every declaration

### Option 3: Hybrid Approach

**Pros:**

- Fast filtering via status column
- Full correction support via snapshots

**Cons:**

- Dual mechanism complexity
- Risk of status/snapshot desynchronization
- More code to maintain

### Option 4: Event Sourcing

**Pros:**

- Ultimate flexibility
- Complete history

**Cons:**

- Major architectural change
- Significant implementation effort
- Over-engineering for current needs

## More Information

### Related ADRs

- ADR-0010: Denormalize transport wastestream data

### LMA Declaration Types

The LMA system supports:

- **Eerste Ontvangst Melding** (First Receival): Initial declaration for a waste stream
- **Maandelijkse Ontvangst Melding** (Monthly Receival): Recurring monthly declaration
- Both support corrections via positive/negative weight values

### Future Considerations

- Consider adding a UI to view declaration history per weight ticket
- May need reconciliation tooling to verify declared vs actual weights
- Consider alerting when corrections exceed certain thresholds
