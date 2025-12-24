# Automated Waste Declaration System

## Status

Accepted

## Context and Problem Statement

The application must interface with AMICE, the reporting system of the Dutch National Waste Notification Bureau (LMA), to fulfill mandatory waste stream registration and periodic volume reporting requirements. The system needs to:

1. **Automate declarations**: Automatically declare waste receipts on a monthly basis
   2. **Handle late completions**: Process weight tickets that are finalized after the regular declaration window
   3. **Support corrections**: Handle weight changes after initial declaration
   4. **Meet legal deadlines**: Declarations must be submitted within 4 weeks after month end (we target 3 weeks for safety buffer)
   5. **Maintain audit trail**: Track what has been declared and when
   6. **Ensure data integrity**: Prevent mismatches between our records and AMICE's accepted totals

### AMICE System Overview

The LMA/AMICE interface provides three key operations:

#### 1. First Receipt Notification (Eerste Ontvangstmelding - EOM)

Registers a unique waste stream before any volume can be reported.

- **Identifier**: `Afvalstroomnummer` (12 characters: 5-digit processor code + 7-digit internal ID)
  - **Validation Rules**:
    - Origin: Must define the source. For Route Collection or Collector Schemes, specific origin address is omitted but Collector is mandatory
    - Entities: Dutch entities require KVK number (Name empty); Foreign entities require Name
  - **Timing**: Must be accepted before any monthly reports (MOM) can be submitted

#### 2. Monthly Receipt Notification (Maandelijkse Ontvangstmelding - MOM)

Reports total weight and carrier count for a specific waste stream per calendar month.

- **Frequency**: Monthly reporting required
  - **Legal Deadline**: Within 4 weeks after month end
  - **Our Deadline**: 3 weeks after month end (21st) for safety buffer
  - **Corrections (Nameldingen)**:
    - Cumulative Logic: AMICE aggregates all submissions for same stream/period
    - Adjustments: Send supplementary reports with positive/negative values
    - Constraint: Critical fields (Disposer, Period) cannot be modified via update

#### 3. Synchronous Status Check (API v3.3)

Direct polling-based status verification introduced July 2025.

- **Submission**: `MeldingSessie` method returns Session ID immediately
  - **Polling**: `OpvragenResultaatVerwerkingMeldingSessie` retrieves validation results (Accepted/Rejected + Error Codes)
  - **Benefit**: Near real-time feedback without email integration
  - **Rate Limiting**:
    - Maximum 1 status request per 5 minutes per waste stream/period
    - Maximum 6 requests per minute per account

### Technical Constraints

- **Protocol**: SOAP 1.2 WebServices over HTTPS with Client Certificate Authentication (2-way SSL)
  - **Environments**: Production (amice.lma.nl) and Test (test.lma.nl) are separated
  - **Throttling**: Strict rate limits enforced

## Decision Outcome

**Chosen approach: Weight Ticket Line Declaration Tracking with Existing LMA Tables**

The system tracks declaration state at the weight ticket line level while leveraging existing `lma_declarations` and `lma_declaration_sessions` tables for AMICE communication.

### Core Design Principles

1. **State on entity**: Each weight ticket line knows its own declaration state via `declared_weight` and `last_declared_at`
   2. **Single query**: One query finds all lines needing declaration (initial, late, or correction)
   3. **Existing infrastructure**: Use `lma_declarations` for individual declarations and `lma_declaration_sessions` for batch processing
   4. **Delta-based corrections**: Natural fit with AMICE's cumulative model

## Implementation Design

### 1. Database Schema

```sql
-- Add declaration tracking to weight ticket lines
ALTER TABLE weight_ticket_lines 
  ADD COLUMN declared_weight NUMERIC DEFAULT NULL,
  ADD COLUMN last_declared_at TIMESTAMPTZ DEFAULT NULL;

-- Existing tables (already in place):

-- lma_declarations: Records individual declarations
-- Structure:
--   id TEXT PRIMARY KEY (meldingsNummerMelder)
--   waste_stream_number TEXT NOT NULL REFERENCES waste_streams(number)
--   period TEXT NOT NULL (format: MMYYYY)
--   transporters TEXT[] NOT NULL
--   total_weight BIGINT NOT NULL
--   total_shipments BIGINT NOT NULL
--   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
--   status TEXT NOT NULL (PENDING, COMPLETED, FAILED)
--   errors TEXT[]
--   amice_uuid UUID UNIQUE (UUID from AMICE response)

-- lma_declaration_sessions: Tracks batch processing
-- Structure:
--   id UUID PRIMARY KEY
--   type TEXT NOT NULL ('FIRST_RECEIVAL' or 'MONTHLY_RECEIVAL')
--   declaration_ids TEXT[] NOT NULL (References lma_declarations.id)
--   status TEXT NOT NULL (PENDING, PROCESSING, COMPLETED, FAILED)
--   created_at TIMESTAMPTZ NOT NULL
--   processed_at TIMESTAMPTZ
--   errors TEXT[]

CREATE INDEX idx_weight_ticket_lines_declaration_state 
  ON weight_ticket_lines(declared_weight, last_declared_at) 
  WHERE declared_weight IS NOT NULL;
```

### 2. Domain Model

```kotlin
// Value object for declaration state on a weight ticket line
data class LineDeclarationState(
  val declaredWeight: BigDecimal?,
  val lastDeclaredAt: Instant?
) {
  val isDeclared: Boolean get() = declaredWeight != null
  
  fun needsDeclaration(currentWeight: BigDecimal): Boolean =
    declaredWeight == null || declaredWeight != currentWeight
  
  fun delta(currentWeight: BigDecimal): BigDecimal =
    currentWeight - (declaredWeight ?: BigDecimal.ZERO)
}

// LMA Declaration (existing domain model)
data class LmaDeclaration(
  val id: String,  // meldingsNummerMelder
  val wasteStreamNumber: WasteStreamNumber,
  val period: String,  // MMYYYY format
  val transporters: List<String>,
  val totalWeight: Long,
  val totalShipments: Long,
  val createdAt: Instant,
  val status: DeclarationStatus,
  val errors: List<String>,
  val amiceUuid: UUID?
)

// Declaration Session (existing domain model)
data class LmaDeclarationSession(
  val id: UUID,
  val type: SessionType,
  val declarationIds: List<String>,
  val status: SessionStatus,
  val createdAt: Instant,
  val processedAt: Instant?,
  val errors: List<String>
)

enum class SessionType { FIRST_RECEIVAL, MONTHLY_RECEIVAL }
enum class SessionStatus { PENDING, PROCESSING, COMPLETED, FAILED }
enum class DeclarationStatus { PENDING, COMPLETED, FAILED }
```

### 3. Query: Lines Eligible for Declaration

```kotlin
interface WeightTicketLines {
  /**
   * Find all weight ticket lines eligible for declaration for the given period.
   * 
   * A line is eligible if:
   * - Weight ticket status is COMPLETED or INVOICED
   * - Weight ticket weighted_at falls within the period
   * - Line is either undeclared OR current weight differs from declared weight
   */
  fun findEligibleForPeriod(yearMonth: YearMonth): List<WeightTicketLine>
  
  /**
   * Find all lines that need declaration (late or corrections) from periods
   * before the cutoff date.
   */
  fun findNeedingDeclaration(beforeCutoff: YearMonth): List<WeightTicketLine>
}
```

```sql
-- Query for eligible lines in a specific period
SELECT wtl.*, wt.weighted_at, wt.id as weight_ticket_id
FROM weight_ticket_lines wtl
JOIN weight_tickets wt ON wt.id = wtl.weight_ticket_id
WHERE wt.status IN ('COMPLETED', 'INVOICED')
  AND wt.weighted_at >= :startOfMonth
  AND wt.weighted_at < :startOfNextMonth
  AND (
    wtl.declared_weight IS NULL  -- never declared
    OR wtl.declared_weight != wtl.weight_value  -- weight changed since declaration
  )
ORDER BY wtl.waste_stream_number, wt.id;

-- Query for late/correction lines (before cutoff date)
SELECT wtl.*, wt.weighted_at, wt.id as weight_ticket_id
FROM weight_ticket_lines wtl
JOIN weight_tickets wt ON wt.id = wtl.weight_ticket_id
WHERE wt.status IN ('COMPLETED', 'INVOICED')
  AND wt.weighted_at < :cutoffDate
  AND (
    wtl.declared_weight IS NULL
    OR wtl.declared_weight != wtl.weight_value
  )
ORDER BY wt.weighted_at, wtl.waste_stream_number;
```

### 4. Declaration Service

```kotlin
@Component
class WasteDeclarationService(
  private val weightTicketLines: WeightTicketLines,
  private val lmaDeclarations: LmaDeclarations,
  private val lmaDeclarationSessions: LmaDeclarationSessions,
  private val amiceClient: AmiceClient,
  private val wasteStreams: WasteStreams
) {

  /**
   * Declare all eligible weight ticket lines for the given period.
   * Creates declarations for each waste stream and batches them into two sessions:
   * - One session for all First Receivals (EOM)
   * - One session for all Monthly Receivals (MOM)
   */
  @Transactional
  fun declareForPeriod(yearMonth: YearMonth) {
    val lines = weightTicketLines.findEligibleForPeriod(yearMonth)
    
    val firstReceivalDeclarations = mutableListOf<LmaDeclaration>()
    val monthlyReceivalDeclarations = mutableListOf<LmaDeclaration>()
    
    // Group by waste stream and create declarations
    lines.groupBy { it.wasteStreamNumber }.forEach { (wasteStreamNumber, wasteStreamLines) ->
      val declaration = createDeclaration(wasteStreamNumber, yearMonth, wasteStreamLines)
      
      // Determine if this is a first receival (EOM) or monthly receival (MOM)
      val hasExistingDeclaration = lmaDeclarations.existsForWasteStream(wasteStreamNumber)
      if (hasExistingDeclaration) {
        monthlyReceivalDeclarations.add(declaration)
      } else {
        firstReceivalDeclarations.add(declaration)
      }
      
      // Update line declaration state
      wasteStreamLines.forEach { line ->
        line.markAsDeclared(line.weightValue)
      }
      weightTicketLines.saveAll(wasteStreamLines)
    }
    
    // Save all declarations
    lmaDeclarations.saveAll(firstReceivalDeclarations + monthlyReceivalDeclarations)
    
    // Create and send batch sessions
    if (firstReceivalDeclarations.isNotEmpty()) {
      val session = lmaDeclarationSessions.create(
        type = SessionType.FIRST_RECEIVAL,
        declarationIds = firstReceivalDeclarations.map { it.id }
      )
      amiceClient.sendFirstReceivalBatch(firstReceivalDeclarations, session)
    }
    
    if (monthlyReceivalDeclarations.isNotEmpty()) {
      val session = lmaDeclarationSessions.create(
        type = SessionType.MONTHLY_RECEIVAL,
        declarationIds = monthlyReceivalDeclarations.map { it.id }
      )
      amiceClient.sendMonthlyReceivalBatch(monthlyReceivalDeclarations, session)
    }
  }

  private fun createDeclaration(
    wasteStreamNumber: WasteStreamNumber,
    yearMonth: YearMonth,
    lines: List<WeightTicketLine>
  ): LmaDeclaration {
    val totalWeight = lines.sumOf { it.weightValue }
    val totalShipments = lines.distinctBy { it.weightTicketId }.count()
    
    val declarationId = generateDeclarationId()
    return LmaDeclaration(
      id = declarationId,
      wasteStreamNumber = wasteStreamNumber,
      period = yearMonth.format("MMYYYY"),
      transporters = extractTransporters(lines),
      totalWeight = totalWeight.toLong(),
      totalShipments = totalShipments.toLong(),
      status = DeclarationStatus.PENDING
    )
  }
}
```

### 5. Scheduling and Job Processing

The system uses a job-based approach with three types of jobs:

| Job Type | Schedule | Description |
|----------|----------|-------------|
| `FIRST_RECEIVALS` | 21st of each month, 06:00 | Declare first receivals for new waste streams |
| `MONTHLY_RECEIVALS` | 21st of each month, 06:00 | Declare monthly receivals for existing waste streams |
| `LATE_WEIGHT_TICKETS` | Daily, 23:00 | Process late declarations and corrections |

**Job Processor**: `WasteDeclarationJobProcessScheduler` runs every 10 minutes to process pending jobs.

**Session Batching Strategy**:
- **First Receivals**: All EOM declarations for a period are batched into **one session**
  - **Monthly Receivals**: All MOM declarations for a period are batched into **one session**
  - This minimizes AMICE API calls and simplifies status tracking

**EOM vs MOM Logic**: Within a single declaration run:
- Each waste stream is declared as either EOM (if never declared before) or MOM (if previously declared)
  - These are mutually exclusive per waste stream
  - The service automatically determines which type based on `lma_declarations` history
  - All EOMs go into one session, all MOMs go into another session

**Polling**: `PollDeclarationSessionsJob` runs every 5 minutes to check session statuses with AMICE.

**Cutoff date calculation for late declarations**:
```kotlin
fun calculateCutoffDate(now: LocalDate): YearMonth {
  // Lines from months before this date are considered "late" if undeclared
  return if (now.dayOfMonth >= 21) {
    YearMonth(now.year, now.month)
  } else {
    YearMonth(now.year, now.month).minusMonths(1)
  }
}
```

**Example timeline**:
- On December 4th: November lines wait for regular declaration on Dec 21st; October and earlier are processed as late
  - On December 21st: 
    - `FIRST_RECEIVALS` job creates one session with all EOM declarations for new waste streams
    - `MONTHLY_RECEIVALS` job creates one session with all MOM declarations for existing waste streams
    - December lines wait for Jan 21st

### 6. Declaration Session Processing

The `lma_declaration_sessions` table enables batch processing and asynchronous status polling.

**Key Points**:
- Each session contains **multiple declarations** (all EOMs or all MOMs for a period)
  - Sessions are polled every 5 minutes for status updates
  - When a session completes, all individual declarations within it are updated
  - Rate limits: max 6 session polls per minute

**Session Lifecycle**:
1. **Creation**: Job creates session with all declaration IDs for that type (EOM or MOM)
   2. **Submission**: Batch of declarations sent to AMICE via `MeldingSessie` API
   3. **Polling**: Background job polls `OpvragenResultaatVerwerkingMeldingSessie` for results
   4. **Update**: Individual declaration statuses updated based on AMICE response
   5. **Completion**: Session marked as COMPLETED or FAILED

```kotlin
@Component
class DeclarationSessionProcessor(
  private val lmaDeclarationSessions: LmaDeclarationSessions,
  private val lmaDeclarations: LmaDeclarations,
  private val amiceClient: AmiceClient
) {

  /**
   * Poll AMICE for results of pending sessions.
   * Rate limited by AMICE: max 6 requests per minute per account.
   */
  @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
  fun pollPendingSessions() {
    val pendingSessions = lmaDeclarationSessions.findByStatus(SessionStatus.PENDING)
      .take(6) // Respect rate limit
    
    pendingSessions.forEach { session ->
      try {
        val result = amiceClient.querySessionResult(session.id)
        
        when (result.status) {
          AmiceSessionStatus.COMPLETED -> {
            // Update all declarations in this session based on AMICE feedback
            result.declarationResults.forEach { (declarationId, declarationResult) ->
              val declaration = lmaDeclarations.findById(declarationId)
              if (declaration != null) {
                val updatedDeclaration = declaration.copy(
                  status = if (declarationResult.accepted) DeclarationStatus.COMPLETED else DeclarationStatus.FAILED,
                  errors = declarationResult.errors,
                  amiceUuid = declarationResult.amiceUuid
                )
                lmaDeclarations.save(updatedDeclaration)
              }
            }
            
            // Update session
            lmaDeclarationSessions.markCompleted(session.id)
          }
          AmiceSessionStatus.FAILED -> {
            lmaDeclarationSessions.markFailed(session.id, result.errors)
          }
          AmiceSessionStatus.PROCESSING -> {
            // Still processing, will poll again later
          }
        }
      } catch (e: Exception) {
        logger.error("Failed to poll session ${session.id}", e)
      }
    }
  }
}
```

### 7. Weight Ticket Line Lifecycle

```
Weight Ticket Line Declaration States:
                                          
  ┌─────────────┐                         
  │   Created   │                         
  │ (on ticket) │                         
  └──────┬──────┘                         
         │                                
         ▼                                
  ┌─────────────┐    ┌─────────────────┐  
  │ Undeclared  │───▶│    Declared     │  
  │ (null wt)   │    │ (wt = current)  │  
  └─────────────┘    └────────┬────────┘  
                              │           
                     Weight   │           
                     Changed  ▼           
                     ┌─────────────────┐  
                     │ Needs Correction│  
                     │ (wt ≠ current)  │  
                     └────────┬────────┘  
                              │           
                     Correction          
                     Sent     ▼           
                     ┌─────────────────┐  
                     │    Declared     │  
                     │ (wt = current)  │  
                     └─────────────────┘  
```

### 8. Status Reconciliation

```kotlin
@Component
class AmiceReconciliationService(
  private val lmaDeclarations: LmaDeclarations,
  private val amiceClient: AmiceClient
) {
  
  /**
   * Reconcile our declarations with AMICE's accepted totals.
   * Rate limited: 1 request per 5 min per waste stream/period.
   */
  fun reconcile(wasteStreamNumber: WasteStreamNumber, period: YearMonth) {
    val amiceStatus = amiceClient.queryStatus(wasteStreamNumber, period)
    
    // Sum all completed declarations for this waste stream/period
    val ourDeclarations = lmaDeclarations.findByWasteStreamAndPeriod(
      wasteStreamNumber, 
      period.format("MMYYYY")
    ).filter { it.status == DeclarationStatus.COMPLETED }
    
    val ourTotalWeight = ourDeclarations.sumOf { it.totalWeight }
    val ourTotalShipments = ourDeclarations.sumOf { it.totalShipments }
    
    if (amiceStatus.totalWeight != ourTotalWeight || 
        amiceStatus.totalShipments != ourTotalShipments) {
      logger.warn("Reconciliation mismatch for $wasteStreamNumber/$period: " +
               "AMICE=${amiceStatus.totalWeight}/${amiceStatus.totalShipments}, " +
               "Ours=${ourTotalWeight}/${ourTotalShipments}")
      // Alert or queue for investigation
    }
  }
}
```

### 9. Error Handling

```kotlin
sealed class DeclarationError {
  data class ValidationError(val code: String, val message: String) : DeclarationError()
  data class RateLimitExceeded(val retryAfter: Duration) : DeclarationError()
  data class NetworkError(val cause: Throwable) : DeclarationError()
  data class AmiceRejection(val errors: List<String>) : DeclarationError()
}

// Error codes from AMICE
enum class AmiceErrorCode(val code: String) {
  ONTDOENER_LEEG("OntdoenerLeeg"),
  TOTAAL_GEWICHT_NEGATIEF("TotaalGewichtNegatief"),
  AFVALSTROOM_NIET_GEVONDEN("AfvalstroomNietGevonden"),
  // ... other error codes
}
```

## Pros and Cons

### Chosen Approach: Weight Ticket Line Declaration Tracking

**Pros:**
- Simple mental model: each line knows its own state
  - Single query to find what needs declaration
  - Natural correction support via delta calculation
  - Leverages existing `lma_declarations` and `lma_declaration_sessions` infrastructure
  - Clear separation: line state vs. AMICE communication history
  - Easy reconciliation with AMICE totals
  - No complex snapshot table joins

**Cons:**
- `declared_weight` column denormalizes state
  - Requires careful handling of concurrent updates to weight ticket lines

### Alternative: Immutable Snapshot Table (Rejected)

**Pros:**
- Full history of declared values
  - Immutable audit trail

**Cons:**
- Disconnected from actual declaration logic
  - Complex queries to determine current state
  - Risk of snapshot/declaration mismatch
  - Additional storage overhead

## Implementation Notes

Since the system is not yet live in production:
- No data migration required
  - Only need to add `declared_weight` and `last_declared_at` columns to `weight_ticket_lines`
  - Existing `lma_declarations` and `lma_declaration_sessions` tables used as-is

## More Information

### Key Simplifications

1. **One query to rule them all**: Single query finds lines needing declaration (initial, late, or correction)
   2. **State on entity**: No need to join with separate snapshot tables
   3. **Clear ledger**: Every AMICE submission recorded in `lma_declarations` with outcome
   4. **Session-based processing**: Batch declarations and poll for results asynchronously

### AMICE Integration Details

- **Authentication**: Client certificate (2-way SSL)
  - **Protocol**: SOAP 1.2 over HTTPS
  - **Endpoints**:
    - Production: `amice.lma.nl`
    - Test: `test.lma.nl`
  - **Rate Limits**:
    - Status queries: 1 per 5 minutes per waste stream/period
    - Global: 6 requests per minute per account

### Future Considerations

- Add UI to view declaration history per waste stream
  - Implement automatic reconciliation with alerting
  - Consider batch processing optimization for high-volume scenarios
  - Add retry mechanism with exponential backoff for transient failures
